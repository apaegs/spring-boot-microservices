package org.example.messageservice;

import org.example.messageservice.dto.MessageRequest;
import org.example.messageservice.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private static final int MAX_PAGE_SIZE = 100;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @RequestBody @Valid MessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String senderUsername = jwt.getSubject();
        return ResponseEntity.ok(messageService.send(senderUsername, request));
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getAll() {
        return ResponseEntity.ok(messageService.getAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<MessageResponse>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be > 0 and <= " + MAX_PAGE_SIZE);
        }
        return ResponseEntity.ok(messageService.getAll(page, size));
    }
}
