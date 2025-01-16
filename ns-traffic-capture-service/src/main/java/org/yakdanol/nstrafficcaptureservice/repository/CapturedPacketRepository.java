package org.yakdanol.nstrafficcaptureservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.service.TrafficCaptureService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

@Slf4j
@Repository
public class CapturedPacketRepository {
    @Value("${traffic-capture.log-directory}")
    private String logDirectory;

    @Value("${traffic-capture.log-format}")
    private String logFormat;

    private static final Logger logger = LoggerFactory.getLogger(CapturedPacketRepository.class);
    private BufferedWriter bufferedWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CapturedPacketRepository() throws IOException {
        openLogFile();
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
                default -> objectMapper.writeValueAsString(packet);
            };
            bufferedWriter.write(logEntry);
            bufferedWriter.newLine();
        } catch (IOException e) {
            logger.error("Error writing packet to file", e);
        }
    }

//    private String convertToXml(CapturedPacket packet) {
//        // Реализация преобразования объекта в XML с использованием JAXB
//        // Для упрощения, возвращаем строковое представление
//        return "<CapturedPacket>" +
//                "<timestamp>" + packet.getTimestamp() + "</timestamp>" +
//                "<sourceIp>" + packet.getSourceIp() + "</sourceIp>" +
//                "<destinationIp>" + packet.getDestinationIp() + "</destinationIp>" +
//                "<protocol>" + packet.getProtocol() + "</protocol>" +
//                "<length>" + packet.getLength() + "</length>" +
//                "<data>" + packet.getData() + "</data>" +
//                "</CapturedPacket>";
//    }

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
