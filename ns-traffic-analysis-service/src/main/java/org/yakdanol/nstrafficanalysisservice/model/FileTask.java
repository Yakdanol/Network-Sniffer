package org.yakdanol.nstrafficanalysisservice.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FileTask {
    private List<String> fileNames;
    private String path;
}
