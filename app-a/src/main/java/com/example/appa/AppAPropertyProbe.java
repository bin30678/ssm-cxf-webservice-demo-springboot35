package com.example.appa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppAPropertyProbe {

    private static final Logger log = LoggerFactory.getLogger(AppAPropertyProbe.class);

    private final String directDatabaseValue;
    private final String valueResolvedInsideApplicationProperties;
    private final String configSource;

    public AppAPropertyProbe(
            @Value("${system.app.message}") String directDatabaseValue,
            @Value("${app-a.application-message}") String valueResolvedInsideApplicationProperties,
            @Value("${app-a.config-source}") String configSource) {
        this.directDatabaseValue = directDatabaseValue;
        this.valueResolvedInsideApplicationProperties = valueResolvedInsideApplicationProperties;
        this.configSource = configSource;
        log.info("DB property proof - @Value: [{}], application placeholder: [{}], source: [{}]",
                directDatabaseValue, valueResolvedInsideApplicationProperties, configSource);
    }

    public String getDirectDatabaseValue() {
        return directDatabaseValue;
    }

    public String getValueResolvedInsideApplicationProperties() {
        return valueResolvedInsideApplicationProperties;
    }

    public String getConfigSource() {
        return configSource;
    }
}
