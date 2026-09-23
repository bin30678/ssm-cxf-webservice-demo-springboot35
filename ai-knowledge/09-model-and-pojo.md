# 領域模型、DTO 與資料物件 (Domain Models & POJOs)

本文件收錄系統所使用的 POJO 領域實體與資料傳輸物件 (DTO)，包含 JAXB XML 註解模型與郵件資料物件。

> 本文件收錄 2 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java

```java
package com.example.cxfdemo.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolicyInfo", propOrder = {"policyNo", "holderName", "productName", "status"})
public class PolicyInfo {
    private String policyNo;
    private String holderName;
    private String productName;
    private String status;

    public PolicyInfo() {}

    public PolicyInfo(String policyNo, String holderName, String productName, String status) {
        this.policyNo = policyNo;
        this.holderName = holderName;
        this.productName = productName;
        this.status = status;
    }

    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java

```java
package com.example.cxfdemo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 郵件資料模型 (Mail DTO)
 * 包含主旨、內文、收件者、副本與附件資訊。
 */
public class Mail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subject;
    private String content;
    private List<String> to = new ArrayList<String>();
    private List<String> cc = new ArrayList<String>();
    private List<String> attachmentPaths = new ArrayList<String>();

    public Mail() {
    }

    public Mail(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public Mail(String subject, String content, List<String> to) {
        this.subject = subject;
        this.content = content;
        if (to != null) {
            this.to = to;
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public void addTo(String recipient) {
        if (recipient != null && !recipient.trim().isEmpty()) {
            this.to.add(recipient);
        }
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public void addCc(String ccRecipient) {
        if (ccRecipient != null && !ccRecipient.trim().isEmpty()) {
            this.cc.add(ccRecipient);
        }
    }

    public List<String> getAttachmentPaths() {
        return attachmentPaths;
    }

    public void setAttachmentPaths(List<String> attachmentPaths) {
        this.attachmentPaths = attachmentPaths;
    }

    public void addAttachmentPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            this.attachmentPaths.add(path);
        }
    }

    @Override
    public String toString() {
        return "Mail{" +
                "subject='" + subject + '\'' +
                ", to=" + to +
                ", cc=" + cc +
                ", attachmentPaths=" + attachmentPaths +
                '}';
    }
}

```

---
