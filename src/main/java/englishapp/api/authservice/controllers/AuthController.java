package englishapp.api.authservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import englishapp.api.authservice.dto.register.BodyParamRegister;
import englishapp.api.authservice.services.UserService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody BodyParamRegister request) {
        return userService.registerUser(request)
            .map(user -> ResponseEntity.ok("User registered successfully!"))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }
}
