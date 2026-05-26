package org.example.messageservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatusIn(
                List.of(OutboxEvent.OutboxStatus.PENDING, OutboxEvent.OutboxStatus.FAILED)
        );

        for (OutboxEvent event : events) {
            if (event.getRetryCount() >= MAX_RETRIES) {
                log.warn("Skipping event {} after {} retries", event.getId(), event.getRetryCount());
                continue;
            }
            try {
                MessagePublishedEvent payload = objectMapper.readValue(
                        event.getPayload(), MessagePublishedEvent.class
                );
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, payload);
                event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                outboxRepository.save(event);
                log.info("Published outbox event {}", event.getId());
            } catch (Exception e) {
                event.incrementRetryCount();
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                outboxRepository.save(event);
                log.error("Failed to publish outbox event {} (attempt {}): {}", event.getId(), event.getRetryCount(), e.getMessage());
            }
        }
    }
}
