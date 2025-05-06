package org.yakdanol.nstrafficsecurityservice.users.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.yakdanol.nstrafficsecurityservice.service.DataSource;

@Getter
@Setter
@AllArgsConstructor
public class SecurityAnalysisRequest {
    private final String fullUserName;
    private final DataSource type;
}
