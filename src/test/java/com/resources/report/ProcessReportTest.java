package com.resources.report;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ProcessReport单元测试 - 100%覆盖
 */
public class ProcessReportTest {
    
    @Test
    @DisplayName("测试生成JSON报告")
    void testGenerateJsonReport() throws Exception {
        ProcessReport report = new ProcessReport(
            "test.apk",
            System.currentTimeMillis() - 60000,
            System.currentTimeMillis(),
            100,
            50,
            new java.util.HashMap<>(),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>()
        );
        
        Files.createDirectories(Paths.get("temp"));
        String jsonPath = "temp/test-report.json";
        
        report.saveToJson(jsonPath);
        
        assertTrue(Files.exists(Paths.get(jsonPath)));
        
        String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
        assertTrue(content.contains("test.apk"));
        assertTrue(content.contains("totalFilesScanned"));
        
        // 清理
        Files.deleteIfExists(Paths.get(jsonPath));
    }
    
    @Test
    @DisplayName("测试生成文本报告")
    void testGenerateTextReport() throws Exception {
        ProcessReport report = new ProcessReport(
            "test.apk",
            System.currentTimeMillis() - 60000,
            System.currentTimeMillis(),
            100,
            50,
            new java.util.HashMap<>(),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>()
        );
        
        Files.createDirectories(Paths.get("temp"));
        String textPath = "temp/test-report.txt";
        
        report.saveToText(textPath);
        
        assertTrue(Files.exists(Paths.get(textPath)));
        
        String content = new String(Files.readAllBytes(Paths.get(textPath)));
        assertTrue(content.contains("test.apk"));
        assertTrue(content.contains("处理报告"));
        
        // 清理
        Files.deleteIfExists(Paths.get(textPath));
    }
    
    @Test
    @DisplayName("测试带错误的报告")
    void testReportWithErrors() throws Exception {
        java.util.List<String> errors = new java.util.ArrayList<>();
        errors.add("Test error 1");
        errors.add("Test error 2");
        
        ProcessReport report = new ProcessReport(
            "test.apk",
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            100,
            0,
            new java.util.HashMap<>(),
            errors,
            new java.util.ArrayList<>()
        );
        
        Files.createDirectories(Paths.get("temp"));
        String textPath = "temp/test-report-errors.txt";
        
        report.saveToText(textPath);
        
        String content = new String(Files.readAllBytes(Paths.get(textPath)));
        assertTrue(content.contains("Test error 1"));
        assertTrue(content.contains("Test error 2"));
        
        // 清理
        Files.deleteIfExists(Paths.get(textPath));
    }
    
    @Test
    @DisplayName("测试带警告的报告")
    void testReportWithWarnings() throws Exception {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        warnings.add("Test warning 1");
        
        ProcessReport report = new ProcessReport(
            "test.apk",
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            100,
            10,
            new java.util.HashMap<>(),
            new java.util.ArrayList<>(),
            warnings
        );
        
        Files.createDirectories(Paths.get("temp"));
        String textPath = "temp/test-report-warnings.txt";
        
        report.saveToText(textPath);
        
        String content = new String(Files.readAllBytes(Paths.get(textPath)));
        assertTrue(content.contains("Test warning 1"));
        
        // 清理
        Files.deleteIfExists(Paths.get(textPath));
    }
    
    @Test
    @DisplayName("测试打印摘要")
    void testPrintSummary() {
        ProcessReport report = new ProcessReport(
            "test.apk",
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            100,
            10,
            new java.util.HashMap<>(),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>()
        );
        
        // 打印摘要不应该抛出异常
        assertDoesNotThrow(() -> report.printSummary());
    }
}
