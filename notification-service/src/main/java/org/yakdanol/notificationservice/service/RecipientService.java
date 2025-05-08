package org.yakdanol.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yakdanol.notificationservice.model.EmailRecipient;
import org.yakdanol.notificationservice.model.TelegramRecipient;
import org.yakdanol.notificationservice.repository.EmailRecipientRepository;
import org.yakdanol.notificationservice.repository.TelegramRecipientRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientService {

    private final EmailRecipientRepository emailRecipientRepository;
    private final TelegramRecipientRepository telegramRecipientRepository;
    private final ConcurrentHashMap<String, List<String>> recipientsMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadRecipients();
    }

    // Метод вызывается планировщиком ежедневно
    public void refreshRecipients() {
        log.info("Refreshing recipient lists from database.");
        loadRecipients();
    }

    private void loadRecipients() {
        List<EmailRecipient> emailRecipients = emailRecipientRepository.findAll();
        List<TelegramRecipient> telegramRecipients = telegramRecipientRepository.findAll();

        List<String> emailList = emailRecipients.stream()
                .map(EmailRecipient::getEmail)
                .toList();

        List<String> telegramList = telegramRecipients.stream()
                .map(TelegramRecipient::getChatId)
                .toList();

        recipientsMap.put("EMAIL", emailList);
        recipientsMap.put("TELEGRAM", telegramList);

        log.info("Loaded {} email recipients and {} telegram recipients.", emailList.size(), telegramList.size());
    }

    public List<String> getEmailRecipients() {
        return recipientsMap.getOrDefault("EMAIL", List.of());
    }

    public List<String> getTelegramRecipients() {
        return recipientsMap.getOrDefault("TELEGRAM", List.of());
    }
}
