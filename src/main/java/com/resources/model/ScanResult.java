package com.resources.model;

import java.util.Objects;

/**
 * 扫描结果 - 记录发现的类名/包名位置和值
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ScanResult {
    
    /**
     * 语义类型枚举
     */
    public enum SemanticType {
        TAG_NAME,           // 标签名本身是类名（如<com.example.MyView>）
        ATTRIBUTE_VALUE,    // 属性值是类名（如android:name="com.example.Fragment"）
        DATABINDING_TYPE,   // Data Binding的type属性
        DATABINDING_IMPORT, // Data Binding的import
        DATABINDING_EXPR,   // Data Binding表达式中的T(FQCN)
        ARSC_STRING,        // resources.arsc中的字符串
        PACKAGE_NAME        // 包名（ResTable_package.name）
    }
    
    private final String filePath;        // 文件路径
    private final SemanticType semanticType; // 语义类型
    private final String location;        // 位置描述（如"line 15, tag fragment, attr android:name"）
    private final String originalValue;   // 原始值
    private final String newValue;        // 新值（如果已确定）
    private final int stringPoolIndex;    // ARSC字符串池索引（仅ARSC_STRING类型）
    
    private ScanResult(Builder builder) {
        this.filePath = Objects.requireNonNull(builder.filePath, "filePath不能为null");
        this.semanticType = Objects.requireNonNull(builder.semanticType, "semanticType不能为null");
        this.location = Objects.requireNonNull(builder.location, "location不能为null");
        this.originalValue = Objects.requireNonNull(builder.originalValue, "originalValue不能为null");
        this.newValue = builder.newValue;
        this.stringPoolIndex = builder.stringPoolIndex;
    }
    
    // Getters
    public String getFilePath() { return filePath; }
    public SemanticType getSemanticType() { return semanticType; }
    public String getLocation() { return location; }
    public String getOriginalValue() { return originalValue; }
    public String getNewValue() { return newValue; }
    public int getStringPoolIndex() { return stringPoolIndex; }
    public boolean hasNewValue() { return newValue != null; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScanResult that = (ScanResult) o;
        return stringPoolIndex == that.stringPoolIndex &&
               Objects.equals(filePath, that.filePath) &&
               semanticType == that.semanticType &&
               Objects.equals(location, that.location) &&
               Objects.equals(originalValue, that.originalValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath, semanticType, location, originalValue, stringPoolIndex);
    }
    
    @Override
    public String toString() {
        return String.format("ScanResult{file='%s', type=%s, location='%s', original='%s', new='%s'}",
                filePath, semanticType, location, originalValue, newValue);
    }
    
    // Builder模式
    public static class Builder {
        private String filePath;
        private SemanticType semanticType;
        private String location;
        private String originalValue;
        private String newValue;
        private int stringPoolIndex = -1;
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder semanticType(SemanticType semanticType) {
            this.semanticType = semanticType;
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
        
        public Builder stringPoolIndex(int stringPoolIndex) {
            this.stringPoolIndex = stringPoolIndex;
            return this;
        }
        
        public ScanResult build() {
            return new ScanResult(this);
        }
    }
}

