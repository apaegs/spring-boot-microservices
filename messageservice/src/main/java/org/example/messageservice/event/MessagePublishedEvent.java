package org.example.messageservice.event;

public record MessagePublishedEvent(
        Long messageId,
        String senderUsername,
        String content
) {}
