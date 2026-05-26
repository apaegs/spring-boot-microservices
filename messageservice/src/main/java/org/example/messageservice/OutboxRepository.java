package org.example.messageservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
    List<OutboxEvent> findByStatusIn(List<OutboxEvent.OutboxStatus> statuses);
}
