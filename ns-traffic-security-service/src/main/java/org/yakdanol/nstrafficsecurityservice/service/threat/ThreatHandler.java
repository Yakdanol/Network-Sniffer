package org.yakdanol.nstrafficsecurityservice.service.threat;

import org.pcap4j.packet.Packet;
import java.io.IOException;

public interface ThreatHandler {
    /** Категория источника (PHISHING, ADVERTISING …). */
    String category();

    /**
     * Инициализирует внутреннюю Redis-базу: чистит key и загружает IP‑лист из файла.
     * Вызывается при старте контейнера.
     */
    void preload() throws IOException;

    /**
     * Проверка пакета на безопасность. В случае обнаружения угрозы обработчик сам публикует
     * уведомление и возвращает {@code true}, иначе {@code false}.
     */
    boolean checkSecurity(Packet packet, String userFullName);
}
