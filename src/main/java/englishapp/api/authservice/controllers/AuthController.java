package englishapp.api.authservice.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import api.common.englishapp.requests.CommonResponse;
import api.common.englishapp.requests.ResponseUtil;
import englishapp.api.authservice.dto.apiCheckEmail.InputParamApiCheckEmail;
import englishapp.api.authservice.dto.apiCheckOTP.InputParamApiCheckOTP;
import englishapp.api.authservice.dto.apiLogin.InputParamApiLogin;
import englishapp.api.authservice.dto.apiLogout.InputParamApiLogout;
import englishapp.api.authservice.dto.apiRefreshToken.InputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRegister.InputParamApiRegister;
import englishapp.api.authservice.dto.apiResetPassword.InputPramApiResetPassword;
import englishapp.api.authservice.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
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

    @Operation(summary = "Kiểm tra token", description = "Xác thực tính hợp lệ của token")
    @PostMapping("/checkToken") // Thêm endpoint mapping
    public Mono<ResponseEntity<CommonResponse<?>>> checkToken(@RequestHeader("Authorization") String token) {
        // Loại bỏ "Bearer " nếu có
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return authService.checkToken(token)
                .map(data -> {
                    logger.info("Token is valid");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during token check: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/checkEmail")
    @Operation(summary = "Kiểm tra email", description = "Kiểm tra xem email đã tồn tại hay chưa")
    public Mono<ResponseEntity<CommonResponse<?>>> checkEmail(@RequestBody InputParamApiCheckEmail input) {
        return authService.checkEmail(input.getEmail())
                .map(data -> {
                    logger.info("Email check completed");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    if (error instanceof ResponseStatusException statusException &&
                            statusException.getStatusCode() == HttpStatus.NO_CONTENT) {
                        return Mono.just(ResponseUtil.noContent());
                    }
                    logger.error("Error during email check: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/checkOTP")
    @Operation(summary = "Kiểm tra OTP", description = "Xác thực mã OTP được gửi đến email")
    public Mono<ResponseEntity<CommonResponse<?>>> checkOTP(@RequestBody InputParamApiCheckOTP input) {
        return authService.verifyOtp(
                input.getEmail(),
                input.getOtpCode())
                .map(data -> {
                    logger.info("OTP check completed");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during OTP check: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "Đặt lại mật khẩu", description = "Cung cấp chức năng đặt lại mật khẩu cho người dùng")
    public Mono<ResponseEntity<CommonResponse<?>>> resetPassword(@RequestBody InputPramApiResetPassword input) {
        return authService.resetPassword(
                input.getEmail(),
                input.getNewPassword())
                .map(data -> {
                    logger.info("Password reset completed");
                    return ResponseUtil.ok(data);
                })
                .onErrorResume(error -> {
                    logger.error("Error during password reset: {}", error.getMessage());
                    return Mono.just(ResponseUtil.serverError(error.getMessage()));
                });
    }
}
