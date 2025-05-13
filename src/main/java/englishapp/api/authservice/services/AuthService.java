package englishapp.api.authservice.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import englishapp.api.authservice.dto.apiCheckToken.OutputParamApiCheckToken;
import englishapp.api.authservice.dto.apiLogin.InputParamApiLogin;
import englishapp.api.authservice.dto.apiLogin.OutputParamApiLogin;
import englishapp.api.authservice.dto.apiRefreshToken.InputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRefreshToken.OutputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRegister.InputParamApiRegister;
import englishapp.api.authservice.dto.apiRegister.OutputParamApiRegister;
import englishapp.api.authservice.models.RefreshToken;
import englishapp.api.authservice.models.User;
import englishapp.api.authservice.repositories.RefreshTokenRepository;
import englishapp.api.authservice.repositories.UserRepository;
import englishapp.api.authservice.util.JwtUtil;
import englishapp.api.authservice.util.PasswordUtil;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.time.Duration;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmailService emailService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Assuming you have a RedisTemplate bean configured

    private final PasswordUtil passwordEncoder = new PasswordUtil();
    private static final Logger logger = LogManager.getLogger(AuthService.class);

    public Mono<OutputParamApiRegister> register(InputParamApiRegister inputParamApiRegister) {
        return userRepository.findByEmail(inputParamApiRegister.getEmail())
                .flatMap(user -> Mono.<OutputParamApiRegister>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            User user = new User();
                            user.setEmail(inputParamApiRegister.getEmail());
                            user.setPassword(passwordEncoder.hashPassword(inputParamApiRegister.getPassword()));
                            user.setUserName(inputParamApiRegister.getUserName());
                            user.setTypeUser(0);
                            user.setRole("USER");
                            user.setCreatedAt(LocalDateTime.now());
                            user.setUpdatedAt(LocalDateTime.now());

                            return userRepository.save(user)
                                    .onErrorMap(error -> true, error -> {
                                        logger.error("Error saving user: {}", error.getMessage(), error);
                                        return new RuntimeException("Error saving user");
                                    })
                                    .flatMap(savedUser -> {
                                        String jwt = jwtUtil.generateToken(savedUser.getUserId(), savedUser.getEmail(),
                                                savedUser.getRole());
                                        String refreshToken = UUID.randomUUID().toString();
                                        RefreshToken token = new RefreshToken(null, savedUser.getUserId(),
                                                jwt, refreshToken,
                                                Instant.now().plusMillis(86400000));
                                        return refreshTokenRepository.save(token)
                                                .onErrorMap(error -> true, error -> {
                                                    logger.error("Error saving token: {}", error.getMessage(), error);
                                                    return new RuntimeException("Error saving token");
                                                })
                                                .map(savedToken -> {
                                                    OutputParamApiRegister outputParamApiRegister = new OutputParamApiRegister();
                                                    outputParamApiRegister.setAccessToken(jwt);
                                                    outputParamApiRegister.setUserName(savedUser.getUserName());
                                                    outputParamApiRegister.setRefreshToken(refreshToken);
                                                    return outputParamApiRegister;
                                                });
                                    });
                        }));
    }

    public Mono<OutputParamApiLogin> login(InputParamApiLogin inputParamApiLogin) {
        return userRepository.findByEmail(inputParamApiLogin.getEmail())
                .flatMap(user -> {
                    if (!passwordEncoder.checkPassword(inputParamApiLogin.getPassword(), user.getPassword())) {
                        return Mono.<OutputParamApiLogin>error(new RuntimeException("Invalid credentials"));
                    }

                    String jwt = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
                    String refreshToken = UUID.randomUUID().toString();
                    RefreshToken token = new RefreshToken(null, user.getUserId(),
                            jwt, refreshToken,
                            Instant.now().plusMillis(86400000));

                    return refreshTokenRepository.save(token) // Đưa lệnh save vào chuỗi reactive
                            .map(savedToken -> {
                                OutputParamApiLogin outputParamApiLogin = new OutputParamApiLogin();
                                outputParamApiLogin.setAccessToken(jwt);
                                outputParamApiLogin.setRefreshToken(refreshToken);
                                outputParamApiLogin.setUserId(user.getUserId());
                                outputParamApiLogin.setUserName(user.getUserName());
                                outputParamApiLogin.setEmail(user.getEmail());
                                outputParamApiLogin.setTypeUser(user.getTypeUser());
                                outputParamApiLogin.setRole(user.getRole());
                                return outputParamApiLogin;
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
    }

    public Mono<OutputParamApiRefreshToken> refreshToken(InputParamApiRefreshToken inputParamApiRefreshToken) {
        return refreshTokenRepository.findByRfToken(inputParamApiRefreshToken.getRefreshToken())
                .flatMap(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        return Mono.error(new RuntimeException("Refresh token expired"));
                    }
                    return userRepository.findById(token.getUserId())
                            .flatMap(user -> {
                                if (user == null) {
                                    return Mono.error(new RuntimeException("User not found"));
                                }
                                String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(),
                                        user.getRole());
                                // Cập nhật lại access token và expiry date trong refresh token cũ
                                token.setToken(newAccessToken);
                                return refreshTokenRepository.save(token)
                                        .then(Mono.just(new OutputParamApiRefreshToken(
                                                newAccessToken)));
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")));
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.deleteByRfToken(refreshToken)
                .doOnError(error -> {
                    logger.error("Error deleting token: {}", error.getMessage(), error);
                    throw new RuntimeException("Error deleting token");
                })
                .then(Mono.fromRunnable(() -> {
                    logger.info("Token deleted successfully");
                }));
    }

    public Mono<OutputParamApiCheckToken> checkToken(String token) {
        return Mono.defer(() -> {
            Claims claims;
            try {
                claims = jwtUtil.extractClaims(token);
            } catch (Exception ex) {
                return Mono.error(new RuntimeException("Invalid or expired token"));
            }

            String userId = claims.get("idUser", String.class);
            return userRepository.findById(userId)
                    .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                    .flatMap(user -> {
                        if (!jwtUtil.validateToken(token, user.getEmail())) {
                            return Mono.error(new RuntimeException("Invalid token"));
                        }

                        return Mono.just(new OutputParamApiCheckToken(
                                user.getUserId(), user.getEmail(), user.getRole(), user.getListBlockEndpoint()));
                    });
        }).doOnError(error -> logger.error("Error checking token: {}", error.getMessage()));
    }

    public Mono<Void> checkEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NO_CONTENT, "Email not found")))
                .doOnSuccess(user -> logger.info("Found user: {}", user))
                .flatMap(user -> {
                    // Tạo mã OTP ngẫu nhiên 6 chữ số
                    String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
                    String redisKey = "reset_otp:" + email;
                    logger.info("Generated OTP: {} for email: {}", otp, email);

                    // Lưu OTP vào Redis với TTL 5 phút và gửi email
                    redisTemplate.opsForValue().set(redisKey, otp, Duration.ofMinutes(5));
                    logger.info("Saved OTP to Redis with key: {}", redisKey);

                    return emailService.sendEmail(email, "Reset Password OTP", "Your OTP is: " + otp)
                            .doOnSuccess(v -> logger.info("Email sent successfully to: {}", email))
                            .doOnError(error -> logger.error("Error sending email: {}", error.getMessage()));
                })
                .onErrorResume(error -> {
                    logger.error("Error in checkEmail: {}", error.getMessage(), error);
                    return Mono.error(error); // giữ nguyên lỗi để controller xử lý
                });
    }

    public Mono<Void> verifyOtp(String email, String otp) {
        String redisKey = "reset_otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            return Mono.error(new RuntimeException("OTP expired or not found"));
        }

        if (!storedOtp.equals(otp)) {
            return Mono.error(new RuntimeException("Invalid OTP"));
        }
        redisTemplate.delete(redisKey);
        return Mono.empty();
    }

    public Mono<Void> resetPassword(String email, String newPassword) {
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.hashPassword(newPassword));
                    return userRepository.save(user)
                            .then();
                })
                .doOnSuccess(v -> logger.info("Password reset successfully for email: {}", email))
                .doOnError(error -> logger.error("Error resetting password: {}", error.getMessage()));
    }
}
