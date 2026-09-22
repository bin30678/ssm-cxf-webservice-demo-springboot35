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

        private JndiTomcatServletWebServerFactory(EmbeddedJndiProperties properties) {
            this.properties = properties;
            this.applicationClassLoader = Thread.currentThread().getContextClassLoader();
        }

        @Override
        protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
            tomcat.enableNaming();
            return super.getTomcatWebServer(tomcat);
        }

        @Override
        protected void postProcessContext(Context context) {
            super.postProcessContext(context);
            registerNamingContextCleanup(context);
            for (Map.Entry<String, EmbeddedJndiProperties.DataSourceResource> entry
                    : properties.getDatasources().entrySet()) {
                addDataSourceResource(context, entry.getKey(), entry.getValue());
            }
        }

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

        private void bindApplicationClassLoader(StandardContext context) {
            try {
                ContextBindings.bindClassLoader(context, context.getNamingToken(), applicationClassLoader);
            }
            catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to bind embedded Tomcat JNDI to the application class loader", ex);
            }
        }

        private void bindApplicationClassLoaders(TomcatWebServer tomcatWebServer) {
            for (var child : tomcatWebServer.getTomcat().getHost().findChildren()) {
                if (child instanceof StandardContext standardContext) {
                    bindApplicationClassLoader(standardContext);
                }
            }
        }

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

        private void requireText(String value, String property) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("sharedservices.jndi.datasources." + property + " is required");
            }
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
