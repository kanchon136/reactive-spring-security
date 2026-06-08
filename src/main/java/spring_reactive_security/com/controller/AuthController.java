package spring_reactive_security.com.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import spring_reactive_security.com.dto.AuthResponse;
import spring_reactive_security.com.param.AuthRequest;
import spring_reactive_security.com.param.RefreshRequest;
import spring_reactive_security.com.param.RegisterRequest;
import spring_reactive_security.com.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public Mono<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }
}
