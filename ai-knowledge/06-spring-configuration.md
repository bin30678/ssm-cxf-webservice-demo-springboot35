# Spring Boot 配置、自動配置與啟動類別 (Spring Boot & AutoConfiguration)

本文件收錄 Spring Boot 應用程式主啟動類別 (@SpringBootApplication)、Spring @Configuration 配置類別、自訂 AutoConfiguration 自動裝配類別、EnvironmentPostProcessor 以及 META-INF SPI 設定檔。

> 本文件收錄 15 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/main/java/com/example/appa/AppAApplication.java

```java
package com.example.appa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppAApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppAApplication.class, args);
    }
}

```

---

## File: app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java

```java
package com.example.appa.config;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.external.liba.dao.ExternalJarGenericDao;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({PolicyDaoImpl.class, ExternalJarGenericDao.class})
// Register legacy JAX-RS annotations as Spring beans, including constructor injection.
@ComponentScan(basePackages = {"com.example.cxfdemo.rest", "com.example.cxfdemo.provider"},
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                classes = {Path.class, Provider.class}))
public class AppACxfConfiguration {

    @Bean
    ServletRegistrationBean<CXFServlet> cxfServletRegistration() {
        // One servlet per application, preserving both legacy URL prefixes.
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>(
                new CXFServlet(), "/rest/*", "/Webservice/*");
        registration.setName("CXFServlet");
        registration.setLoadOnStartup(2);
        return registration;
    }
}

```

---

## File: app-b/src/main/java/com/example/appb/AppBApplication.java

```java
package com.example.appb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppBApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppBApplication.class, args);
    }
}

```

---

## File: app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java

```java
package com.example.appb.config;

import com.example.cxfdemo.scheduler.QuartzDemoJob;
import com.example.cxfdemo.scheduler.ScheduledTasks;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;
import java.util.Date;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@Import(ScheduledTasks.class)
public class AppBSchedulingConfiguration {

    @Bean
    JobDetail quartzDemoJobDetail() {
        return JobBuilder.newJob(QuartzDemoJob.class)
                .withIdentity("quartzDemoJobDetail")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger quartzDemoTrigger(JobDetail quartzDemoJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("quartzDemoTrigger")
                .forJob(quartzDemoJobDetail)
                .startAt(Date.from(Instant.now().plusSeconds(1)))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(300000)
                        .repeatForever())
                .build();
    }
}

```

---

## File: app-c/src/main/java/com/example/appc/AppCApplication.java

```java
package com.example.appc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppCApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppCApplication.class, args);
    }
}

```

---

## File: app-d/src/main/java/com/example/appd/AppDApplication.java

```java
package com.example.appd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppDApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppDApplication.class, args);
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java

```java
package com.example.cxfdemo.config;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系統參數快取單例類別 (ConfigSingleton)
 * 負責從 system_properties 資料庫表載入系統設定資訊。
 */
public class ConfigSingleton {

    private static final Logger log = LoggerFactory.getLogger(ConfigSingleton.class);

    private static volatile ConfigSingleton instance;

    private final Map<String, String> map = new ConcurrentHashMap<String, String>();

    private ConfigSingleton() {
    }

    /**
     * 取得單例實例，若需初始化 (needInit為true) 則自動執行 load()
     */
    public static ConfigSingleton getInstance() {
        if (instance == null) {
            synchronized (ConfigSingleton.class) {
                if (instance == null) {
                    instance = new ConfigSingleton();
                }
            }
        }
        if (instance.needInit()) {
            synchronized (instance) {
                if (instance.needInit()) {
                    instance.load();
                }
            }
        }
        return instance;
    }

    /**
     * 判斷 map 是否為空 (需要初始化)
     */
    public boolean needInit() {
        return map == null || map.isEmpty();
    }

    /**
     * 從 system_properties 讀取所有資料放進 map 裡
     */
    public synchronized void load() {
        log.info("ConfigSingleton.load() starting...");
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = GenericDao.getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties");
                rs = ps.executeQuery();
                while (rs.next()) {
                    String key = rs.getString("prop_key");
                    String val = rs.getString("prop_value");
                    if (key != null) {
                        map.put(key, val != null ? val : "");
                        log.debug("Loaded ConfigSingleton property: {} = {}", key, val);
                    }
                }
                log.info("ConfigSingleton.load() completed, loaded {} properties.", map.size());
            } else {
                log.warn("ConfigSingleton.load() failed to obtain connection from GenericDao.getConnection1()");
            }
        } catch (Exception e) {
            log.error("Error loading properties in ConfigSingleton", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            GenericDao.closeConnection(conn);
        }
    }

    /**
     * 清除 map
     */
    public synchronized void clear() {
        if (map != null) {
            map.clear();
            log.info("ConfigSingleton.clear() executed.");
        }
    }

    /**
     * 調用 clear，再調用 load
     */
    public synchronized void reload() {
        log.info("ConfigSingleton.reload() triggered.");
        clear();
        load();
    }

    /**
     * 取得 map 內部資料
     */
    public Map<String, String> getMap() {
        return map;
    }

    /**
     * 靜態便捷存取方法：傳 key 進去，取得資訊
     */
    public static String getMappingData(String key) {
        if (key == null) {
            return null;
        }
        return getInstance().getMap().get(key);
    }
}

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java

