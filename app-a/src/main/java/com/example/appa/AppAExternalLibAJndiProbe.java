package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAExternalLibAJndiProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAExternalLibAJndiProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("External JAR A GenericDao source: {}",
                com.external.liba.utils.GenericDao.class
                        .getProtectionDomain().getCodeSource().getLocation());
        log.info("External JAR A GenericDao JNDI proof: {}", verifyAllConnections());
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo2", verify(
                "jdbc/cxfdemo2", com.external.liba.utils.GenericDao::getConnection1));
        results.put("jdbc/as400_c", verify(
                "jdbc/as400_c", com.external.liba.utils.GenericDao::getConnection2));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("External JAR JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("External JAR probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to verify external JAR connection " + expectedName, ex);
        }
    }
}
