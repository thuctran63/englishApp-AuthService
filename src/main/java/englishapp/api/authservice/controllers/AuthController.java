package englishapp.api.authservice.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import api.common.englishapp.requests.CommonResponse;
import api.common.englishapp.requests.ResponseUtil;
import englishapp.api.authservice.dto.apiCheckToken.InputParamApiCheckToken;
import englishapp.api.authservice.dto.apiGetToken.InputParamApiGetToken;
import englishapp.api.authservice.dto.apiLogin.InputParamApiLogin;
import englishapp.api.authservice.dto.apiLogout.InputParamApiLogout;
import englishapp.api.authservice.dto.apiRefreshToken.InputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRegister.InputParamApiRegister;
import englishapp.api.authservice.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs liên quan đến xác thực người dùng")
public class AuthController {

    private final AuthService authService;
    private static final Logger logger = LogManager.getLogger(AuthController.class);

    @PostMapping("/register")
    public Mono<ResponseEntity<CommonResponse<?>>> register(@RequestBody InputParamApiRegister input) {
        return authService.register(input)
                .map(data -> {
                    logger.info("User registered successfully: {}", data.getUserName());
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during registration: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng và nhận token")
    public Mono<ResponseEntity<CommonResponse<?>>> login(@RequestBody InputParamApiLogin input) {
        return authService.login(input)
                .map(data -> {
                    logger.info("User logged in successfully: {}", data.getUserName());
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during login: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới token", description = "Cung cấp token mới dựa trên token cũ")
    public Mono<ResponseEntity<CommonResponse<?>>> refresh(@RequestBody InputParamApiRefreshToken input) {
        return authService.refreshToken(input)
                .map(data -> {
                    logger.info("Token refreshed successfully");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during token refresh: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Xóa token hiện tại")
    public Mono<ResponseEntity<CommonResponse<?>>> logout(@RequestBody InputParamApiLogout input) {
        return authService.logout(
                input.getRefreshToken())
                .map(data -> {
                    logger.info("User logged out successfully");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during logout: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/checkToken")
    @Operation(summary = "Kiểm tra token", description = "Xác thực tính hợp lệ của token")
    public Mono<ResponseEntity<CommonResponse<?>>> checkToken(@RequestBody InputParamApiCheckToken input) {
        return authService.checkToken(input)
                .map(data -> {
                    logger.info("Token is valid");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during token check: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/getToken")
    @Operation(summary = "Lấy token", description = "Cung cấp token mới dựa vào refresh token")
    public Mono<ResponseEntity<CommonResponse<?>>> getToken(@RequestBody InputParamApiGetToken input) {
        return authService.getToken(input)
                .map(data -> {
                    logger.info("Token retrieved successfully");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during token retrieval: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }
}
