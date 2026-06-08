package spring_reactive_security.com.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import spring_reactive_security.com.dto.AuthResponse;
import spring_reactive_security.com.entity.User;
import spring_reactive_security.com.exception.InvalidTokenException;
import spring_reactive_security.com.param.AuthRequest;
import spring_reactive_security.com.param.RegisterRequest;
import spring_reactive_security.com.repository.UserRepository;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<AuthResponse> authenticate(AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid username or password")))
                .flatMap(user -> {
                    if (!user.isEnabled() || !user.isActive() || user.isLocked()) {
                        return Mono.error(new BadCredentialsException("Account status is invalid"));
                    }
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid username or password"));
                    }
                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user);
                })
                .map(this::createAuthResponse);
    }

    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> exists ? Mono.error(new IllegalArgumentException("Username already exists")) : userRepository.existsByEmail(request.getEmail()))
                .flatMap(exists -> exists ? Mono.error(new IllegalArgumentException("Email already exists")) : Mono.empty())
                .then(Mono.defer(() -> {
                    User user = new User();
                    user.setUsername(request.getUsername());
                    user.setEmail(request.getEmail());
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    user.setRoles(Set.of("USER"));
                    user.setCreatedAt(Instant.now());
                    user.setActive(true);
                    user.setEnabled(true);
                    user.setLocked(false);
                    return userRepository.save(user);
                }))
                .map(this::createAuthResponse);
    }

    public Mono<AuthResponse> refreshToken(String refreshToken) {
        return jwtService.isRefreshToken(refreshToken)
                .flatMap(isValid -> !isValid ? Mono.error(new InvalidTokenException("Invalid refresh token")) : jwtService.extractUsername(refreshToken))
                .flatMap(userRepository::findByUsername)
                .filter(user -> user.isEnabled() && user.isActive() && !user.isLocked())
                .switchIfEmpty(Mono.error(new InvalidTokenException("User account is locked or unavailable")))
                .map(this::createAuthResponse);
    }

    private AuthResponse createAuthResponse(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        String accessToken = jwtService.generateAccessToken(user.getUsername(), authorities);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return new AuthResponse(accessToken, refreshToken, null);
    }
}
