package com.example.sharedservices.database;

import com.example.cxfdemo.dao.AuditLogDao;
import com.example.cxfdemo.dao.ExternalApiLogDao;
import com.example.cxfdemo.service.ApiServiceImpl;
import com.example.cxfdemo.service.MailServiceImpl;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Java configuration equivalent of the main-database section in the legacy
 * applicationContext.xml. The DAO and mapper XML remain unchanged.
 */
@AutoConfiguration
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionTemplate.class, JdbcTemplate.class})
@ConditionalOnProperty(prefix = "sharedservices.main-database", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import({ExternalApiLogDao.class, ApiServiceImpl.class, MailServiceImpl.class})
public class MainDatabaseAutoConfiguration {

    @Bean(name = "dataSource1")
    @Primary
    @ConditionalOnMissingBean(name = "dataSource1")
    DataSource dataSource1() throws Exception {
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiName("jdbc/cxfdemo1");
        factory.setResourceRef(true);
        factory.setProxyInterface(DataSource.class);
        factory.setLookupOnStartup(false);
        factory.afterPropertiesSet();
        return (DataSource) factory.getObject();
    }

    @Bean(name = "sqlSessionFactory1")
    @ConditionalOnMissingBean(name = "sqlSessionFactory1")
    SqlSessionFactory sqlSessionFactory1(@Qualifier("dataSource1") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));
        return factory.getObject();
    }

    @Bean(name = "sqlSessionTemplate1")
    @ConditionalOnMissingBean(name = "sqlSessionTemplate1")
    SqlSessionTemplate sqlSessionTemplate1(
            @Qualifier("sqlSessionFactory1") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "jdbcTemplate1")
    @ConditionalOnMissingBean(name = "jdbcTemplate1")
    JdbcTemplate jdbcTemplate1(@Qualifier("dataSource1") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "transactionManager1")
    @ConditionalOnMissingBean(name = "transactionManager1")
    PlatformTransactionManager transactionManager1(@Qualifier("dataSource1") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(AuditLogDao.class)
    AuditLogDao auditLogDao() {
        return new AuditLogDao();
    }

}
