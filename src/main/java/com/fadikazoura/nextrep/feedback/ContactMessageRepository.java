package com.fadikazoura.nextrep.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    List<ContactMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
}
