package org.yakdanol.nstrafficanalysisservice.service.domain.packet;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Класс со статическими методами для парсинга TLS ClientHello и извлечения SNI.
 */
@Slf4j
public class TlsParser {

    /**
     * Ищем в массиве байт TLS Record (type=0x16 Handshake),
     * парсим ClientHello (0x01) и находим extension "server_name" (0x0000).
     *
     * @param data - общий массив байт, склеенный из TCP-сегментов
     * @return строка SNI или null, если не найдена
     */
    public static String extractSniFromTls(byte[] data) {
        int index = 0;
        while (index + 5 <= data.length) {
            // Проверяем ContentType = 0x16 (Handshake)
            if ((data[index] & 0xFF) == 0x16) {
                // Версия TLS (major=0x03, minor=0x01..0x04)
                int major = data[index+1] & 0xFF;
                int minor = data[index+2] & 0xFF;
                // Длина рекорда
                int recordLen = ((data[index+3] & 0xFF) << 8) | (data[index+4] & 0xFF);

                if (major == 0x03) {
                    // Проверяем, есть ли у нас достаточно байт
                    if (index + 5 + recordLen > data.length) {
                        // данных не хватает для полного рекорда
                        log.debug("extractSniFromTls: не хватает байт для полного TLS Record, index={} recordLen={}", index, recordLen);
                        break;
                    }

                    // Проверяем тип Handshake (1 байт: data[index+5])
                    int handshakeType = data[index+5] & 0xFF;
                    if (handshakeType == 0x01) {
                        // HandshakeType = ClientHello
                        log.debug("extractSniFromTls: найден ClientHello (index={})", index);
                        String sni = parseClientHelloForSni(data, index + 5, recordLen);
                        if (sni != null) {
                            return sni;
                        }
                    }
                    // Переходим за этот record
                    index += 5 + recordLen;
                } else {
                    // не поддерживаем, сдвигаемся на 1
                    index++;
                }
            } else {
                index++;
            }
        }

        log.debug("extractSniFromTls: SNI не найден");
        return null;
    }

    /**
     * Парсим Handshake(ClientHello) структуру: ищем extensions, среди них - "server_name"
     *
     * @param data - общий массив
     * @param handshakeStart - позиция, где начинается Handshake (тип 0x01)
     * @param recordLen - длина TLS-рекорда
     * @return SNI или null
     */
    private static String parseClientHelloForSni(byte[] data, int handshakeStart, int recordLen) {
        // HandshakeHeader = 1 байт (type=0x01), 3 байта length
        if (handshakeStart + 4 > data.length) {
            log.debug("parseClientHelloForSni: недостаточно байт для HandshakeHeader");
            return null;
        }
        int handshakeLen = ((data[handshakeStart+1] & 0xFF) << 16)
                | ((data[handshakeStart+2] & 0xFF) << 8)
                | (data[handshakeStart+3] & 0xFF);

        // Проверяем, укладывается ли handshake в текущий record
        if (handshakeLen + 4 > recordLen) {
            return null;
        }

        // Смещаемся на тело ClientHello
        int position = handshakeStart + 4;
        int end = position + handshakeLen; // конец ClientHello

        // Минимальные проверки (версия, random, sessionId, cipherSuites, compression)
        // 2 байта версии + 32 random + 1 байт sessionLen + session + 2 байта cipherLen + ... + 1 байт compLen
        if (position + 2 + 32 + 1 > end) return null;
        position += 2 + 32; // skip version + random

        int sessionIdLen = data[position] & 0xFF;
        position++;
        if (position + sessionIdLen > end) return null;
        position += sessionIdLen;

        if (position + 2 > end) return null;
        int cipherLen = ((data[position] & 0xFF) << 8) | (data[position+1] & 0xFF);
        position += 2;
        if (position + cipherLen > end) return null;
        position += cipherLen;

        if (position + 1 > end) return null;
        int compLen = data[position] & 0xFF;
        position++;
        if (position + compLen > end) return null;
        position += compLen;

        // Теперь extensionsLength
        if (position + 2 > end) return null;
        int extLen = ((data[position] & 0xFF) << 8) | (data[position+1] & 0xFF);
        position += 2;
        if (position + extLen > end) return null;

        // Перебираем extensions
        int extEnd = position + extLen;
        while (position + 4 <= extEnd) {
            int extType = ((data[position] & 0xFF) << 8) | (data[position+1] & 0xFF);
            int length = ((data[position+2] & 0xFF) << 8) | (data[position+3] & 0xFF);
            int extDataStart = position + 4;
            int extDataEnd = extDataStart + length;
            if (extDataEnd > extEnd) {
                break; // выход за границы
            }

            if (extType == 0x0000) { // server_name
                String sni = parseServerNameExtension(data, extDataStart, length);
                if (sni != null) {
                    return sni;
                }
            }

            position = extDataEnd;
        }

        return null;
    }

    /**
     * Парсим extension type=0x0000 (server_name).
     * Формат: 2 байта server_name_list_length,
     *         затем [1 байт name_type, 2 байта name_len, name...], и т.д.
     */
    private static String parseServerNameExtension(byte[] data, int start, int length) {
        log.debug("Зашли в метод поиска Имени сервера parseServerNameExtension");
        ByteBuffer buffer = ByteBuffer.wrap(data, start, length);
        if (buffer.remaining() < 2) {
            log.debug("parseServerNameExtension: мало байт для server_name_list_length");
            return null;
        }
        int serverNameListLen = buffer.getShort() & 0xFFFF;
        if (serverNameListLen > buffer.remaining()) {
            return null; // некорректно
        }

        // читаем записи до конца serverNameListLen
        int limit = buffer.position() + serverNameListLen;
        while (buffer.position() + 3 <= limit) {
            int nameType = buffer.get() & 0xFF; // 0 = host_name
            int nameLen = buffer.getShort() & 0xFFFF;
            if (buffer.position() + nameLen > limit) {
                break;
            }

            if (nameType == 0x00) {
                byte[] sniBytes = new byte[nameLen];
                buffer.get(sniBytes);
                log.debug("Получили имя сервера");
                return new String(sniBytes, StandardCharsets.UTF_8);
            } else {
                // пропускаем, если nameType != 0
                buffer.position(buffer.position() + nameLen);
            }
        }

        return null;
    }
}
