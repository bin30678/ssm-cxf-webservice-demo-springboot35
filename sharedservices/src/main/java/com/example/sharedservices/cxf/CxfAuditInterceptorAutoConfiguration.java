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
