package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.utils.GenericDao;
import com.example.cxfdemo.dao.ExternalApiLogDao;
import com.example.cxfdemo.service.ApiService;
import com.example.cxfdemo.service.MailService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.net.URL;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class DatabaseSystemPropertiesStartupTest {

    @Test
    void databaseValuesResolveApplicationPlaceholdersAndValueInjectionAtStartup() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AppAApplication.class)
                .web(WebApplicationType.SERVLET)
                .run("--spring.main.banner-mode=off")) {
            AppAPropertyProbe probe = context.getBean(AppAPropertyProbe.class);

            assertThat(context.getBean(ApiService.class)).isNotNull();
            assertThat(context.getBean(MailService.class)).isNotNull();
            assertThat(context.getBean(ExternalApiLogDao.class)).isNotNull();

            assertThat(context.getEnvironment().getProperty("system.app.message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(context.getEnvironment().getProperty("app-a.application-message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getDirectDatabaseValue()).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getValueResolvedInsideApplicationProperties()).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getConfigSource()).isEqualTo("system_properties");
            assertThat(context.getBean(AppAJndiConnectionProbe.class).verifyAllConnections())
                    .containsOnlyKeys("jdbc/cxfdemo1", "jdbc/cxfdemo2", "jdbc/as400_a", "jdbc/as400_b")
                    .allSatisfy((name, url) -> assertThat(url).startsWith("jdbc:h2:mem:"));
            assertThat(context.getBean(AppAExternalLibAJndiProbe.class).verifyAllConnections())
                    .containsEntry("jdbc/cxfdemo2", "jdbc:h2:mem:cxfdemo2")
                    .containsEntry("jdbc/as400_c", "jdbc:h2:mem:as400_c");

            URL externalJarLocation = com.external.liba.utils.GenericDao.class
                    .getProtectionDomain().getCodeSource().getLocation();
            assertThat(externalJarLocation.getPath()).endsWith("external-lib-a-1.0.0.jar");
            assertThat(GenericDao.getSystemProperty("system.app.message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");

            DataSource bootDataSource = context.getBean(DataSource.class);
            try (Connection connection = bootDataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT prop_value FROM system_properties WHERE prop_key = 'system.app.message'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            }
        }
    }
}
