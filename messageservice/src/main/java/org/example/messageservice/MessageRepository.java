package org.example.messageservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByOrderBySentAtAsc();
    Page<Message> findAllByOrderBySentAtAsc(Pageable pageable);
}
