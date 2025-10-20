package com.resources.scanner;

import com.resources.model.ScanResult;
import com.resources.validator.SemanticValidator;
import com.resources.mapping.WhitelistFilter;
import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 资源扫描器 - 批量扫描APK中的所有资源文件
 * 
 * 协调：
 * - AxmlScanner（扫描二进制XML）
 * - ArscScanner（扫描resources.arsc）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResourceScanner {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceScanner.class);
    
    private final AxmlScanner axmlScanner;
    private final ArscScanner arscScanner;
    
    public ResourceScanner(SemanticValidator semanticValidator,
                          WhitelistFilter whitelistFilter,
                          Set<String> ownPackagePrefixes) {
        
        Objects.requireNonNull(semanticValidator, "semanticValidator不能为null");
        Objects.requireNonNull(whitelistFilter, "whitelistFilter不能为null");
        Objects.requireNonNull(ownPackagePrefixes, "ownPackagePrefixes不能为null");
        
        this.axmlScanner = new AxmlScanner(semanticValidator, ownPackagePrefixes);
        this.arscScanner = new ArscScanner(whitelistFilter, ownPackagePrefixes);
        
        log.info("ResourceScanner初始化完成");
    }
    
    /**
     * 扫描APK文件
     * 
     * @param apkPath APK文件路径
     * @return 扫描报告
     * @throws IOException 扫描失败
     */
    public ScanReport scanApk(String apkPath) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.info("开始扫描APK: {}", apkPath);
        
        long startTime = System.currentTimeMillis();
        ScanReport.Builder reportBuilder = new ScanReport.Builder()
            .apkPath(apkPath)
            .startTime(startTime);
        
        try {
            // 1. 加载APK到VFS
            VirtualFileSystem vfs = new VirtualFileSystem();
            int fileCount = vfs.loadFromApk(apkPath);
            log.info("VFS加载完成: {} 个文件", fileCount);
            
            VfsResourceProvider provider = new VfsResourceProvider(vfs);
            
            List<ScanResult> allResults = new ArrayList<>();
            
            // 2. 扫描resources.arsc
            if (vfs.exists("resources.arsc")) {
                byte[] arscData = provider.getResourcesArsc();
                List<ScanResult> arscResults = arscScanner.scan(arscData);
                allResults.addAll(arscResults);
                reportBuilder.addArscResults(arscResults);
                
                log.info("ARSC扫描: 发现{}处", arscResults.size());
            } else {
                log.warn("未找到resources.arsc文件");
            }
            
            // 3. 批量扫描res/下所有XML（支持混淆后的APK）
            // 尝试标准路径
            Map<String, byte[]> layoutFiles = provider.getAllLayouts();
            Map<String, byte[]> menuFiles = provider.getAllMenus();
            Map<String, byte[]> navigationFiles = provider.getAllNavigations();
            Map<String, byte[]> xmlFiles = provider.getAllXmlConfigs();
            
            // 如果标准路径找不到文件，启用全局扫描（混淆后的APK）
            if (layoutFiles.isEmpty() && menuFiles.isEmpty() && 
                navigationFiles.isEmpty() && xmlFiles.isEmpty()) {
                log.warn("标准res目录为空，启用全局XML扫描（混淆APK模式）");
                
                // 尝试res根目录平铺
                Map<String, byte[]> allResXml = provider.getFilesByPattern("res/*.xml");
                
                // 如果res/*.xml也为空，尝试全局扫描
                if (allResXml.isEmpty()) {
                    log.warn("res/*.xml也为空，尝试全局扫描（res目录可能被重命名）");
                    allResXml = provider.getFilesByPattern("**/*.xml");
                    
                    // 过滤非资源XML
                    allResXml = filterNonResourceXml(allResXml);
                    log.info("全局扫描发现 {} 个XML文件", allResXml.size());
                }
                log.info("res/根目录扫描: {} 个XML文件", allResXml.size());
                
                // 全部当作layout处理（因为混淆后无法区分类型）
                layoutFiles = allResXml;
            }
            
            // 扫描layout
            List<ScanResult> layoutResults = axmlScanner.scanBatch(layoutFiles);
            allResults.addAll(layoutResults);
            reportBuilder.addLayoutResults(layoutResults);
            log.info("Layout扫描: {} 个文件, 发现{}处", layoutFiles.size(), layoutResults.size());
            
            // 扫描menu
            List<ScanResult> menuResults = axmlScanner.scanBatch(menuFiles);
            allResults.addAll(menuResults);
            reportBuilder.addMenuResults(menuResults);
            log.info("Menu扫描: {} 个文件, 发现{}处", menuFiles.size(), menuResults.size());
            
            // 扫描navigation
            List<ScanResult> navigationResults = axmlScanner.scanBatch(navigationFiles);
            allResults.addAll(navigationResults);
            reportBuilder.addNavigationResults(navigationResults);
            log.info("Navigation扫描: {} 个文件, 发现{}处", navigationFiles.size(), navigationResults.size());
            
            // 扫描xml config
            List<ScanResult> xmlResults = axmlScanner.scanBatch(xmlFiles);
            allResults.addAll(xmlResults);
            reportBuilder.addXmlResults(xmlResults);
            log.info("XML Config扫描: {} 个文件, 发现{}处", xmlFiles.size(), xmlResults.size());
            
            long endTime = System.currentTimeMillis();
            reportBuilder.endTime(endTime);
            reportBuilder.totalResults(allResults.size());
            
            ScanReport report = reportBuilder.build();
            
            log.info("APK扫描完成: 耗时{}ms, 发现{}处", 
                    report.getDurationMs(), allResults.size());
            log.info(vfs.getStatistics());
            
            return report;
            
        } catch (Exception e) {
            log.error("APK扫描失败: {}", apkPath, e);
            throw new IOException("APK扫描失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 扫描报告
     */
    public static class ScanReport {
        private final String apkPath;
        private final long startTime;
        private final long endTime;
        private final int totalResults;
        
        private final List<ScanResult> arscResults;
        private final List<ScanResult> layoutResults;
        private final List<ScanResult> menuResults;
        private final List<ScanResult> navigationResults;
        private final List<ScanResult> xmlResults;
        
        private ScanReport(Builder builder) {
            this.apkPath = builder.apkPath;
            this.startTime = builder.startTime;
            this.endTime = builder.endTime;
            this.totalResults = builder.totalResults;
            this.arscResults = new ArrayList<>(builder.arscResults);
            this.layoutResults = new ArrayList<>(builder.layoutResults);
            this.menuResults = new ArrayList<>(builder.menuResults);
            this.navigationResults = new ArrayList<>(builder.navigationResults);
            this.xmlResults = new ArrayList<>(builder.xmlResults);
        }
        
        // Getters
        public String getApkPath() { return apkPath; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDurationMs() { return endTime - startTime; }
        public int getTotalResults() { return totalResults; }
        public List<ScanResult> getArscResults() { return arscResults; }
        public List<ScanResult> getLayoutResults() { return layoutResults; }
        public List<ScanResult> getMenuResults() { return menuResults; }
        public List<ScanResult> getNavigationResults() { return navigationResults; }
        public List<ScanResult> getXmlResults() { return xmlResults; }
        
        /**
         * 获取所有结果
         */
        public List<ScanResult> getAllResults() {
            List<ScanResult> all = new ArrayList<>();
            all.addAll(arscResults);
            all.addAll(layoutResults);
            all.addAll(menuResults);
            all.addAll(navigationResults);
            all.addAll(xmlResults);
            return all;
        }
        
        /**
         * 生成摘要
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("════════════════════════════════════════\n");
            sb.append("    资源扫描报告\n");
            sb.append("════════════════════════════════════════\n");
            sb.append(String.format("APK: %s\n", apkPath));
            sb.append(String.format("耗时: %d ms (%.2f 秒)\n", 
                                   getDurationMs(), getDurationMs() / 1000.0));
            sb.append(String.format("发现总数: %d\n\n", totalResults));
            sb.append(String.format("  - resources.arsc: %d\n", arscResults.size()));
            sb.append(String.format("  - res/layout: %d\n", layoutResults.size()));
            sb.append(String.format("  - res/menu: %d\n", menuResults.size()));
            sb.append(String.format("  - res/navigation: %d\n", navigationResults.size()));
            sb.append(String.format("  - res/xml: %d\n", xmlResults.size()));
            sb.append("════════════════════════════════════════\n");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
        
        // Builder
        public static class Builder {
            private String apkPath;
            private long startTime;
            private long endTime;
            private int totalResults;
            private List<ScanResult> arscResults = new ArrayList<>();
            private List<ScanResult> layoutResults = new ArrayList<>();
            private List<ScanResult> menuResults = new ArrayList<>();
            private List<ScanResult> navigationResults = new ArrayList<>();
            private List<ScanResult> xmlResults = new ArrayList<>();
            
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
            
            public Builder totalResults(int totalResults) {
                this.totalResults = totalResults;
                return this;
            }
            
            public Builder addArscResults(List<ScanResult> results) {
                this.arscResults.addAll(results);
                return this;
            }
            
            public Builder addLayoutResults(List<ScanResult> results) {
                this.layoutResults.addAll(results);
                return this;
            }
            
            public Builder addMenuResults(List<ScanResult> results) {
                this.menuResults.addAll(results);
                return this;
            }
            
            public Builder addNavigationResults(List<ScanResult> results) {
                this.navigationResults.addAll(results);
                return this;
            }
            
            public Builder addXmlResults(List<ScanResult> results) {
                this.xmlResults.addAll(results);
                return this;
            }
            
            public ScanReport build() {
                return new ScanReport(this);
            }
        }
    }
    
    /**
     * 过滤非资源XML
     * 
     * 排除：
     * - AndroidManifest.xml
     * - META-INF/目录
     * - original/目录（签名相关）
     * - kotlin/目录（Kotlin元数据）
     * 
     * @param allXmls 所有XML文件
     * @return 过滤后的资源XML
     */
    private Map<String, byte[]> filterNonResourceXml(Map<String, byte[]> allXmls) {
        Map<String, byte[]> filtered = new LinkedHashMap<>();
        
        for (Map.Entry<String, byte[]> entry : allXmls.entrySet()) {
            String path = entry.getKey();
            
            // 排除已知非资源XML
            if (path.equals("AndroidManifest.xml")) continue;
            if (path.startsWith("META-INF/")) continue;
            if (path.startsWith("original/")) continue;
            if (path.startsWith("kotlin/")) continue;
            
            // 保留所有其他XML
            filtered.put(path, entry.getValue());
        }
        
        if (filtered.size() < allXmls.size()) {
            log.debug("过滤非资源XML: {} -> {} 个文件", allXmls.size(), filtered.size());
        }
        
        return filtered;
    }
}

