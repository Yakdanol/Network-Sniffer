package org.yakdanol.nstrafficcaptureservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

@Slf4j
@NoArgsConstructor
@Repository
public class CapturedPacketRepository {
    @Value("${traffic-capture.log-directory}")
    private String logDirectory;

    @Value("${traffic-capture.log-format}")
    private String logFormat;

    private static final Logger logger = LoggerFactory.getLogger(CapturedPacketRepository.class);
    private BufferedWriter bufferedWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void init() throws IOException {
        openLogFile(); // Открытие файла после инициализации
    }

    private synchronized void openLogFile() throws IOException {
        String fileName = String.format("%s/%s.json", logDirectory, LocalDate.now());
        bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
        logger.info("Opened log file: {}", fileName);
    }

    public synchronized void save(CapturedPacket packet) {
        try {
            String logEntry = switch (logFormat.toLowerCase()) {
                case "text" -> packet.toString();
                case "xml" -> convertToXml(packet);
                default -> objectMapper.writeValueAsString(packet);
            };
            bufferedWriter.write(logEntry);
            bufferedWriter.newLine();
        } catch (IOException e) {
            logger.error("Error writing packet to file", e);
        }
    }

    private String convertToXml(CapturedPacket packet) {
        // Реализация преобразования объекта в XML с использованием JAXB
        // Для упрощения, возвращаем строковое представление
        StringBuilder xmlBuilder = new StringBuilder(2048)
                .append("<CapturedPacket>")
                .append("<timestamp>").append(packet.getTimestamp()).append("</timestamp>")
                .append("<sourceIp>").append(packet.getSourceIp()).append("</sourceIp>")
                .append("<destinationIp>").append(packet.getDestinationIp()).append("</destinationIp>")
                .append("<protocol>").append(packet.getProtocol()).append("</protocol>")
                .append("<length>").append(packet.getLength()).append("</length>")
                .append("<data>").append(packet.getData()).append("</data>")
                .append("</CapturedPacket>");

        return xmlBuilder.toString();
    }

    public synchronized void rotateLogFile() {
        try {
            bufferedWriter.flush();
            bufferedWriter.close();
            logger.info("Closed current log file.");
            openLogFile();
        } catch (IOException e) {
            logger.error("Error rotating log file", e);
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
                logger.info("Closed log file.");
            }
        } catch (IOException e) {
            logger.error("Error closing BufferedWriter", e);
        }
    }
}
