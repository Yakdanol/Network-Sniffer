//package org.yakdanol.nstrafficanalysisservice.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;
//import org.yakdanol.nstrafficanalysisservice.service.AnalysisOrchestrator;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/analysis")
//@RequiredArgsConstructor
//public class AnalysisController {
//
//    private final AnalysisOrchestrator orchestrator;
//
//    @PostMapping("/tasks/add")
//    public String addTasks(@RequestBody List<AnalysisTask> tasks) {
//        orchestrator.addTasks(tasks);
//        return "Tasks added: " + tasks.size();
//    }
//
//    @PostMapping("/tasks/start")
//    public String startAll() {
//        orchestrator.startAll();
//        return "Analysis started";
//    }
//
//    @PostMapping("/stop")
//    public String stopAll() {
//        orchestrator.stopAll();
//        return "All analyses stopped";
//    }
//
//    @GetMapping("/status")
//    public String getStatus() {
//        // Упрощенный вариант
//        return "Status not fully implemented. Possibly show queued tasks, current tasks, etc.";
//    }
//
//    @GetMapping("/report")
//    public String getReport() {
//        return "No real report yet. Implement downloading from ReportGeneratorService if needed.";
//    }
//}
