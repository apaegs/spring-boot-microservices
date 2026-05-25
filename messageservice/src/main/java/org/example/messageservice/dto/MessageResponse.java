package org.example.messageservice.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String senderUsername,
        String content,
        LocalDateTime sentAt
) {}
