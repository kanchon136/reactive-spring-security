package spring_reactive_security.com.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring_reactive_security.com.dto.UserDto;
import spring_reactive_security.com.entity.User;
import spring_reactive_security.com.repository.UserRepository;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserDto> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDto);
    }

    public Flux<UserDto> findAll() {
        return userRepository.findAll().map(this::toDto);
    }

    public Mono<UserDto> findById(String id) {
        return userRepository.findById(id).map(this::toDto);
    }

    public Mono<UserDto> updateRoles(String id, Set<String> roles) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setRoles(roles);
                    return userRepository.save(user);
                }).map(this::toDto);
    }

    public Mono<UserDto> update(String id, UserDto dto) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setEmail(dto.getEmail());
                    user.setUsername(dto.getUsername());
                    return userRepository.save(user);
                }).map(this::toDto);
    }

    public Mono<Void> delete(String id) {
        return userRepository.deleteById(id);
    }

    public Mono<Boolean> isOwner(String id, String authenticatedUsername) {
        return userRepository.findById(id)
                .map(user -> user.getUsername().equalsIgnoreCase(authenticatedUsername))
                .defaultIfEmpty(false);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
}
