package org.example.userservice;

import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getUsername(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserResponse create(@RequestBody UserRequest request) {
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.email()
        );
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody UserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.username());
                    user.setEmail(request.email());
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
