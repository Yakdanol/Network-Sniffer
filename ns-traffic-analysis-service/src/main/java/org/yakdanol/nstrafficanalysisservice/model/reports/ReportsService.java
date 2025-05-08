//package org.yakdanol.nstrafficanalysisservice.model.reports;
//
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class ReportsService {
//    private final static Logger logger = LoggerFactory.getLogger(ReportsService.class);
//    private final ReportsRepository reportRepository;
//
//    /**
//     * Возвращает все отчёты.
//     */
//    public List<Reports> findAll() {
//        logger.debug("Получаем все отчёты");
//        return reportRepository.findAll();
//    }
//
//    /**
//     * Ищет отчёт по id.
//     */
//    public Reports findById(Long id) {
//        logger.debug("Ищем отчёт с id={}", id);
//        return reportRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reports not found with id=" + id));
//    }
//
//    /**
//     * Создаёт новый отчёт.
//     */
//    public Reports create(Reports report) {
//        logger.info("Создаём новый отчёт для {}", report.getFullName());
//        return reportRepository.save(report);
//    }
//
//    /**
//     * Обновляет существующий отчёт.
//     */
//    public Reports update(Long id, Reports updated) {
//        Reports existing = findById(id);
//        logger.info("Обновляем отчёт id={}", id);
//
//        existing.setFullName(updated.getFullName());
//        existing.setInternalUserName(updated.getInternalUserName());
//        existing.setPosition(updated.getPosition());
//        existing.setDataSource(updated.getDataSource());
//        existing.setTotalNumberPackages(updated.getTotalNumberPackages());
//        existing.setDateAnalysis(updated.getDateAnalysis());
//        existing.setDurationAnalysis(updated.getDurationAnalysis());
//
//        return reportRepository.save(existing);
//    }
//
//    /**
//     * Удаляет отчёт по id.
//     */
//    public void delete(Long id) {
//        Reports existing = findById(id);
//        logger.info("Удаляем отчёт id={}", id);
//        reportRepository.delete(existing);
//    }
//}
