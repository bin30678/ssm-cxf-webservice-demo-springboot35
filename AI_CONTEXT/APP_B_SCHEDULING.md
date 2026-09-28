# app-b scheduling source

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: app-b/src/main/java/com/example/appb/AppBApplication.java

````java
package com.example.appb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppBApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppBApplication.class, args);
    }
}
````


## File: app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java

````java
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
````


## File: app-b/src/main/java/com/example/cxfdemo/scheduler/QuartzDemoJob.java

````java
package com.example.cxfdemo.scheduler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;

public class QuartzDemoJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("=== [Quartz Job] Triggered at: " + new Date() + " ===");
        // 實作商業邏輯
    }
}
````


## File: app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java

````java
package com.example.cxfdemo.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScheduledTasks {

    /**
     * 每小時整點觸發
     */
    @Scheduled(cron = "0 0 * * * *")
    public void executeEveryHour() {
        System.out.println("=== [@Scheduled] executeEveryHour Triggered at: " + new Date());
    }

    /**
     * 每天凌晨零時觸發
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void executeAtMidnight() {
        System.out.println("=== [@Scheduled] executeAtMidnight Triggered at: " + new Date());
    }

    /**
     * 每天凌晨一點觸發：遞迴檢查備份目錄，刪除修改時間超過 N 天的檔案
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void executeAtOneAm() {
        System.out.println("=== [@Scheduled] executeAtOneAm Triggered at: " + new Date());
        cleanOldBackupFiles("C:/backup_folder", 7);
    }

    /**
     * 手動或定時清理過期備份檔案
     * @param backupDirPath 目標目錄（預設 C:/backup_folder）
     * @param daysToKeep 保存天數（預設 7 天）
     * @return 執行結果統計資訊
     */
    public Map<String, Object> cleanOldBackupFiles(String backupDirPath, int daysToKeep) {
        if (backupDirPath == null || backupDirPath.trim().isEmpty()) {
            backupDirPath = "C:/backup_folder";
        }
        if (daysToKeep <= 0) {
            daysToKeep = 7;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        long maxAgeMillis = daysToKeep * 24L * 60L * 60L * 1000L;
        long currentTime = System.currentTimeMillis();

        File backupDir = new File(backupDirPath);
        List<String> deletedFiles = new ArrayList<String>();
        List<String> deletedDirs = new ArrayList<String>();

        if (!backupDir.exists()) {
            result.put("success", false);
            result.put("message", "目標備份目錄不存在: " + backupDirPath);
            result.put("targetDirectory", backupDirPath);
            result.put("daysToKeep", daysToKeep);
            result.put("deletedFilesCount", 0);
            return result;
        }
        if (!backupDir.isDirectory()) {
            result.put("success", false);
            result.put("message", "目標路徑不是資料夾: " + backupDirPath);
            result.put("targetDirectory", backupDirPath);
            result.put("daysToKeep", daysToKeep);
            result.put("deletedFilesCount", 0);
            return result;
        }

        cleanUpOldFiles(backupDir, currentTime, maxAgeMillis, deletedFiles, deletedDirs);

        result.put("success", true);
        result.put("message", "過期檔案清理完成");
        result.put("targetDirectory", backupDir.getAbsolutePath());
        result.put("daysToKeep", daysToKeep);
        result.put("deletedFilesCount", deletedFiles.size());
        result.put("deletedFiles", deletedFiles);
        result.put("deletedDirectoriesCount", deletedDirs.size());
        result.put("deletedDirectories", deletedDirs);
        return result;
    }

    // 提供原本反射單元測試相容的簽名
    private void cleanUpOldFiles(File dir, long currentTime, long maxAgeMillis) {
        cleanUpOldFiles(dir, currentTime, maxAgeMillis, null, null);
    }

    private void cleanUpOldFiles(File dir, long currentTime, long maxAgeMillis, List<String> deletedFiles, List<String> deletedDirs) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 遞迴檢查子目錄
                    cleanUpOldFiles(file, currentTime, maxAgeMillis, deletedFiles, deletedDirs);
                    // 如果目錄空了可以考慮刪除
                    if (file.list() != null && file.list().length == 0) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Deleted empty directory: " + file.getAbsolutePath());
                            if (deletedDirs != null) deletedDirs.add(file.getAbsolutePath());
                        }
                    }
                } else {
                    // 判斷檔案最後修改時間
                    long lastModified = file.lastModified();
                    if (currentTime - lastModified > maxAgeMillis) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Deleted old backup file: " + file.getAbsolutePath());
                            if (deletedFiles != null) deletedFiles.add(file.getAbsolutePath());
                        } else {
                            System.out.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * 每天中午 12 點觸發
     */
    @Scheduled(cron = "0 0 12 * * *")
    public void executeAtNoon() {
        System.out.println("=== [@Scheduled] executeAtNoon Triggered at: " + new Date());
    }
}
````

