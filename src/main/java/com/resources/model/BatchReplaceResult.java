package com.resources.model;

import java.util.*;

/**
 * 批量替换结果
 * 
 * 包含处理统计信息和结果数据
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class BatchReplaceResult {
    
    private final Map<String, byte[]> results;
    private final int successCount;
    private final int skippedCount;
    private final int errorCount;
    private final List<String> errorFiles;
    
    private BatchReplaceResult(Builder builder) {
        this.results = new LinkedHashMap<>(builder.results);
        this.successCount = builder.successCount;
        this.skippedCount = builder.skippedCount;
        this.errorCount = builder.errorCount;
        this.errorFiles = new ArrayList<>(builder.errorFiles);
    }
    
    // Getters
    public Map<String, byte[]> getResults() { 
        return Collections.unmodifiableMap(results); 
    }
    
    public int getSuccessCount() { 
        return successCount; 
    }
    
    public int getSkippedCount() { 
        return skippedCount; 
    }
    
    public int getErrorCount() { 
        return errorCount; 
    }
    
    public List<String> getErrorFiles() { 
        return Collections.unmodifiableList(errorFiles); 
    }
    
    public int getTotalProcessed() {
        return successCount + skippedCount + errorCount;
    }
    
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    public boolean hasModifications() {
        return successCount > 0;
    }
    
    /**
     * 获取摘要
     */
    public String getSummary() {
        return String.format("批量处理完成: 成功=%d, 跳过=%d, 失败=%d", 
                           successCount, skippedCount, errorCount);
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    // Builder模式
    public static class Builder {
        private Map<String, byte[]> results = new LinkedHashMap<>();
        private int successCount = 0;
        private int skippedCount = 0;
        private int errorCount = 0;
        private List<String> errorFiles = new ArrayList<>();
        
        public Builder results(Map<String, byte[]> results) {
            this.results = new LinkedHashMap<>(results);
            return this;
        }
        
        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }
        
        public Builder skippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
            return this;
        }
        
        public Builder errorCount(int errorCount) {
            this.errorCount = errorCount;
            return this;
        }
        
        public Builder errorFiles(List<String> errorFiles) {
            this.errorFiles = new ArrayList<>(errorFiles);
            return this;
        }
        
        public Builder addErrorFile(String errorFile) {
            this.errorFiles.add(errorFile);
            return this;
        }
        
        public BatchReplaceResult build() {
            return new BatchReplaceResult(this);
        }
    }
}
