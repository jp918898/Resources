package com.resources.validator;

import com.resources.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * aapt2静态验证器
 * 
 * 使用aapt2工具验证APK资源的合法性：
 * - aapt2 dump xmltree（验证XML结构）
 * - aapt2 dump resources（验证resources.arsc）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class Aapt2Validator {
    
    private static final Logger log = LoggerFactory.getLogger(Aapt2Validator.class);
    
    private final String aapt2Path;
    
    public Aapt2Validator() {
        this("bin/win/aapt2.exe");
    }
    
    public Aapt2Validator(String aapt2Path) {
        this.aapt2Path = Objects.requireNonNull(aapt2Path, "aapt2Path不能为null");
        
        // 验证aapt2是否存在
        File aapt2File = new File(aapt2Path);
        if (!aapt2File.exists()) {
            log.warn("aapt2工具不存在: {}", aapt2Path);
        } else {
            log.info("aapt2工具路径: {}", aapt2File.getAbsolutePath());
        }
    }
    
    /**
     * 验证APK
     * 
     * @param apkPath APK文件路径
     * @return 验证结果
     */
    public ValidationResult validate(String apkPath) {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.info("aapt2验证: {}", apkPath);
        
        ValidationResult.Builder builder = new ValidationResult.Builder();
        
        // 1. 验证AndroidManifest.xml
        boolean manifestValid = validateXml(apkPath, "AndroidManifest.xml");
        if (manifestValid) {
            builder.addPassed(ValidationResult.ValidationLevel.AAPT2_STATIC, 
                            "AndroidManifest.xml验证通过");
        } else {
            builder.addFailed(ValidationResult.ValidationLevel.AAPT2_STATIC, 
                            "AndroidManifest.xml验证失败", 
                            "请检查aapt2输出日志");
        }
        
        // 2. 验证resources.arsc
        boolean resourcesValid = validateResources(apkPath);
        if (resourcesValid) {
            builder.addPassed(ValidationResult.ValidationLevel.AAPT2_STATIC, 
                            "resources.arsc验证通过");
        } else {
            builder.addFailed(ValidationResult.ValidationLevel.AAPT2_STATIC, 
                            "resources.arsc验证失败", 
                            "请检查aapt2输出日志");
        }
        
        return builder.build();
    }
    
    /**
     * 验证XML文件
     * 
     * @param apkPath APK路径
     * @param xmlPath XML文件路径（在APK中）
     * @return true=验证通过
     */
    public boolean validateXml(String apkPath, String xmlPath) {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        Objects.requireNonNull(xmlPath, "xmlPath不能为null");
        
        log.debug("验证XML: {} in {}", xmlPath, apkPath);
        
        try {
            // 执行aapt2 dump xmltree
            ProcessBuilder pb = new ProcessBuilder(
                aapt2Path, "dump", "xmltree", apkPath, "--file", xmlPath
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            String output = readProcessOutput(process);
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("aapt2 dump xmltree失败: exitCode={}", exitCode);
                log.error("输出:\n{}", output);
                return false;
            }
            
            // 检查输出中是否有ERROR
            if (output.toLowerCase().contains("error")) {
                log.error("aapt2输出包含错误:\n{}", output);
                return false;
            }
            
            log.debug("XML验证通过: {}", xmlPath);
            return true;
            
        } catch (Exception e) {
            log.error("XML验证失败: {}", xmlPath, e);
            return false;
        }
    }
    
    /**
     * 验证resources.arsc
     * 
     * @param apkPath APK路径
     * @return true=验证通过
     */
    public boolean validateResources(String apkPath) {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.debug("验证resources.arsc: {}", apkPath);
        
        try {
            // 执行aapt2 dump resources
            ProcessBuilder pb = new ProcessBuilder(
                aapt2Path, "dump", "resources", apkPath
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("aapt2 dump resources失败: exitCode={}", exitCode);
                log.error("输出:\n{}", output);
                return false;
            }
            
            // 检查输出中是否有ERROR
            if (output.toLowerCase().contains("error")) {
                log.error("aapt2输出包含错误:\n{}", output);
                return false;
            }
            
            log.debug("resources.arsc验证通过");
            return true;
            
        } catch (Exception e) {
            log.error("resources.arsc验证失败", e);
            return false;
        }
    }
    
    /**
     * 获取dump输出
     * 
     * @param apkPath APK路径
     * @param xmlPath XML路径
     * @return dump输出内容
     */
    public String getDumpOutput(String apkPath, String xmlPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                aapt2Path, "dump", "xmltree", apkPath, "--file", xmlPath
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            process.waitFor();
            
            return output;
            
        } catch (Exception e) {
            log.error("获取dump输出失败", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        return output.toString();
    }
    
    /**
     * 批量验证XML文件
     * 
     * @param apkPath APK路径
     * @param xmlPaths XML文件路径列表
     * @return 所有验证通过则返回true
     */
    public boolean validateXmlBatch(String apkPath, List<String> xmlPaths) {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        Objects.requireNonNull(xmlPaths, "xmlPaths不能为null");
        
        log.info("批量验证XML: {} 个文件", xmlPaths.size());
        
        int passedCount = 0;
        int failedCount = 0;
        
        for (String xmlPath : xmlPaths) {
            if (validateXml(apkPath, xmlPath)) {
                passedCount++;
            } else {
                failedCount++;
                log.warn("XML验证失败: {}", xmlPath);
            }
        }
        
        log.info("批量验证完成: 通过={}, 失败={}", passedCount, failedCount);
        
        return failedCount == 0;
    }
}

