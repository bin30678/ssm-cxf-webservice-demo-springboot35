package com.example.appb;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class AppBSchedulingIntegrationTest {

    @Test
    void appBRegistersLegacyScheduledAndQuartzJobs() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AppBApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.main.banner-mode=off", "--spring.quartz.auto-startup=false")) {
            assertThat(context.getBean(ScheduledTasks.class)).isNotNull();

            ScheduledTaskHolder scheduledTaskHolder = context.getBean(ScheduledTaskHolder.class);
            assertThat(scheduledTaskHolder.getScheduledTasks()).hasSize(4);

            Set<String> cronExpressions = Arrays.stream(ScheduledTasks.class.getDeclaredMethods())
                    .map(method -> method.getAnnotation(Scheduled.class))
                    .filter(annotation -> annotation != null)
                    .map(Scheduled::cron)
                    .collect(Collectors.toSet());
            assertThat(cronExpressions).containsExactlyInAnyOrder(
                    "0 0 * * * *", "0 0 0 * * *", "0 0 1 * * *", "0 0 12 * * *");

            JobDetail jobDetail = context.getBean("quartzDemoJobDetail", JobDetail.class);
            Trigger trigger = context.getBean("quartzDemoTrigger", Trigger.class);
            assertThat(jobDetail.getJobClass().getName())
                    .isEqualTo("com.example.cxfdemo.scheduler.QuartzDemoJob");
            assertThat(jobDetail.isDurable()).isTrue();
            assertThat(trigger.getJobKey()).isEqualTo(jobDetail.getKey());
            assertThat(trigger).isInstanceOf(SimpleTrigger.class);
            assertThat(((SimpleTrigger) trigger).getRepeatInterval()).isEqualTo(300000L);
        }
    }
}
