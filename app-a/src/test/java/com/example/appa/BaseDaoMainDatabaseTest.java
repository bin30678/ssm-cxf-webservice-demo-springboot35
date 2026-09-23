package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.dao.BaseDao;
import com.example.sharedservices.database.MainDatabaseAutoConfiguration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BaseDaoMainDatabaseTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MainDatabaseAutoConfiguration.class))
            .withUserConfiguration(Databases.class)
            .withBean(ConsumerDao.class);

    @Test
    void independentConsumersUseMainDatabaseEvenWithSecondaryPrimaryBeans() {
        // Two contexts represent two consumers of the shared JAR, not a shared Spring container.
        runner.run(first -> {
            assertThat(first).hasNotFailed();
            verifyMainDatabase(first.getBean(ConsumerDao.class));
            runner.run(second -> {
                assertThat(second).hasNotFailed();
                ConsumerDao secondDao = second.getBean(ConsumerDao.class);
                verifyMainDatabase(secondDao);
                assertThat(secondDao.getDataSource())
                        .isNotSameAs(first.getBean(ConsumerDao.class).getDataSource());
            });
        });
    }

    @Test
    void missingMainDatabaseFailsInsteadOfFallingBackToSecondary() {
        new ApplicationContextRunner()
                .withUserConfiguration(SecondaryDatabase.class)
                .withBean(ConsumerDao.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("sqlSessionTemplate1");
                });
    }

    private void verifyMainDatabase(ConsumerDao dao) throws Exception {
        // Reads exercise all three inherited access paths against distinguishable databases.
        assertThat(dao.getJdbcTemplate().queryForObject("SELECT label FROM db_identity", String.class))
                .isEqualTo("MAIN");
        var configuration = dao.getSqlSessionTemplate().getConfiguration();
        if (!configuration.hasMapper(IdentityMapper.class)) {
            configuration.addMapper(IdentityMapper.class);
        }
        assertThat(dao.getSqlSessionTemplate().getMapper(IdentityMapper.class).read()).isEqualTo("MAIN");
        try (Connection connection = dao.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT label FROM db_identity")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("MAIN");
        }
    }

    public interface IdentityMapper {
        @Select("SELECT label FROM db_identity")
        String read();
    }

    static class ConsumerDao extends BaseDao { }

    @TestConfiguration(proxyBeanMethods = false)
    static class Databases extends SecondaryDatabase {
        @Bean("dataSource1")
        DataSource mainDataSource() {
            return database("main", "MAIN");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecondaryDatabase {
        @Bean("dataSource2")
        @Primary
        DataSource secondaryDataSource() {
            return database("secondary", "SECONDARY");
        }

        @Bean("jdbcTemplate2")
        @Primary
        JdbcTemplate secondaryJdbcTemplate(@Qualifier("dataSource2") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean("sqlSessionTemplate2")
        @Primary
        SqlSessionTemplate secondarySqlSessionTemplate(@Qualifier("dataSource2") DataSource dataSource)
                throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            SqlSessionFactory sessionFactory = factory.getObject();
            return new SqlSessionTemplate(sessionFactory);
        }
    }

    private static DataSource database(String name, String label) {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:base_dao_" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS db_identity (id INT PRIMARY KEY, label VARCHAR(20))");
        jdbc.update("MERGE INTO db_identity (id, label) KEY(id) VALUES (1, ?)", label);
        return dataSource;
    }
}
