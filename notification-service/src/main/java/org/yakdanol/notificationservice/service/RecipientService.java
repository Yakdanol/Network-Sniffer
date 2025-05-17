package org.yakdanol.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yakdanol.notificationservice.model.EmailRecipient;
import org.yakdanol.notificationservice.model.TelegramRecipient;
import org.yakdanol.notificationservice.repository.EmailRecipientRepository;
import org.yakdanol.notificationservice.repository.TelegramRecipientRepository;
import org.yakdanol.notificationservice.users.UserNotificationTargetsRepository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientService {

    private final EmailRecipientRepository emailRepo;
    private final TelegramRecipientRepository tgRepo;
    private final UserNotificationTargetsRepository routeRepo;

    private final Map<String, List<String>> globalEmails = new ConcurrentHashMap<>();
    private final Map<String, List<String>> globalTelegrams = new ConcurrentHashMap<>();

    private final Map<String, List<String>> personalEmails = new ConcurrentHashMap<>();
    private final Map<String, List<String>> personalTelegrams = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadRecipients();
    }

    /** Вызывается планировщиком раз в сутки. */
    public void refreshRecipients() {
        log.info("Refreshing recipient lists from Database");
        loadRecipients();
    }

    private void loadRecipients() {
        // ---------- глобальные ----------
        List<String> emails = emailRepo.findAll().stream().map(EmailRecipient::getEmail).toList();
        List<String> telegrams = tgRepo.findAll().stream().map(TelegramRecipient::getChatId).toList();
        globalEmails.put("ALL", emails);
        globalTelegrams.put("ALL", telegrams);

        // ---------- персональные ----------
        personalEmails.clear();
        personalTelegrams.clear();
        routeRepo.findAll().forEach(r -> {
            personalEmails.put(r.getMonitoredInternalUserName(), new ArrayList<>(r.getEmails()));
            personalTelegrams.put(r.getMonitoredInternalUserName(), new ArrayList<>(r.getTelegramChatIds()));
        });

        log.info("Loaded: {} global emails, {} global telegrams; {} personal routes", emails.size(), telegrams.size(), personalEmails.size());
    }

    // ----------------- API для отправителей -----------------

    public List<String> getEmailRecipients(String internalUserName) {
        return merge(globalEmails.get("ALL"), personalEmails.get(internalUserName));
    }

    public List<String> getTelegramRecipients(String internalUserName) {
        return merge(globalTelegrams.get("ALL"), personalTelegrams.get(internalUserName));
    }

    private List<String> merge(List<String> globals, List<String> personals) {
        if (globals == null && personals == null) return List.of();
        Set<String> set = new LinkedHashSet<>();
        if (globals != null) set.addAll(globals);
        if (personals != null) set.addAll(personals);

        return List.copyOf(set);
    }
}
