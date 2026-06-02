package org.example.userservice.controller;

import jakarta.validation.Valid;
import org.example.userservice.dto.UserAuthDto;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.dto.UserUpdateRequest;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<UserResponse> create(@RequestBody UserRequest request) {
        // Reject duplicates with a clear 409 instead of surfacing a DB error.
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        try {
            User user = new User(
                    request.username(),
                    passwordEncoder.encode(request.password()),
                    request.email()
            );
            User saved = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail()));
        } catch (DataIntegrityViolationException e) {
            // Catches the unique constraint on email (or a race on username).
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already taken");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
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

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponse> getByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getUsername(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/auth/{username}")
    public ResponseEntity<UserAuthDto> getForAuth(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(new UserAuthDto(u.getUsername(), u.getPassword())))
                .orElse(ResponseEntity.notFound().build());
    }
}
