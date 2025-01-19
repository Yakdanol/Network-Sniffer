package org.yakdanol.nstrafficcaptureservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ConfigService<T> {

    private final ConfigurableEnvironment environment;

    @Autowired
    public ConfigService(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    // Метод для обновления параметров конфигурации в реальном времени
    public void updateTrafficCaptureConfig(String key, T value) {
        MutablePropertySources propertySources = environment.getPropertySources();

        // Проверяем, существует ли источник с таким именем
        if (propertySources.contains("customTrafficCaptureConfig")) {
            // Если источник существует, получаем его
            MapPropertySource propertySource = (MapPropertySource) propertySources.get("customTrafficCaptureConfig");

            // Обновляем значение параметра в источнике
            propertySource.getSource().put(key, value);
        } else {
            // Если источника нет, создаем новый и добавляем его
            propertySources.addFirst(new MapPropertySource("customTrafficCaptureConfig",
                    Collections.singletonMap(key, value)));
        }
    }
}
