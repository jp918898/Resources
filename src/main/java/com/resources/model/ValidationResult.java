package com.resources.model;

import java.util.*;

/**
 * 验证结果
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ValidationResult {
    
    /**
     * 验证级别
     */
    public enum ValidationLevel {
        AAPT2_STATIC,      // aapt2静态验证
        DEX_CROSS,         // DEX交叉验证
        INTEGRITY,         // 完整性检查
        RUNTIME            // 运行时验证（可选）
    }
    
    /**
     * 验证状态
     */
    public enum ValidationStatus {
        PASSED,    // 通过
        FAILED,    // 失败
        SKIPPED,   // 跳过
        WARNING    // 警告
    }
    
    /**
     * 单个验证项结果
     */
    public static class ValidationItem {
        private final ValidationLevel level;
        private final ValidationStatus status;
        private final String message;
        private final String details;
        
        public ValidationItem(ValidationLevel level, ValidationStatus status, 
                            String message, String details) {
            this.level = Objects.requireNonNull(level);
            this.status = Objects.requireNonNull(status);
            this.message = Objects.requireNonNull(message);
            this.details = details;
        }
        
        public ValidationLevel getLevel() { return level; }
        public ValidationStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        
        public boolean isPassed() { return status == ValidationStatus.PASSED; }
        public boolean isFailed() { return status == ValidationStatus.FAILED; }
        
        @Override
        public String toString() {
            String statusSymbol = status == ValidationStatus.PASSED ? "✓" :
                                status == ValidationStatus.FAILED ? "✗" :
                                status == ValidationStatus.WARNING ? "⚠" : "-";
            return String.format("[%s] %s: %s - %s", 
                               statusSymbol, level, message, 
                               details != null ? details : "");
        }
    }
    
    private final List<ValidationItem> items;
    private final boolean overallSuccess;
    
    public ValidationResult(List<ValidationItem> items) {
        this.items = new ArrayList<>(items);
        this.overallSuccess = items.stream().noneMatch(ValidationItem::isFailed);
    }
    
    public List<ValidationItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public boolean isOverallSuccess() {
        return overallSuccess;
    }
    
    public List<ValidationItem> getFailedItems() {
        List<ValidationItem> failed = new ArrayList<>();
        for (ValidationItem item : items) {
            if (item.isFailed()) {
                failed.add(item);
            }
        }
        return failed;
    }
    
    public List<ValidationItem> getWarningItems() {
        List<ValidationItem> warnings = new ArrayList<>();
        for (ValidationItem item : items) {
            if (item.status == ValidationStatus.WARNING) {
                warnings.add(item);
            }
        }
        return warnings;
    }
    
    /**
     * 获取摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("总体状态: %s\n", overallSuccess ? "✓ 通过" : "✗ 失败"));
        sb.append(String.format("验证项总数: %d\n", items.size()));
        
        int passed = 0, failed = 0, skipped = 0, warnings = 0;
        for (ValidationItem item : items) {
            switch (item.status) {
                case PASSED: passed++; break;
                case FAILED: failed++; break;
                case SKIPPED: skipped++; break;
                case WARNING: warnings++; break;
            }
        }
        
        sb.append(String.format("  通过: %d\n", passed));
        sb.append(String.format("  失败: %d\n", failed));
        sb.append(String.format("  跳过: %d\n", skipped));
        sb.append(String.format("  警告: %d\n", warnings));
        
        if (failed > 0) {
            sb.append("\n失败项:\n");
            getFailedItems().forEach(item -> sb.append("  " + item + "\n"));
        }
        
        if (warnings > 0) {
            sb.append("\n警告项:\n");
            getWarningItems().forEach(item -> sb.append("  " + item + "\n"));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    // Builder模式
    public static class Builder {
        private List<ValidationItem> items = new ArrayList<>();
        
        public Builder addItem(ValidationLevel level, ValidationStatus status, 
                              String message, String details) {
            items.add(new ValidationItem(level, status, message, details));
            return this;
        }
        
        public Builder addPassed(ValidationLevel level, String message) {
            return addItem(level, ValidationStatus.PASSED, message, null);
        }
        
        public Builder addFailed(ValidationLevel level, String message, String details) {
            return addItem(level, ValidationStatus.FAILED, message, details);
        }
        
        public Builder addWarning(ValidationLevel level, String message, String details) {
            return addItem(level, ValidationStatus.WARNING, message, details);
        }
        
        public Builder addSkipped(ValidationLevel level, String message) {
            return addItem(level, ValidationStatus.SKIPPED, message, null);
        }
        
        public ValidationResult build() {
            return new ValidationResult(items);
        }
    }
}

