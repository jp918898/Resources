package com.resources.model;

import java.util.*;

/**
 * 事务对象 - 用于两阶段提交
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class Transaction {
    
    public enum TransactionStatus {
        CREATED,      // 已创建
        VALIDATING,   // 验证中
        VALIDATED,    // 已验证
        EXECUTING,    // 执行中
        COMMITTED,    // 已提交
        ROLLED_BACK,  // 已回滚
        FAILED        // 失败
    }
    
    private final String transactionId;
    private final long createTime;
    private final String apkPath;
    private final String snapshotPath;
    
    private TransactionStatus status;
    private String outputApkPath;
    private long commitTime;
    private List<String> modifiedFiles;
    private String errorMessage;
    
    public Transaction(String transactionId, String apkPath, String snapshotPath) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.apkPath = Objects.requireNonNull(apkPath);
        this.snapshotPath = Objects.requireNonNull(snapshotPath);
        this.createTime = System.currentTimeMillis();
        this.status = TransactionStatus.CREATED;
        this.modifiedFiles = new ArrayList<>();
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public long getCreateTime() { return createTime; }
    public String getApkPath() { return apkPath; }
    public String getSnapshotPath() { return snapshotPath; }
    public TransactionStatus getStatus() { return status; }
    public String getOutputApkPath() { return outputApkPath; }
    public long getCommitTime() { return commitTime; }
    public List<String> getModifiedFiles() { 
        return Collections.unmodifiableList(modifiedFiles); 
    }
    public String getErrorMessage() { return errorMessage; }
    
    public long getDurationMs() {
        if (commitTime > 0) {
            return commitTime - createTime;
        }
        return System.currentTimeMillis() - createTime;
    }
    
    // Setters（public，但只应由TransactionManager调用）
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
    
    public void setOutputApkPath(String outputApkPath) {
        this.outputApkPath = outputApkPath;
    }
    
    public void setCommitTime(long commitTime) {
        this.commitTime = commitTime;
    }
    
    public void addModifiedFile(String filePath) {
        this.modifiedFiles.add(filePath);
    }
    
    public void setModifiedFiles(List<String> modifiedFiles) {
        this.modifiedFiles = new ArrayList<>(modifiedFiles);
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 是否可以提交
     */
    public boolean canCommit() {
        return status == TransactionStatus.VALIDATED;
    }
    
    /**
     * 是否可以回滚
     */
    public boolean canRollback() {
        return status != TransactionStatus.COMMITTED && 
               status != TransactionStatus.ROLLED_BACK;
    }
    
    /**
     * 是否已完成（成功或失败）
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMMITTED || 
               status == TransactionStatus.ROLLED_BACK ||
               status == TransactionStatus.FAILED;
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id='%s', status=%s, apk='%s', duration=%dms}", 
                           transactionId, status, apkPath, getDurationMs());
    }
}

