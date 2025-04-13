package englishapp.api.authservice.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import englishapp.api.authservice.dto.apiCheckToken.InputParamApiCheckToken;
import englishapp.api.authservice.dto.apiGetToken.InputParamApiGetToken;
import englishapp.api.authservice.dto.apiGetToken.OutputParamApiGetToken;
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
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtUtil jwtUtil;

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
                            user.setCreatedAt(LocalDateTime.now());
                            user.setUpdatedAt(LocalDateTime.now());

                            return userRepository.save(user)
                                    .onErrorMap(error -> true, error -> {
                                        logger.error("Error saving user: {}", error.getMessage(), error);
                                        return new RuntimeException("Error saving user");
                                    })
                                    .flatMap(savedUser -> {
                                        String jwt = jwtUtil.generateToken(savedUser.getEmail());
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

                    String jwt = jwtUtil.generateToken(user.getEmail());
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
                                String newAccessToken = jwtUtil.generateToken(user.getEmail());
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
                .flatMap(deletedCount -> {
                    if (deletedCount > 0) {
                        logger.info("Token deleted successfully");
                        return Mono.empty();
                    } else {
                        return Mono.error(new RuntimeException("No token found"));
                    }
                });
    }

    public Mono<Void> checkToken(InputParamApiCheckToken inputParamCheckToken) {
        return Mono.just(inputParamCheckToken)
                .flatMap(input -> {
                    if (!jwtUtil.validateToken(input.getToken(), input.getEmail())) {
                        return Mono.<Void>error(new RuntimeException("Token invalid"));
                    }
                    return Mono.empty();
                })
                .doOnError(error -> {
                    logger.error("Error checking token: {}", error.getMessage());
                });
    }

    public Mono<OutputParamApiGetToken> getToken(InputParamApiGetToken inputParamApiGetToken) {
        return refreshTokenRepository.findByRfToken(inputParamApiGetToken.getRefreshToken())
                .flatMap(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        return Mono.error(new RuntimeException("Refresh token expired"));
                    }
                    return userRepository.findById(token.getUserId())
                            .flatMap(user -> {
                                if (user == null) {
                                    return Mono.error(new RuntimeException("User not found"));
                                }
                                String newAccessToken = jwtUtil.generateToken(user.getEmail());
                                // Cập nhật lại access token và expiry date trong refresh token cũ
                                token.setToken(newAccessToken);
                                return refreshTokenRepository.save(token)
                                        .then(Mono.just(new OutputParamApiGetToken(
                                                newAccessToken)));
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")));
    }
}
