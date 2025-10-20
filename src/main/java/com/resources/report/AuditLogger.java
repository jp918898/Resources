package com.resources.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志记录器
 * 
 * 记录所有修改操作、验证结果、事务操作
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class AuditLogger {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final List<String> auditLogs;
    private final String logFilePath;
    
    public AuditLogger() {
        this("logs/audit.log");
    }
    
    public AuditLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        this.auditLogs = new ArrayList<>();
        
        // 确保日志目录存在
        try {
            Files.createDirectories(Paths.get(logFilePath).getParent());
        } catch (IOException e) {
            log.error("创建日志目录失败", e);
        }
    }
    
    public void logModification(String file, String location, 
                              String oldValue, String newValue) {
        String logEntry = String.format("[%s] MODIFY | %s | %s | '%s' -> '%s'",
                                       now(), file, location, oldValue, newValue);
        auditLogs.add(logEntry);
        appendToFile(logEntry);
    }
    
    public void logValidation(String validator, boolean passed, String details) {
        String status = passed ? "PASS" : "FAIL";
        String logEntry = String.format("[%s] VALIDATE | %s | %s | %s",
                                       now(), validator, status, details);
        auditLogs.add(logEntry);
        appendToFile(logEntry);
    }
    
    public void logTransaction(String transactionId, String action, String status) {
        String logEntry = String.format("[%s] TRANSACTION | %s | %s | %s",
                                       now(), transactionId, action, status);
        auditLogs.add(logEntry);
        appendToFile(logEntry);
    }
    
    public void saveAuditLog(String path) throws IOException {
        Files.write(Paths.get(path), 
                   String.join("\n", auditLogs).getBytes(StandardCharsets.UTF_8));
        log.info("审计日志已保存: {}", path);
    }
    
    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
    
    private void appendToFile(String logEntry) {
        try {
            Files.write(Paths.get(logFilePath),
                       (logEntry + "\n").getBytes(StandardCharsets.UTF_8),
                       StandardOpenOption.CREATE,
                       StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("写入审计日志失败", e);
        }
    }
}