```java
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
    static final String MAIN_JNDI_DATASOURCE_PREFIX =
            "sharedservices.jndi.datasources.cxfdemo1.";
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
                        "Failed to load system_properties from the main DataSource jdbc/cxfdemo1", ex);
            }
        }
    }

    private Map<String, Object> loadProperties(ConfigurableEnvironment environment, String query)
            throws Exception {
        String jndiName = environment.getProperty(PREFIX + "jndi-name");
        if (jndiName != null) {
            return loadProperties(lookupDataSource(jndiName).getConnection(), query);
        }

        // Embedded Tomcat has not created JNDI yet at this early boot phase.
        // Use the exact same cxfdemo1 resource definition that will later be
        // registered as java:comp/env/jdbc/cxfdemo1; do not define a second DataSource.
        String configuredJndiName = requiredProperty(
                environment, MAIN_JNDI_DATASOURCE_PREFIX + "jndi-name");
        if (!"jdbc/cxfdemo1".equals(configuredJndiName)) {
            throw new IllegalStateException(MAIN_JNDI_DATASOURCE_PREFIX
                    + "jndi-name must be jdbc/cxfdemo1 because cxfdemo1 is the main database");
        }
        String url = requiredProperty(environment, MAIN_JNDI_DATASOURCE_PREFIX + "jdbc-url");
        String username = environment.getProperty(MAIN_JNDI_DATASOURCE_PREFIX + "username", "");
        String password = environment.getProperty(MAIN_JNDI_DATASOURCE_PREFIX + "password", "");
        String driverClassName = environment.getProperty(
                MAIN_JNDI_DATASOURCE_PREFIX + "driver-class-name");
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

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java

```java
package com.example.sharedservices.cxf;

