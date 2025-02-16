package org.yakdanol.nstrafficcaptureservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

@Repository
public class CapturedPacketRepository {

    private final TrafficCaptureConfig config;
    private final PacketToJsonConverter converter;
    private static final Logger logger = LoggerFactory.getLogger(CapturedPacketRepository.class);
    private BufferedWriter bufferedWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String logFormat;

    @Autowired
    public CapturedPacketRepository(TrafficCaptureConfig config, PacketToJsonConverter converter) {
        this.config = config;
        this.logFormat = config.getLogFormat();
        this.converter = converter;
    }

    @PostConstruct
    private void init() throws IOException {
        openLogFile(); // Открытие файла после инициализации
    }

    private synchronized void openLogFile() throws IOException {
        String fileName = String.format("%s/%s.%s", config.getLogDirectory(), LocalDate.now(), config.getLogFormat());
        bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
        logger.info("Opened log file: {}", fileName);
    }

    public synchronized void save(Packet packet) {
        CapturedPacket capturedPacket = converter.convert(packet);
        try {
            String logEntry = switch (logFormat) {
                case "text" -> capturedPacket.toString();
                case "xml" -> convertToXml(capturedPacket);
                case "csv" -> convertToCsv(capturedPacket);
                default -> objectMapper.writeValueAsString(capturedPacket);
            };
            bufferedWriter.write(logEntry);
            bufferedWriter.newLine();
        } catch (IOException e) {
            logger.error("Error writing packet to file", e);
        }
    }

    private String convertToCsv(CapturedPacket packet) {
        // Собираем данные в строку CSV, экранируем поля с запятыми и кавычками
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(escapeCsvField(packet.getTimestamp())).append(",");
        csvBuilder.append(escapeCsvField(packet.getSourceIp())).append(",");
        csvBuilder.append(escapeCsvField(packet.getDestinationIp())).append(",");
        csvBuilder.append(escapeCsvField(packet.getProtocol())).append(",");
        csvBuilder.append(packet.getLength()).append(",");
        csvBuilder.append(escapeCsvField(packet.getData()));

        return csvBuilder.toString();
    }

    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }

        // Экранируем кавычки и заменяем их на две кавычки внутри строки
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String convertToXml(CapturedPacket packet) {
        // Реализация преобразования объекта в XML с использованием JAXB
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
