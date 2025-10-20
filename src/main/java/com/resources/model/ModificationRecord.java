package com.resources.model;

import java.util.Objects;

/**
 * 修改记录 - 记录单个修改操作的详细信息
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ModificationRecord {
    
    public enum ModificationType {
        TAG_NAME_REPLACE,        // 标签名替换
        ATTRIBUTE_VALUE_REPLACE, // 属性值替换
        ARSC_STRING_REPLACE,     // ARSC字符串替换
        ARSC_PACKAGE_NAME,       // ARSC包名替换
        DATABINDING_TYPE,        // Data Binding类型替换
        DATABINDING_IMPORT,      // Data Binding导入替换
        DATABINDING_EXPRESSION   // Data Binding表达式替换
    }
    
    private final String filePath;
    private final ModificationType type;
    private final String location;
    private final String originalValue;
    private final String newValue;
    private final long timestamp;
    private final boolean applied;
    private final String errorMessage;
    
    private ModificationRecord(Builder builder) {
        this.filePath = Objects.requireNonNull(builder.filePath);
        this.type = Objects.requireNonNull(builder.type);
        this.location = Objects.requireNonNull(builder.location);
        this.originalValue = Objects.requireNonNull(builder.originalValue);
        this.newValue = Objects.requireNonNull(builder.newValue);
        this.timestamp = builder.timestamp;
        this.applied = builder.applied;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getFilePath() { return filePath; }
    public ModificationType getType() { return type; }
    public String getLocation() { return location; }
    public String getOriginalValue() { return originalValue; }
    public String getNewValue() { return newValue; }
    public long getTimestamp() { return timestamp; }
    public boolean isApplied() { return applied; }
    public String getErrorMessage() { return errorMessage; }
    public boolean hasError() { return errorMessage != null; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModificationRecord that = (ModificationRecord) o;
        return Objects.equals(filePath, that.filePath) &&
               type == that.type &&
               Objects.equals(location, that.location) &&
               Objects.equals(originalValue, that.originalValue) &&
               Objects.equals(newValue, that.newValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath, type, location, originalValue, newValue);
    }
    
    @Override
    public String toString() {
        String status = applied ? "✓" : (hasError() ? "✗" : "○");
        return String.format("[%s] %s @ %s: '%s' → '%s'%s", 
                           status, type, location, originalValue, newValue,
                           hasError() ? " (错误: " + errorMessage + ")" : "");
    }
    
    // Builder模式
    public static class Builder {
        private String filePath;
        private ModificationType type;
        private String location;
        private String originalValue;
        private String newValue;
        private long timestamp = System.currentTimeMillis();
        private boolean applied = false;
        private String errorMessage;
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder type(ModificationType type) {
            this.type = type;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder originalValue(String originalValue) {
            this.originalValue = originalValue;
            return this;
        }
        
        public Builder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder applied(boolean applied) {
            this.applied = applied;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public ModificationRecord build() {
            return new ModificationRecord(this);
        }
    }
}

