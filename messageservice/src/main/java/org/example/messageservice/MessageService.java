package org.example.messageservice;

import org.example.messageservice.dto.MessageRequest;
import org.example.messageservice.dto.MessageResponse;
import org.example.userservice.grpc.GetUserByUsernameRequest;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public MessageService(MessageRepository messageRepository,
                          RabbitTemplate rabbitTemplate,
                          UserServiceGrpc.UserServiceBlockingStub userServiceStub) {
        this.messageRepository = messageRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.userServiceStub = userServiceStub;
    }

    public MessageResponse send(String senderUsername, MessageRequest request) {
        String verifiedUsername = fetchUsername(senderUsername);

        Message message = new Message(verifiedUsername, request.content());
        Message saved = messageRepository.save(message);

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                new MessagePublishedEvent(saved.getId(), verifiedUsername, saved.getContent())
        );

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
            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                    .setUsername(username)
                    .build();
            return userServiceStub.getUserByUsername(request).getUsername();
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            // Fallback vid nätverksfel eller andra gRPC-fel
            return username;
        }
    }
}
