package org.yakdanol.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "telegramconfig")
@Setter
@Getter
public class TelegramConfig {

    private boolean telegramEnabled;
    private String telegramBotToken;
    private int telegramConnectionPoolSize;

    @Bean(name = "telegramRestTemplate")
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(telegramConnectionPoolSize);
        connectionManager.setDefaultMaxPerRoute(telegramConnectionPoolSize);

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Bean
    public String telegramBotToken() {
        return telegramBotToken;
    }

    @Bean
    public boolean isTelegramEnabled() {
        return telegramEnabled;
    }
}
