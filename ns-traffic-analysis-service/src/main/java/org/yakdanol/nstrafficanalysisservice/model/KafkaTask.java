package org.yakdanol.nstrafficanalysisservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTask {
    private String topicName;
    private String userName;
}
