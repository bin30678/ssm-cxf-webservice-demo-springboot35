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
