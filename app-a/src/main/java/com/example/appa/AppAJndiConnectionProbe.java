package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAJndiConnectionProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAJndiConnectionProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> results = verifyAllConnections();
        log.info("Embedded Tomcat JNDI proof: {}", results);
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo1", verify("jdbc/cxfdemo1", GenericDao::getConnection1));
        results.put("jdbc/cxfdemo2", verify("jdbc/cxfdemo2", GenericDao::getConnection2));
        results.put("jdbc/as400_a", verify("jdbc/as400_a", GenericDao::getConnection3));
        results.put("jdbc/as400_b", verify("jdbc/as400_b", GenericDao::getConnection4));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify " + expectedName, ex);
        }
    }
}
