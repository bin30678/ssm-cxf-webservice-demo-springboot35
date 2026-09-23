# 資料存取層與持久化類別 (Data Access Objects)

本文件包含基於 MyBatis SqlSessionTemplate 的 DAO 介面、抽象基底類別 BaseDao、GenericDao 通用工具以及具體的資料庫存取實作類別。

> 本文件收錄 6 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java

```java
package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;

public interface PolicyDao {

    PolicyInfo findPolicyViaMapper(String policyNo);

    PolicyInfo findPolicyViaJdbc(String policyNo);

    PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception;

    int insertPolicy(PolicyInfo policy);

    int updatePolicyStatus(String policyNo, String status);
}

```

---

## File: app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java

```java
package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * SSM business DAO copied from the original application.
 */
@Repository("policyDao")
public class PolicyDaoImpl extends BaseDao implements PolicyDao {

    public PolicyDaoImpl() {
    }

    @Override
    public PolicyInfo findPolicyViaMapper(String policyNo) {
        return sqlSessionTemplate.selectOne("com.example.cxfdemo.mapper.DemoMapper.findPolicy", policyNo);
    }

    @Override
    public PolicyInfo findPolicyViaJdbc(String policyNo) {
        String sql = "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?";
        List<PolicyInfo> list = jdbcTemplate.query(sql, new RowMapper<PolicyInfo>() {
            @Override
            public PolicyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PolicyInfo(
                        rs.getString("policy_no"),
                        rs.getString("holder_name"),
                        rs.getString("product_name"),
                        rs.getString("status")
                );
            }
        }, policyNo);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception {
        Connection conn = DataSourceUtils.getConnection(this.dataSource);
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?");
            ps.setString(1, policyNo);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    return new PolicyInfo(
                            rs.getString("policy_no"),
                            rs.getString("holder_name"),
                            rs.getString("product_name"),
                            rs.getString("status")
                    );
                }
            } finally {
                rs.close();
                ps.close();
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, this.dataSource);
        }
        return null;
    }

    @Override
    public int insertPolicy(PolicyInfo policy) {
        String sql = "INSERT INTO policy_info (policy_no, holder_name, product_name, status) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, policy.getPolicyNo(), policy.getHolderName(), policy.getProductName(), policy.getStatus());
    }

    @Override
    public int updatePolicyStatus(String policyNo, String status) {
        String sql = "UPDATE policy_info SET status = ? WHERE policy_no = ?";
        return jdbcTemplate.update(sql, status, policyNo);
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java

```java
package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class AuditLogDao {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDao.class);

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method) {
        insertRequestLog(guid, ip, clientType, hostname, uri, method, null);
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("ip", ip);
        param.put("clientType", clientType);
        param.put("hostname", hostname);
        param.put("uri", uri);
        param.put("method", method);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertRequestLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertRequestLog for guid: " + guid, e);
        }
    }

    public void insertResponseLog(String guid, Integer responseCode) {
        insertResponseLog(guid, responseCode, null);
    }

    public void insertResponseLog(String guid, Integer responseCode, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("responseCode", responseCode);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertResponseLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertResponseLog for guid: " + guid, e);
        }
    }

    public void insertFaultLog(String guid, String errorMsg) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("errorMsg", errorMsg);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertFaultLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertFaultLog for guid: " + guid, e);
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java

```java
package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 主專案 BaseDao 抽象類別 (SSM 基準實作)。
 * 持有 SqlSessionTemplate、JdbcTemplate、DataSource 三個欄位。
 * 採用屬性注入（Field Injection / Setter Injection），由 Spring 容器自動注入主庫 (cxfdemo1) 相關元件。
 */
public abstract class BaseDao {

    @Autowired
    @Qualifier("sqlSessionTemplate1")
    protected SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    @Qualifier("jdbcTemplate1")
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("dataSource1")
    protected DataSource dataSource;

    public BaseDao() {
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java

```java
package com.example.cxfdemo.dao;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ExternalApiLogDao extends BaseDao {

    public void insertApiLog(String url, String requestParam, String responseBody, String exceptionMsg, Date callTime) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("url", url);
        param.put("requestParam", requestParam);
        param.put("responseBody", responseBody);
        param.put("exceptionMsg", exceptionMsg);
        param.put("callTime", callTime);
        
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("ExternalApiLog.insertApiLog", param);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java

```java
package com.example.cxfdemo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用資料庫連線與查詢工具類別 (GenericDao)
 * 模擬主專案與外部系統之多路資料庫連線：
 * - getConnection1(): cxfdemo1 主資料庫連線 (模擬主專案 DB 連線)
 * - getConnection2(): cxfdemo2 資料庫連線 (模擬 Oracle 連線)
 * - getConnection3(): AS400 系統 A 連線 (模擬 AS400 連線)
 * - getConnection4(): AS400 系統 B 連線 (模擬 AS400 連線)
 */
public class GenericDao {

    private static final Logger log = LoggerFactory.getLogger(GenericDao.class);

    public GenericDao() {
    }

    /** 路徑 1: 主資料庫 (cxfdemo1) - 模擬主專案的 DB 連線 */
    public static Connection getConnection1() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo1");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection1 from java:comp/env/jdbc/cxfdemo1", e);
            return null;
        }
    }

    /** 路徑 2: 模擬 Oracle 連線 (cxfdemo2) */
    public static Connection getConnection2() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo2");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection2 from java:comp/env/jdbc/cxfdemo2", e);
            return null;
        }
    }

    /** 路徑 3: 模擬 AS400 系統 A 連線 (as400_a) */
    public static Connection getConnection3() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_a");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection3 from java:comp/env/jdbc/as400_a", e);
            return null;
        }
    }

    /** 路徑 4: 模擬 AS400 系統 B 連線 (as400_b) */
    public static Connection getConnection4() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_b");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection4 from java:comp/env/jdbc/as400_b", e);
            return null;
        }
    }

    /**
     * 查詢系統參數：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     * 透過 getConnection1() 取得 cxfdemo1 主專案連線查詢
     *
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 查得之 prop_value，查無資料或異常時回傳 null
     */
    public static String getSystemProperty(String propKey) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?");
                ps.setString(1, propKey);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("prop_value");
                }
            }
        } catch (Exception e) {
            log.error("Failed to query system_properties for prop_key: " + propKey, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            closeConnection(conn);
        }
        return null;
    }

    /**
     * 安全關閉資料庫連線
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close connection", e);
            }
        }
    }
}

```

---
