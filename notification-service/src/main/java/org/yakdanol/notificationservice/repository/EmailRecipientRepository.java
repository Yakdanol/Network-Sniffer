package org.yakdanol.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yakdanol.notificationservice.model.EmailRecipient;

public interface EmailRecipientRepository extends JpaRepository<EmailRecipient, Long> {
}
