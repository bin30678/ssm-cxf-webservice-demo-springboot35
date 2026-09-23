# 共用工具類別、監聽器與啟動探針 (Utilities, Listeners & Probes)

本文件收錄專案通用工具類別 (WebUtils)、Servlet 容器啟動監聽器 (FontCheckListener, TiffImageReaderCheckListener)、啟動自我檢測探針 (AppA Probes) 與模組標記類別。

> 本文件收錄 7 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java

```java
package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAExternalLibAJndiProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAExternalLibAJndiProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("External JAR A GenericDao source: {}",
                com.external.liba.utils.GenericDao.class
                        .getProtectionDomain().getCodeSource().getLocation());
        log.info("External JAR A GenericDao JNDI proof: {}", verifyAllConnections());
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo2", verify(
                "jdbc/cxfdemo2", com.external.liba.utils.GenericDao::getConnection1));
        results.put("jdbc/as400_c", verify(
                "jdbc/as400_c", com.external.liba.utils.GenericDao::getConnection2));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("External JAR JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("External JAR probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to verify external JAR connection " + expectedName, ex);
        }
    }
}

```

---

## File: app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java

```java
package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAJndiConnectionProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAJndiConnectionProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> results = verifyAllConnections();
        log.info("Embedded Tomcat JNDI proof: {}", results);
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo1", verify("jdbc/cxfdemo1", GenericDao::getConnection1));
        results.put("jdbc/cxfdemo2", verify("jdbc/cxfdemo2", GenericDao::getConnection2));
        results.put("jdbc/as400_a", verify("jdbc/as400_a", GenericDao::getConnection3));
        results.put("jdbc/as400_b", verify("jdbc/as400_b", GenericDao::getConnection4));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify " + expectedName, ex);
        }
    }
}

```

---

## File: app-a/src/main/java/com/example/appa/AppAPropertyProbe.java

```java
package com.example.appa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppAPropertyProbe {

    private static final Logger log = LoggerFactory.getLogger(AppAPropertyProbe.class);

    private final String directDatabaseValue;
    private final String valueResolvedInsideApplicationProperties;
    private final String configSource;

    public AppAPropertyProbe(
            @Value("${system.app.message}") String directDatabaseValue,
            @Value("${app-a.application-message}") String valueResolvedInsideApplicationProperties,
            @Value("${app-a.config-source}") String configSource) {
        this.directDatabaseValue = directDatabaseValue;
        this.valueResolvedInsideApplicationProperties = valueResolvedInsideApplicationProperties;
        this.configSource = configSource;
        log.info("DB property proof - @Value: [{}], application placeholder: [{}], source: [{}]",
                directDatabaseValue, valueResolvedInsideApplicationProperties, configSource);
    }

    public String getDirectDatabaseValue() {
        return directDatabaseValue;
    }

    public String getValueResolvedInsideApplicationProperties() {
        return valueResolvedInsideApplicationProperties;
    }

    public String getConfigSource() {
        return configSource;
    }
}

```

---

## File: common-core/src/main/java/com/example/commoncore/CommonCoreMarker.java

```java
package com.example.commoncore;

/** Marks the shared domain/core module. */
public final class CommonCoreMarker {

    private CommonCoreMarker() {
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/listener/FontCheckListener.java

```java
package com.example.cxfdemo.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class FontCheckListener implements ServletContextListener {

    private String targetFont;
    private boolean fontExists = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce != null ? sce.getServletContext() : null;
        if (context != null) {
            targetFont = context.getInitParameter("targetFont");
        }
        if (targetFont == null || targetFont.trim().isEmpty()) {
            targetFont = "Arial"; // 預設檢查 Arial
        }

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontExists = Arrays.asList(fonts).contains(targetFont);

        if (fontExists) {
            System.out.println("=== [Listener Init] Font '" + targetFont + "' is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Listener Init] WARNING: Font '" + targetFont + "' is MISSING in the system.");
        }

        if (context != null) {
            context.setAttribute("fontExists", fontExists);
            context.setAttribute("targetFont", targetFont);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public String getTargetFont() {
        return targetFont;
    }

    public boolean isFontExists() {
        return fontExists;
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/listener/TiffImageReaderCheckListener.java

```java
package com.example.cxfdemo.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Iterator;
import javax.imageio.ImageIO;

public class TiffImageReaderCheckListener implements ServletContextListener {

    private boolean hasTiffReader = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 系統啟動時檢查是否安裝有 TIFF Reader
        Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersByFormatName("tiff");
        if (readers != null && readers.hasNext()) {
            hasTiffReader = true;
            System.out.println("=== [Listener Init] TIFF ImageReader is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Listener Init] WARNING: TIFF ImageReader is MISSING in the system.");
        }

        if (sce != null && sce.getServletContext() != null) {
            ServletContext context = sce.getServletContext();
            context.setAttribute("hasTiffReader", hasTiffReader);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public boolean isHasTiffReader() {
        return hasTiffReader;
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java

```java
package com.example.cxfdemo.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WebUtils {

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多級代理，取第一個IP為真實IP
        if (ip != null && ip.indexOf(',') != -1) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip;
    }

    public static String getClientType(HttpServletRequest request) {
        if (request == null) return "unknown";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "unknown";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("postman")) {
            return "Postman";
        } else if (userAgent.contains("apache-httpclient") || userAgent.contains("java") || userAgent.contains("curl")) {
            return "HttpClient";
        } else if (userAgent.contains("okhttp") || userAgent.contains("android") || userAgent.contains("iphone") || userAgent.contains("app")) {
            return "App";
        } else if (userAgent.contains("mozilla") || userAgent.contains("chrome") || userAgent.contains("safari")) {
            return "Browser";
        }
        return "Other";
    }

    public static String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}

```

---
