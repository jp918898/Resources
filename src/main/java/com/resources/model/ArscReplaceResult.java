package com.resources.model;

/**
 * ARSC替换结果
 * 
 * 记录ARSC文件修改的详细统计
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscReplaceResult {
    
    private final int packageModifications;
    private final int stringPoolModifications;
    private final boolean arscModified;
    
    private ArscReplaceResult(Builder builder) {
        this.packageModifications = builder.packageModifications;
        this.stringPoolModifications = builder.stringPoolModifications;
        this.arscModified = builder.arscModified;
    }
    
    // Getters
    public int getPackageModifications() { 
        return packageModifications; 
    }
    
    public int getStringPoolModifications() { 
        return stringPoolModifications; 
    }
    
    public int getTotalModifications() {
        return packageModifications + stringPoolModifications;
    }
    
    public boolean isArscModified() { 
        return arscModified; 
    }
    
    /**
     * 获取摘要
     */
    public String getSummary() {
        return String.format("ARSC替换: 包名=%d, 字符串池=%d, 总计=%d", 
                           packageModifications, stringPoolModifications, getTotalModifications());
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    // Builder模式
    public static class Builder {
        private int packageModifications = 0;
        private int stringPoolModifications = 0;
        private boolean arscModified = false;
        
        public Builder packageModifications(int packageModifications) {
            this.packageModifications = packageModifications;
            return this;
        }
        
        public Builder stringPoolModifications(int stringPoolModifications) {
            this.stringPoolModifications = stringPoolModifications;
            return this;
        }
        
        public Builder arscModified(boolean arscModified) {
            this.arscModified = arscModified;
            return this;
        }
        
        public ArscReplaceResult build() {
            return new ArscReplaceResult(this);
        }
    }
}
