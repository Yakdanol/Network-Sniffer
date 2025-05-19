package org.yakdanol.nstrafficanalysisservice.users.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.yakdanol.nstrafficanalysisservice.service.DataSource;

@Getter
@Setter
@AllArgsConstructor
public class AnalysisRequest {
    private final String fullUserName;
    private final DataSource type;
}