import com.example.cxfdemo.dao.AuditLogDao;
import com.example.cxfdemo.interceptor.AuditFaultInterceptor;
import com.example.cxfdemo.interceptor.AuditRequestInterceptor;
import com.example.cxfdemo.interceptor.AuditResponseInterceptor;
import com.example.cxfdemo.interceptor.UnifiedFaultInterceptor;
import org.apache.cxf.Bus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration")
@ConditionalOnClass(Bus.class)
@ConditionalOnProperty(prefix = "sharedservices.cxf-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CxfAuditInterceptorAutoConfiguration {

    @Bean
    @ConditionalOnBean(Bus.class)
    CxfAuditInterceptorRegistration cxfAuditInterceptorRegistration(
            Bus bus, ObjectProvider<AuditLogDao> auditLogDaoProvider) {
        return new CxfAuditInterceptorRegistration(bus, auditLogDaoProvider.getIfAvailable());
    }

    static final class CxfAuditInterceptorRegistration implements AutoCloseable {

        private final Bus bus;
        private final AuditRequestInterceptor requestInterceptor = new AuditRequestInterceptor();
        private final AuditResponseInterceptor responseInterceptor = new AuditResponseInterceptor();
        private final AuditFaultInterceptor faultInterceptor = new AuditFaultInterceptor();
        private final UnifiedFaultInterceptor unifiedFaultInterceptor = new UnifiedFaultInterceptor();

        CxfAuditInterceptorRegistration(Bus bus, AuditLogDao auditLogDao) {
            this.bus = bus;
            if (auditLogDao != null) {
                requestInterceptor.setAuditLogDao(auditLogDao);
                responseInterceptor.setAuditLogDao(auditLogDao);
                faultInterceptor.setAuditLogDao(auditLogDao);
            }
            bus.getInInterceptors().add(requestInterceptor);
            bus.getOutInterceptors().add(responseInterceptor);
            bus.getInFaultInterceptors().add(faultInterceptor);
            bus.getOutFaultInterceptors().add(faultInterceptor);
            bus.getOutFaultInterceptors().add(unifiedFaultInterceptor);
        }

        @Override
        public void close() {
            bus.getInInterceptors().remove(requestInterceptor);
            bus.getOutInterceptors().remove(responseInterceptor);
            bus.getInFaultInterceptors().remove(faultInterceptor);
            bus.getOutFaultInterceptors().remove(faultInterceptor);
            bus.getOutFaultInterceptors().remove(unifiedFaultInterceptor);
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java

```java
package com.example.sharedservices.jndi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sharedservices.jndi")
public class EmbeddedJndiProperties {

    private boolean enabled;
    private Map<String, DataSourceResource> datasources = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, DataSourceResource> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DataSourceResource> datasources) {
        this.datasources = datasources;
    }

    public static class DataSourceResource {

        private String jndiName;
        private String driverClassName;
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 10;
        private int minimumIdle = 0;

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(String jndiName) {
            this.jndiName = jndiName;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java

```java
package com.example.sharedservices.jndi;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.core.StandardContext;
import org.apache.naming.ContextBindings;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationListener;

@AutoConfiguration
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Tomcat.class, TomcatServletWebServerFactory.class})
@ConditionalOnProperty(prefix = "sharedservices.jndi", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EmbeddedJndiProperties.class)
public class EmbeddedTomcatJndiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ServletWebServerFactory.class)
    TomcatServletWebServerFactory jndiTomcatServletWebServerFactory(EmbeddedJndiProperties properties) {
        return new JndiTomcatServletWebServerFactory(properties);
    }

    @Bean
    ApplicationListener<WebServerInitializedEvent> bindJndiAfterWebServerStart(
            ServletWebServerFactory servletWebServerFactory) {
        return event -> {
            if (servletWebServerFactory instanceof JndiTomcatServletWebServerFactory jndiFactory
                    && event.getWebServer() instanceof TomcatWebServer tomcatWebServer) {
                jndiFactory.bindApplicationClassLoaders(tomcatWebServer);
            }
        };
    }

    private static final class JndiTomcatServletWebServerFactory
            extends TomcatServletWebServerFactory {

        private final EmbeddedJndiProperties properties;
        private final ClassLoader applicationClassLoader;

        /**
         * 保存 application.properties 綁定完成的全部 JNDI DataSource 設定，並記住啟動
         * Spring Boot 應用程式的 ClassLoader。後續要把這個 ClassLoader 綁到 Tomcat
         * Naming Context，讓主程式及外部 JAR 都能透過 InitialContext 查到同一批資源。
         */
        private JndiTomcatServletWebServerFactory(EmbeddedJndiProperties properties) {
            this.properties = properties;
            this.applicationClassLoader = Thread.currentThread().getContextClassLoader();
        }

        /**
         * 在 Spring Boot 建立 TomcatWebServer 前啟用 Tomcat Naming。沒有呼叫
         * enableNaming()，java:comp/env 下的 JNDI Context 不會建立，後續註冊的
         * jdbc/cxfdemo1 等 DataSource 也無法被 lookup。
         */
        @Override
        protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
            tomcat.enableNaming();
            return super.getTomcatWebServer(tomcat);
        }

        /**
         * Tomcat 建立 Web Application Context 時的擴充點。先保留父類別的標準處理，
         * 再註冊停止時的 ClassLoader 清理動作，最後把 properties.datasources 中的
         * 每一筆設定轉成 Tomcat ContextResource。
         */
        @Override
        protected void postProcessContext(Context context) {
            super.postProcessContext(context);
            registerNamingContextCleanup(context);
            for (Map.Entry<String, EmbeddedJndiProperties.DataSourceResource> entry
                    : properties.getDatasources().entrySet()) {
                addDataSourceResource(context, entry.getKey(), entry.getValue());
            }
        }

        /**
         * 在 Tomcat Context 停止前解除 application ClassLoader 與 Naming Context 的
         * 綁定，避免應用程式停止或重新啟動後留下舊的 JNDI Context／ClassLoader 參照。
         */
        private void registerNamingContextCleanup(Context context) {
            if (!(context instanceof StandardContext standardContext)) {
                throw new IllegalStateException(
                        "Embedded Tomcat JNDI requires a StandardContext, but found "
                                + context.getClass().getName());
            }

            context.addLifecycleListener(event -> {
                if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
                    ContextBindings.unbindClassLoader(
                            standardContext,
                            standardContext.getNamingToken(),
                            applicationClassLoader);
                }
            });
        }

        /**
         * 將啟動 Spring Boot 應用程式的 ClassLoader 綁定到指定的 Tomcat
         * StandardContext。綁定後，由該 ClassLoader 載入的主程式或外部 JAR 執行
         * new InitialContext().lookup("java:comp/env/...") 時，才能找到此 Web 應用的資源。
         */
        private void bindApplicationClassLoader(StandardContext context) {
            try {
                ContextBindings.bindClassLoader(context, context.getNamingToken(), applicationClassLoader);
            }
            catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to bind embedded Tomcat JNDI to the application class loader", ex);
            }
        }

        /**
         * 取得目前 Tomcat Host 下的所有 Web Context，逐一呼叫
         * bindApplicationClassLoader。這個方法在 WebServerInitializedEvent 後執行，
         * 此時 Tomcat 已完成 Naming Context 的建立。
         */
        private void bindApplicationClassLoaders(TomcatWebServer tomcatWebServer) {
            for (var child : tomcatWebServer.getTomcat().getHost().findChildren()) {
                if (child instanceof StandardContext standardContext) {
                    bindApplicationClassLoader(standardContext);
                }
            }
        }

        /**
         * 將一筆 sharedservices.jndi.datasources.<id> 設定轉成 Tomcat JNDI
         * ContextResource。id（例如 cxfdemo1）只用來識別設定及產生錯誤訊息；真正
         * 註冊的 JNDI 名稱取自 settings.jndiName，實際 JDBC 位置取自
         * settings.jdbcUrl。資源由 HikariJNDIFactory 建立為可共用的單例 DataSource。
         */
        private void addDataSourceResource(Context context, String id,
                                           EmbeddedJndiProperties.DataSourceResource settings) {
            requireText(settings.getJndiName(), id + ".jndi-name");
            requireText(settings.getJdbcUrl(), id + ".jdbc-url");

            ContextResource resource = new ContextResource();
            resource.setName(settings.getJndiName());
            resource.setAuth("Container");
            resource.setType(DataSource.class.getName());
            resource.setScope("Shareable");
            resource.setSingleton(true);
            resource.setProperty("factory", "com.zaxxer.hikari.HikariJNDIFactory");
            resource.setProperty("jdbcUrl", settings.getJdbcUrl());
            resource.setProperty("username", nullToEmpty(settings.getUsername()));
            resource.setProperty("password", nullToEmpty(settings.getPassword()));
            resource.setProperty("maximumPoolSize", Integer.toString(settings.getMaximumPoolSize()));
            resource.setProperty("minimumIdle", Integer.toString(settings.getMinimumIdle()));
            if (settings.getDriverClassName() != null && !settings.getDriverClassName().isBlank()) {
                resource.setProperty("driverClassName", settings.getDriverClassName());
            }
            context.getNamingResources().addResource(resource);
        }

        /**
         * 驗證建立 JNDI DataSource 必填的文字設定。property 已包含 Map key 與欄位名，
         * 例如 cxfdemo1.jndi-name，讓啟動失敗訊息能直接指出缺少哪一項設定。
         */
        private void requireText(String value, String property) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("sharedservices.jndi.datasources." + property + " is required");
            }
        }

        /**
         * Tomcat ContextResource 的 property 不接受 null；未設定帳號或密碼時轉成空字串。
         */
        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java

