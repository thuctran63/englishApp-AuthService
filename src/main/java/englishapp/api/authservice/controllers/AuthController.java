package englishapp.api.authservice.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import englishapp.api.authservice.dto.apiLogin.InputParamApiLogin;
import englishapp.api.authservice.dto.apiLogin.OutputParamApiLogin;
import englishapp.api.authservice.dto.apiRefreshToken.InputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRefreshToken.OutputParamApiRefreshToken;
import englishapp.api.authservice.dto.apiRegister.InputParamApiRegister;
import englishapp.api.authservice.dto.apiRegister.OutputParamApiRegister;
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

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới và nhận token")
    public Mono<OutputParamApiRegister> register(@RequestBody InputParamApiRegister inputParamApiRegister) {
        return authService.register(inputParamApiRegister);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng và nhận token")
    public Mono<OutputParamApiLogin> login(@RequestBody InputParamApiLogin inputParamApiLogin) {
        return authService.login(inputParamApiLogin);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới token", description = "Cung cấp token mới dựa trên token cũ")
    public Mono<OutputParamApiRefreshToken> refresh(@RequestBody InputParamApiRefreshToken inputParamApiRefreshToken) {
        return authService.refreshToken(inputParamApiRefreshToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Xóa token hiện tại")
    public Mono<Void> logout(@RequestBody String refreshToken) {
        return authService.logout(refreshToken);
    }
}
