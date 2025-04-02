package englishapp.api.authservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Mono<OutputParamApiRegister> register(InputParamApiRegister inputParamApiRegister) {
        return userRepository.findByEmail(inputParamApiRegister.getEmail())
                .flatMap(email -> Mono.<OutputParamApiRegister>error(new RuntimeException("Email already exists")))
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
                                    .doOnError(error -> {
                                        System.err.println("Error saving user: " + error.getMessage());
                                        error.printStackTrace();
                                    })
                                    .doOnSuccess(savedUser -> {
                                        System.out.println("User saved successfully: " + savedUser.getUserId());
                                    })
                                    .flatMap(savedUser -> {
                                        String jwt = jwtUtil.generateToken(savedUser.getEmail());
                                        String refreshToken = UUID.randomUUID().toString();
                                        RefreshToken token = new RefreshToken(null, savedUser.getUserId(),
                                                jwt, refreshToken,
                                                Instant.now().plusMillis(86400000));

                                        return refreshTokenRepository.save(token)
                                                .doOnError(error -> {
                                                    System.err.println("Error saving token: " + error.getMessage());
                                                    error.printStackTrace();
                                                })
                                                .doOnSuccess(savedToken -> {
                                                    System.out
                                                            .println("Token saved successfully: " + savedToken.getId());
                                                })
                                                .map(savedToken -> {
                                                    OutputParamApiRegister outputParamApiRegister = new OutputParamApiRegister();
                                                    outputParamApiRegister.setAccessToken(jwt);
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
        return refreshTokenRepository.findByToken(inputParamApiRefreshToken.getRefreshToken())
                .flatMap(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        return Mono.error(new RuntimeException("Refresh token expired"));
                    }
                    return userRepository.findById(token.getUserId())
                            .flatMap(user -> {
                                if (user == null) {
                                    return Mono.error(new RuntimeException("User not found"));
                                }
                                String jwt = jwtUtil.generateToken(user.getEmail());
                                String newRefreshToken = UUID.randomUUID().toString();
                                RefreshToken newToken = new RefreshToken(null, user.getUserId(),
                                        jwt, newRefreshToken,
                                        Instant.now().plusMillis(86400000));
                                return refreshTokenRepository.save(newToken)
                                        .then(Mono.just(new OutputParamApiRefreshToken(jwt, newRefreshToken)));
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")));
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.deleteByUserId(refreshToken)
                .doOnError(error -> {
                    System.err.println("Error deleting token: " + error.getMessage());
                    error.printStackTrace();
                })
                .doOnSuccess(aVoid -> {
                    System.out.println("Token deleted successfully");
                });
    }
}