```java
package com.example.sharedservices.listener;

import com.example.cxfdemo.listener.FontCheckListener;
import com.example.cxfdemo.listener.TiffImageReaderCheckListener;
import jakarta.servlet.ServletContextListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ServletContextListener.class)
@ConditionalOnProperty(prefix = "sharedservices.environment-check", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class SystemEnvironmentListenerAutoConfiguration {

    @Bean
    ServletListenerRegistrationBean<TiffImageReaderCheckListener> tiffImageReaderCheckListenerRegistration() {
        ServletListenerRegistrationBean<TiffImageReaderCheckListener> registration =
                new ServletListenerRegistrationBean<>(new TiffImageReaderCheckListener());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    ServletListenerRegistrationBean<FontCheckListener> fontCheckListenerRegistration() {
        ServletListenerRegistrationBean<FontCheckListener> registration =
                new ServletListenerRegistrationBean<>(new FontCheckListener());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}

```

---

## File: sharedservices/src/main/resources/META-INF/spring.factories

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
com.example.sharedservices.config.DatabaseSystemPropertiesEnvironmentPostProcessor

```

---

## File: sharedservices/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

```text
com.example.sharedservices.jndi.EmbeddedTomcatJndiAutoConfiguration
com.example.sharedservices.cxf.CxfAuditInterceptorAutoConfiguration
com.example.sharedservices.database.MainDatabaseAutoConfiguration
com.example.sharedservices.listener.SystemEnvironmentListenerAutoConfiguration

```

---
