package com.example.appb;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduledTasksTest {

    @TempDir
    Path testDirectory;

    @Test
    void cleanOldBackupFilesDeletesExpiredFilesRecursively() throws Exception {
        Path newFile = Files.createFile(testDirectory.resolve("new_backup.txt"));
        Path oldFile = Files.createFile(testDirectory.resolve("old_backup.txt"));
        Path subDirectory = Files.createDirectory(testDirectory.resolve("sub_folder"));
        Path oldFileInSubDirectory = Files.createFile(subDirectory.resolve("old_sub_backup.txt"));
        FileTime tenDaysAgo = FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Files.setLastModifiedTime(oldFile, tenDaysAgo);
        Files.setLastModifiedTime(oldFileInSubDirectory, tenDaysAgo);

        Map<String, Object> result = new ScheduledTasks()
                .cleanOldBackupFiles(testDirectory.toString(), 7);

        assertThat(result).containsEntry("success", true)
                .containsEntry("deletedFilesCount", 2)
                .containsEntry("deletedDirectoriesCount", 1);
        assertThat(newFile).exists();
        assertThat(oldFile).doesNotExist();
        assertThat(oldFileInSubDirectory).doesNotExist();
        assertThat(subDirectory).doesNotExist();
    }
}
