package org.example.messageservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.example.messageservice.dto.MessageRequest;
import org.example.messageservice.dto.MessageResponse;
import org.example.userservice.grpc.GetUserByUsernameRequest;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public MessageService(MessageRepository messageRepository,
                          OutboxRepository outboxRepository,
                          ObjectMapper objectMapper,
                          UserServiceGrpc.UserServiceBlockingStub userServiceStub) {
        this.messageRepository = messageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.userServiceStub = userServiceStub;
    }

    @Transactional
    public MessageResponse send(String senderUsername, MessageRequest request) {
        String verifiedUsername = fetchUsername(senderUsername);

        // 1. Spara meddelandet
        Message message = new Message(verifiedUsername, request.content());
        Message saved = messageRepository.save(message);

        // 2. Spara outbox event i samma transaktion
        MessagePublishedEvent event = new MessagePublishedEvent(
                saved.getId(), verifiedUsername, saved.getContent()
        );
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent("MESSAGE_PUBLISHED", payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        return new MessageResponse(saved.getId(), verifiedUsername, saved.getContent(), saved.getSentAt());
    }

    public List<MessageResponse> getAll() {
        return messageRepository.findAllByOrderBySentAtAsc().stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getSenderUsername(),
                        m.getContent(),
                        m.getSentAt()
                ))
                .toList();
    }

    public Page<MessageResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findAllByOrderBySentAtAsc(pageable)
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getSenderUsername(),
                        m.getContent(),
                        m.getSentAt()
                ));
    }

    private String fetchUsername(String username) {
        try {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            Metadata metadata = new Metadata();
            metadata.put(
                    Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                    "Bearer " + jwt.getTokenValue()
            );
            UserServiceGrpc.UserServiceBlockingStub stub = userServiceStub
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                    .setUsername(username)
                    .build();
            return stub.getUserByUsername(request).getUsername();
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            return username;
        }
    }
}
