package com.example.sharedservices.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads application properties from system_properties before the application
 * context is refreshed, so regular placeholders and @Value see the DB values.
 */
public final class DatabaseSystemPropertiesEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "databaseSystemProperties";
    static final String PREFIX = "sharedservices.system-properties.";
    static final String DEFAULT_QUERY = "SELECT prop_key, prop_value FROM system_properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(PREFIX + "enabled", Boolean.class, false)) {
            return;
        }

        String query = environment.getProperty(PREFIX + "query", DEFAULT_QUERY);
        boolean failFast = environment.getProperty(PREFIX + "fail-fast", Boolean.class, true);

        try {
            Map<String, Object> properties = loadProperties(environment, query);
            environment.getPropertySources().addFirst(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        } catch (Exception ex) {
            if (failFast) {
                throw new IllegalStateException(
                        "Failed to load system_properties using the configured Spring Boot DataSource", ex);
            }
        }
    }

    private Map<String, Object> loadProperties(ConfigurableEnvironment environment, String query)
            throws Exception {
        String jndiName = firstText(
                environment.getProperty(PREFIX + "jndi-name"),
                environment.getProperty("spring.datasource.jndi-name"));
        if (jndiName != null) {
            return loadProperties(lookupDataSource(jndiName).getConnection(), query);
        }

        String url = requiredProperty(environment, "spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        if (driverClassName != null && !driverClassName.isBlank()) {
            Class.forName(driverClassName);
        }
        return loadProperties(DriverManager.getConnection(url, username, password), query);
    }

    private String requiredProperty(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured when DB properties are enabled");
        }
        return value;
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private DataSource lookupDataSource(String jndiName) throws NamingException {
        InitialContext context = new InitialContext();
        try {
            Object value;
            try {
                value = context.lookup(jndiName);
            } catch (NamingException firstFailure) {
                if (jndiName.startsWith("java:")) {
                    throw firstFailure;
                }
                value = context.lookup("java:comp/env/" + jndiName);
            }
            if (!(value instanceof DataSource dataSource)) {
                throw new NamingException("JNDI object is not a DataSource: " + jndiName);
            }
            return dataSource;
        } finally {
            context.close();
        }
    }

    private Map<String, Object> loadProperties(Connection connection, String query) throws Exception {
        Map<String, Object> properties = new LinkedHashMap<>();
        try (connection;
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String key = resultSet.getString(1);
                String value = resultSet.getString(2);
                if (key != null && !key.isBlank()) {
                    properties.put(key, value == null ? "" : value);
                }
            }
        }
        return properties;
    }

    @Override
    public int getOrder() {
        // Run after Boot has loaded application.properties, but still before
        // the ApplicationContext is created and @Value is evaluated.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
