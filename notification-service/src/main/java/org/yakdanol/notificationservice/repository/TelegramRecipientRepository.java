package org.yakdanol.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yakdanol.notificationservice.model.TelegramRecipient;

public interface TelegramRecipientRepository extends JpaRepository<TelegramRecipient, Long> {
}
