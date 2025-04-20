//package org.yakdanol.nstrafficanalysisservice.service.strategy;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@Component
//public class FileBatchReader implements AutoCloseable {
//
//    private final TrafficAnalysisConfig config;
//    private final String format;
//    private final BufferedReader reader;
//
//    @Autowired
//    public FileBatchReader(TrafficAnalysisConfig config) throws IOException {
//        this.config = config;
//        this.reader = new BufferedReader(new FileReader(config.getFile().getDirectory()));
//        this.format = config.getFile().getIncomingFormat();
//    }
//
//    // Читаем N строк (batch)
//    public List<String> readNextBatch(int batchSize) throws IOException {
//        List<String> batch = new ArrayList<>();
//        for (int i = 0; i < batchSize; i++) {
//            String line = reader.readLine();
//            if (line == null) {
//                break; // конец файла
//            }
//            batch.add(line);
//        }
//        return batch.isEmpty() ? null : batch;
//    }
//
//    @Override
//    public void close() throws IOException {
//        reader.close();
//    }
//}
