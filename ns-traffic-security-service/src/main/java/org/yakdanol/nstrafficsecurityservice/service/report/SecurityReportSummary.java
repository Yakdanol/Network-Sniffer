package org.yakdanol.nstrafficsecurityservice.service.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_report_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityReportSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @JsonFormat(pattern = "yyyy:MM:dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Column(length = 8)
    private String duration;

    private long packetsProcessed;

    private int threatsFound;
}
