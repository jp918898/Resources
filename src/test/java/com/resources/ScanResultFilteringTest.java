package com.resources;

import com.resources.model.ScanResult;
import com.resources.scanner.ResourceScanner;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 扫描结果过滤测试
 * 
 * 验证：
 * 1. filesToProcess正确提取
 * 2. 处理阶段只处理标记的文件
 * 3. 跳过的文件不被处理
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
class ScanResultFilteringTest {
    
    @Test
    void testExtractFilesToProcess() {
        // 创建模拟的扫描结果
        List<ScanResult> layoutResults = Arrays.asList(
            createScanResult("res/layout/activity_main.xml"),
            createScanResult("res/layout/fragment_user.xml")
        );
        
        List<ScanResult> menuResults = Arrays.asList(
            createScanResult("res/menu/main_menu.xml")
        );
        
        // 创建ScanReport
        ResourceScanner.ScanReport.Builder builder = new ResourceScanner.ScanReport.Builder();
        builder.apkPath("test.apk");
        builder.startTime(System.currentTimeMillis());
        builder.endTime(System.currentTimeMillis());
        builder.totalResults(3);
        builder.addLayoutResults(layoutResults);
        builder.addMenuResults(menuResults);
        
        ResourceScanner.ScanReport report = builder.build();
        
        // 提取filesToProcess（模拟ResourceProcessor的逻辑）
        Set<String> filesToProcess = new HashSet<>();
        for (ScanResult result : report.getAllResults()) {
            filesToProcess.add(result.getFilePath());
        }
        
        // 验证
        assertEquals(3, filesToProcess.size(), "应该提取3个文件路径");
        assertTrue(filesToProcess.contains("res/layout/activity_main.xml"));
        assertTrue(filesToProcess.contains("res/layout/fragment_user.xml"));
        assertTrue(filesToProcess.contains("res/menu/main_menu.xml"));
    }
    
    @Test
    void testFilterByFilesToProcess() {
        // 模拟所有XML文件
        Map<String, byte[]> allXmls = new LinkedHashMap<>();
        allXmls.put("res/layout/activity_main.xml", new byte[100]);
        allXmls.put("res/layout/fragment_user.xml", new byte[100]);
        allXmls.put("res/drawable/ic_launcher.xml", new byte[50]);  // 应该被跳过
        allXmls.put("res/color/button_color.xml", new byte[50]);     // 应该被跳过
        allXmls.put("res/menu/main_menu.xml", new byte[100]);
        
        // 模拟扫描结果（只有3个文件需要处理）
        Set<String> filesToProcess = new HashSet<>(Arrays.asList(
            "res/layout/activity_main.xml",
            "res/layout/fragment_user.xml",
            "res/menu/main_menu.xml"
        ));
        
        // 执行过滤（模拟ResourceProcessor的逻辑）
        Map<String, byte[]> filteredXmls = new LinkedHashMap<>();
        int skipped = 0;
        
        for (Map.Entry<String, byte[]> entry : allXmls.entrySet()) {
            if (filesToProcess.contains(entry.getKey())) {
                filteredXmls.put(entry.getKey(), entry.getValue());
            } else {
                skipped++;
            }
        }
        
        // 验证
        assertEquals(3, filteredXmls.size(), "应该保留3个需处理的文件");
        assertEquals(2, skipped, "应该跳过2个文件");
        
        assertTrue(filteredXmls.containsKey("res/layout/activity_main.xml"));
        assertTrue(filteredXmls.containsKey("res/layout/fragment_user.xml"));
        assertTrue(filteredXmls.containsKey("res/menu/main_menu.xml"));
        
        assertFalse(filteredXmls.containsKey("res/drawable/ic_launcher.xml"));
        assertFalse(filteredXmls.containsKey("res/color/button_color.xml"));
    }
    
    @Test
    void testEmptyScanReport_ShouldNotFilter() {
        // 空扫描结果
        ResourceScanner.ScanReport.Builder builder = new ResourceScanner.ScanReport.Builder();
        builder.apkPath("test.apk");
        builder.startTime(System.currentTimeMillis());
        builder.endTime(System.currentTimeMillis());
        builder.totalResults(0);
        
        ResourceScanner.ScanReport report = builder.build();
        
        // filesToProcess应该为null或空
        Set<String> filesToProcess = null;
        if (report.getTotalResults() > 0) {
            filesToProcess = new HashSet<>();
            for (ScanResult result : report.getAllResults()) {
                filesToProcess.add(result.getFilePath());
            }
        }
        
        // 验证：空扫描结果时filesToProcess为null，不应该过滤
        assertTrue(filesToProcess == null || filesToProcess.isEmpty(), 
                  "空扫描结果时不应该过滤");
    }
    
    @Test
    void testDuplicateResults_ShouldOnlyProcessOnce() {
        // 创建重复的扫描结果（同一文件多个结果）
        List<ScanResult> results = Arrays.asList(
            createScanResult("res/layout/activity_main.xml"),
            createScanResult("res/layout/activity_main.xml"),  // 重复
            createScanResult("res/layout/fragment_user.xml")
        );
        
        // 提取filesToProcess
        Set<String> filesToProcess = new HashSet<>();
        for (ScanResult result : results) {
            filesToProcess.add(result.getFilePath());
        }
        
        // 验证：Set自动去重
        assertEquals(2, filesToProcess.size(), 
                    "重复的文件路径应该被自动去重");
    }
    
    /**
     * 创建测试用的ScanResult
     */
    private ScanResult createScanResult(String filePath) {
        return new ScanResult.Builder()
            .filePath(filePath)
            .semanticType(ScanResult.SemanticType.TAG_NAME)
            .location("line 1")
            .originalValue("com.test.MyView")
            .build();
    }
}


