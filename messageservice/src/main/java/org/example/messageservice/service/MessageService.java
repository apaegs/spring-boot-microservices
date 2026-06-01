package org.example.messageservice.service;


import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.example.messageservice.event.MessagePublishedEvent;
import org.example.messageservice.dto.MessageRequest;
import org.example.messageservice.dto.MessageResponse;
import org.example.messageservice.model.Message;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxRepository;
import org.example.userservice.grpc.GetUserByUsernameRequest;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;


@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    private static final long GRPC_TIMEOUT_MS = 3000;

    public MessageService(MessageRepository messageRepository,
                          OutboxRepository outboxRepository,
                          ObjectMapper objectMapper,
                          UserServiceGrpc.UserServiceBlockingStub userServiceStub) {
        this.messageRepository = messageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.userServiceStub = userServiceStub;
    }

    /**
     * Accepts a message, verifies the sender via gRPC against the user-service,
     * persists the message, and writes a "message-published" event to the outbox
     * table within the same transaction. The outbox event is later published to
     * RabbitMQ by {@link OutboxPublisher}, guaranteeing that the message and the
     * event are either both saved or neither is (transactional outbox).
     *
     * @param senderUsername the username taken from the authenticated user's JWT
     * @param request        the message content to send
     * @return a {@link MessageResponse} containing the saved message's data
     */
    @Transactional
    public MessageResponse send(String senderUsername, MessageRequest request) {
        String verifiedUsername = fetchUsername(senderUsername);

        Message message = new Message(verifiedUsername, request.content());
        Message saved = messageRepository.save(message);

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

            UserServiceGrpc.UserServiceBlockingStub stubWithAuth = userServiceStub
                    .withDeadlineAfter(GRPC_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                    .setUsername(username)
                    .build();
            return stubWithAuth.getUserByUsername(request).getUsername();
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
            }
            throw new IllegalStateException("Failed to verify user: " + username, e);
        }
    }
}
