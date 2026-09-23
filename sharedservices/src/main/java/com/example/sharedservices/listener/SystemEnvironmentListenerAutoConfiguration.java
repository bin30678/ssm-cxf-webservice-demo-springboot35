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
