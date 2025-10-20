package com.resources.model;

import java.util.*;

/**
 * 处理结果报告
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ProcessingResult {
    
    private final String apkPath;
    private final long startTime;
    private final long endTime;
    private final boolean success;
    private final int totalFilesScanned;
    private final int totalModifications;
    private final Map<String, Integer> modificationsByType;
    private final List<String> errors;
    private final List<String> warnings;
    private final ValidationResult validationResult;
    
    private ProcessingResult(Builder builder) {
        this.apkPath = builder.apkPath;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.success = builder.success;
        this.totalFilesScanned = builder.totalFilesScanned;
        this.totalModifications = builder.totalModifications;
        this.modificationsByType = new HashMap<>(builder.modificationsByType);
        this.errors = new ArrayList<>(builder.errors);
        this.warnings = new ArrayList<>(builder.warnings);
        this.validationResult = builder.validationResult;
    }
    
    // Getters
    public String getApkPath() { return apkPath; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDurationMs() { return endTime - startTime; }
    public boolean isSuccess() { return success; }
    public int getTotalFilesScanned() { return totalFilesScanned; }
    public int getTotalModifications() { return totalModifications; }
    public Map<String, Integer> getModificationsByType() { 
        return Collections.unmodifiableMap(modificationsByType); 
    }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public ValidationResult getValidationResult() { return validationResult; }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    
    /**
     * 打印摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("    处理结果摘要\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(String.format("APK路径: %s\n", apkPath));
        sb.append(String.format("状态: %s\n", success ? "✓ 成功" : "✗ 失败"));
        sb.append(String.format("耗时: %d ms (%.2f 秒)\n", getDurationMs(), getDurationMs() / 1000.0));
        sb.append(String.format("扫描文件数: %d\n", totalFilesScanned));
        sb.append(String.format("修改总数: %d\n", totalModifications));
        
        if (!modificationsByType.isEmpty()) {
            sb.append("\n修改类型统计:\n");
            modificationsByType.forEach((type, count) -> 
                sb.append(String.format("  - %s: %d\n", type, count)));
        }
        
        if (hasWarnings()) {
            sb.append(String.format("\n⚠ 警告 (%d):\n", warnings.size()));
            warnings.forEach(w -> sb.append("  - " + w + "\n"));
        }
        
        if (hasErrors()) {
            sb.append(String.format("\n✗ 错误 (%d):\n", errors.size()));
            errors.forEach(e -> sb.append("  - " + e + "\n"));
        }
        
        if (validationResult != null) {
            sb.append("\n验证结果:\n");
            sb.append(validationResult.getSummary());
        }
        
        sb.append("════════════════════════════════════════\n");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    // Builder模式
    public static class Builder {
        private String apkPath;
        private long startTime;
        private long endTime;
        private boolean success;
        private int totalFilesScanned;
        private int totalModifications;
        private Map<String, Integer> modificationsByType = new HashMap<>();
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private ValidationResult validationResult;
        
        public Builder apkPath(String apkPath) {
            this.apkPath = apkPath;
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder totalFilesScanned(int totalFilesScanned) {
            this.totalFilesScanned = totalFilesScanned;
            return this;
        }
        
        public Builder totalModifications(int totalModifications) {
            this.totalModifications = totalModifications;
            return this;
        }
        
        public Builder modificationsByType(Map<String, Integer> modificationsByType) {
            this.modificationsByType = new HashMap<>(modificationsByType);
            return this;
        }
        
        public Builder addModification(String type, int count) {
            this.modificationsByType.merge(type, count, Integer::sum);
            return this;
        }
        
        public Builder errors(List<String> errors) {
            this.errors = new ArrayList<>(errors);
            return this;
        }
        
        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }
        
        public Builder warnings(List<String> warnings) {
            this.warnings = new ArrayList<>(warnings);
            return this;
        }
        
        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }
        
        public Builder validationResult(ValidationResult validationResult) {
            this.validationResult = validationResult;
            return this;
        }
        
        public ProcessingResult build() {
            return new ProcessingResult(this);
        }
    }
}

