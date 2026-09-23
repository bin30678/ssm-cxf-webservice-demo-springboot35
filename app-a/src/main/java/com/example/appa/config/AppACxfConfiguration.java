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
