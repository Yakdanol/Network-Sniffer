package org.yakdanol.admininterface.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;
import org.yakdanol.admininterface.vaadin.MainView;

@Configuration
public class WebClientConfig {

    @Autowired
    private Environment env;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    /** Получаем базовый URL по типу сервиса */
    public String baseUrl(MainView.ServiceKind kind) {
        return env.getProperty("admin-ui.services." +
                (kind == MainView.ServiceKind.ANALYSIS ? "analysis" : "security"));
    }
}
