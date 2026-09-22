package com.example.appa.config;

import com.example.cxfdemo.provider.GsonProvider;
import com.example.cxfdemo.rest.HealthResource;
import java.util.List;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppACxfConfiguration {

    @Bean
    ServletRegistrationBean<CXFServlet> cxfServletRegistration() {
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>(
                new CXFServlet(), "/rest/*", "/Webservice/*");
        registration.setName("CXFServlet");
        registration.setLoadOnStartup(2);
        return registration;
    }

    @Bean
    HealthResource healthResource() {
        return new HealthResource();
    }

    @Bean
    GsonProvider<?> gsonProvider() {
        return new GsonProvider<>();
    }

    @Bean(destroyMethod = "destroy")
    Server appARestServer(Bus bus, HealthResource healthResource, GsonProvider<?> gsonProvider) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(bus);
        factory.setAddress("/");
        factory.setServiceBeans(List.of(healthResource));
        factory.setProviders(List.of(gsonProvider));
        return factory.create();
    }
}
