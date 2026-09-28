package com.example.sharedservices.cxf;

import com.example.cxfdemo.dao.AuditLogDao;
import com.example.cxfdemo.interceptor.AuditFaultInterceptor;
import com.example.cxfdemo.interceptor.AuditRequestInterceptor;
import com.example.cxfdemo.interceptor.AuditResponseInterceptor;
import com.example.cxfdemo.interceptor.UnifiedFaultInterceptor;
import org.apache.cxf.Bus;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration")
@ConditionalOnClass(Bus.class)
@ConditionalOnBean(Bus.class)
@ConditionalOnProperty(prefix = "sharedservices.cxf-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CxfAuditInterceptorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditRequestInterceptor auditRequestInterceptor(AuditLogDao auditLogDao) {
        AuditRequestInterceptor interceptor = new AuditRequestInterceptor();
        interceptor.setAuditLogDao(auditLogDao);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    AuditResponseInterceptor auditResponseInterceptor(AuditLogDao auditLogDao) {
        AuditResponseInterceptor interceptor = new AuditResponseInterceptor();
        interceptor.setAuditLogDao(auditLogDao);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    AuditFaultInterceptor auditFaultInterceptor(AuditLogDao auditLogDao) {
        AuditFaultInterceptor interceptor = new AuditFaultInterceptor();
        interceptor.setAuditLogDao(auditLogDao);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    UnifiedFaultInterceptor unifiedFaultInterceptor() {
        return new UnifiedFaultInterceptor();
    }

    @Bean
    CxfAuditInterceptorRegistration cxfAuditInterceptorRegistration(
            Bus bus,
            AuditRequestInterceptor requestInterceptor,
            AuditResponseInterceptor responseInterceptor,
            AuditFaultInterceptor faultInterceptor,
            UnifiedFaultInterceptor unifiedFaultInterceptor) {
        return new CxfAuditInterceptorRegistration(bus, requestInterceptor, responseInterceptor,
                faultInterceptor, unifiedFaultInterceptor);
    }

    static final class CxfAuditInterceptorRegistration implements AutoCloseable {

        private final Bus bus;
        private final AuditRequestInterceptor requestInterceptor;
        private final AuditResponseInterceptor responseInterceptor;
        private final AuditFaultInterceptor faultInterceptor;
        private final UnifiedFaultInterceptor unifiedFaultInterceptor;

        CxfAuditInterceptorRegistration(
                Bus bus,
                AuditRequestInterceptor requestInterceptor,
                AuditResponseInterceptor responseInterceptor,
                AuditFaultInterceptor faultInterceptor,
                UnifiedFaultInterceptor unifiedFaultInterceptor) {
            this.bus = bus;
            this.requestInterceptor = requestInterceptor;
            this.responseInterceptor = responseInterceptor;
            this.faultInterceptor = faultInterceptor;
            this.unifiedFaultInterceptor = unifiedFaultInterceptor;
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
