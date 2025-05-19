package org.yakdanol.nstrafficanalysisservice.service.analysis;

import org.pcap4j.packet.Packet;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AnalysisHandler {
    /** Категория источника (PHISHING, ADVERTISING …). */
    String category();

    /**
     * Инициализирует внутреннюю Redis-базу: чистит key и загружает IP‑лист из файла.
     * Вызывается при старте контейнера.
     */
    default void preload() throws IOException, URISyntaxException { return; }

    /**
     * Проверка пакета на безопасность. В случае обнаружения угрозы обработчик сам публикует
     * уведомление и возвращает {@code true}, иначе {@code false}.
     */
    default boolean checkSecurity(Packet packet, String userFullName) { return false; }

    /**
     * Проверка доменов пакетов
     */
    default boolean checkSecurity(String domain, String user) { return false; }
}
