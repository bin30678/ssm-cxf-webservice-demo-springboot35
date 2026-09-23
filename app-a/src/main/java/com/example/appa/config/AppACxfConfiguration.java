package com.example.appa.config;

import com.example.cxfdemo.provider.GsonProvider;
import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.example.cxfdemo.rest.HealthResource;
import com.example.cxfdemo.rest.MainNouternalResource;
import com.external.liba.dao.ExternalJarGenericDao;
import java.util.List;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(PolicyDaoImpl.class)
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

    @Bean
    ExternalJarGenericDao externalJarGenericDao() {
        return new ExternalJarGenericDao();
    }

    @Bean
    MainNouternalResource mainNouternalResource(
            PolicyDaoImpl policyDao, ExternalJarGenericDao externalJarGenericDao) {
        return new MainNouternalResource(policyDao, externalJarGenericDao);
    }

    @Bean(destroyMethod = "destroy")
    Server appARestServer(Bus bus, HealthResource healthResource,
                          MainNouternalResource mainNouternalResource,
                          GsonProvider<?> gsonProvider) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(bus);
        factory.setAddress("/");
        factory.setServiceBeans(List.of(healthResource, mainNouternalResource));
        factory.setProviders(List.of(gsonProvider));
        return factory.create();
    }
}
