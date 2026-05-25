package org.example.messageservice;

public record MessagePublishedEvent(
        Long messageId,
        String senderUsername,
        String content
) {}
