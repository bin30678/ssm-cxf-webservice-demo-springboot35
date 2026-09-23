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
