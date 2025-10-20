package com.resources.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 处理报告生成器
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ProcessReport {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessReport.class);
    
    private final String apkPath;
    private final long startTime;
    private final long endTime;
    private final int totalFilesScanned;
    private final int totalModifications;
    private final Map<String, Integer> modificationsByType;
    private final List<String> errors;
    private final List<String> warnings;
    
    public ProcessReport(String apkPath, long startTime, long endTime,
                        int totalFilesScanned, int totalModifications,
                        Map<String, Integer> modificationsByType,
                        List<String> errors, List<String> warnings) {
        this.apkPath = apkPath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalFilesScanned = totalFilesScanned;
        this.totalModifications = totalModifications;
        this.modificationsByType = new HashMap<>(modificationsByType);
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }
    
    public void saveToJson(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apkPath", apkPath);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("durationMs", endTime - startTime);
        data.put("totalFilesScanned", totalFilesScanned);
        data.put("totalModifications", totalModifications);
        data.put("modificationsByType", modificationsByType);
        data.put("errors", errors);
        data.put("warnings", warnings);
        
        String json = mapper.writeValueAsString(data);
        Files.write(Paths.get(path), json.getBytes(StandardCharsets.UTF_8));
        
        log.info("报告已保存（JSON）: {}", path);
    }
    
    public void saveToText(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("    处理报告\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(String.format("APK: %s\n", apkPath));
        sb.append(String.format("耗时: %d ms (%.2f 秒)\n", 
                               endTime - startTime, (endTime - startTime) / 1000.0));
        sb.append(String.format("扫描文件: %d\n", totalFilesScanned));
        sb.append(String.format("修改总数: %d\n", totalModifications));
        
        if (!modificationsByType.isEmpty()) {
            sb.append("\n修改类型统计:\n");
            modificationsByType.forEach((type, count) -> 
                sb.append(String.format("  - %s: %d\n", type, count)));
        }
        
        if (!warnings.isEmpty()) {
            sb.append(String.format("\n警告 (%d):\n", warnings.size()));
            warnings.forEach(w -> sb.append("  ⚠ " + w + "\n"));
        }
        
        if (!errors.isEmpty()) {
            sb.append(String.format("\n错误 (%d):\n", errors.size()));
            errors.forEach(e -> sb.append("  ✗ " + e + "\n"));
        }
        
        sb.append("════════════════════════════════════════\n");
        
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
        
        log.info("报告已保存（文本）: {}", path);
    }
    
    public void printSummary() {
        System.out.println("════════════════════════════════════════");
        System.out.println("    处理摘要");
        System.out.println("════════════════════════════════════════");
        System.out.println("APK: " + apkPath);
        System.out.println(String.format("耗时: %d ms", endTime - startTime));
        System.out.println(String.format("扫描: %d 个文件", totalFilesScanned));
        System.out.println(String.format("修改: %d 处", totalModifications));
        System.out.println("════════════════════════════════════════");
    }
}

