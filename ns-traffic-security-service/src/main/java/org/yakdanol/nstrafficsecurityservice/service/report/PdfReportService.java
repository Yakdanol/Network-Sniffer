package org.yakdanol.nstrafficsecurityservice.service.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatManager;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PdfReportService implements ReportService {
    Logger logger = LoggerFactory.getLogger(PdfReportService.class);
    private static final Path OUT_DIR = Path.of("ns-traffic-security-service/src/main/resources/data/reports");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /**
     * Создаёт pdf с помощью OpenPDF.
     */
    @Override
    public void buildReport(String user, LocalDateTime startTime, LocalDateTime finishTime,
                            long packets, List<ThreatManager.DetectedThreat> threats) {
        logger.info("Building report for {}", user);
        try (var document = new com.lowagie.text.Document()) {
            String fileName = user + "." + LocalDateTime.now().format(DATE) + ".pdf";
            Files.createDirectories(OUT_DIR);
            var pdfWriter = PdfWriter.getInstance(document, Files.newOutputStream(OUT_DIR.resolve(fileName)));
            document.open();

            document.add(new Paragraph("Traffic security report for user: " + user));
            document.add(new Paragraph("Generated at: " + finishTime.format(TIMESTAMP)));
            document.add(new Paragraph("Analysis period : " + startTime.format(TIMESTAMP) + " – " + finishTime.format(TIMESTAMP)));
            document.add(new Paragraph("Analysed packets: " + packets));
            document.add(new Paragraph("Threats found: " + threats.size()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            Stream.of("IP", "Category", "Time").forEach(h -> {
                PdfPCell cell = new PdfPCell(new Phrase(h));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            });
            for (var threat : threats) {
                table.addCell(threat.ip());
                table.addCell(threat.category());
                table.addCell(threat.when().format(TIMESTAMP));
            }
            document.add(table);
            document.close();
            pdfWriter.close();
        } catch (Exception e) {
            logger.error("PDF failed", e);
        }
    }
}
