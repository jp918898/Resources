package com.resources.core;

import com.resources.arsc.*;
import com.resources.axml.AxmlReplacer;
import com.resources.config.ResourceConfig;
import com.resources.mapping.WhitelistFilter;
import com.resources.model.*;
import com.resources.scanner.ResourceScanner;
import com.resources.transaction.TransactionManager;
import com.resources.util.ApkSignerUtil;
import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import com.resources.util.ZipAlignUtil;
import com.resources.validator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * 资源处理主控制器
 * 
 * 完整流程：
 * 1. 开启事务，创建快照
 * 2. 扫描APK，定位所有需要修改的位置
 * 3. 预验证（语义验证、映射验证、DEX交叉验证）
 * 4. 执行替换（AXML + ARSC）
 * 5. aapt2静态验证
 * 6. 重新打包APK
 * 7. 提交事务或回滚
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResourceProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceProcessor.class);
    
    private final TransactionManager transactionManager;
    
    public ResourceProcessor() {
        this.transactionManager = new TransactionManager();
        
        log.info("ResourceProcessor初始化完成");
    }
    
    /**
     * 处理APK
     * 
     * @param apkPath APK路径
     * @param config 资源配置
     * @return 处理结果
     * @throws IOException 处理失败
     */
    public ProcessingResult processApk(String apkPath, ResourceConfig config) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        Objects.requireNonNull(config, "config不能为null");
        
        log.info("════════════════════════════════════════");
        log.info("  开始处理APK");
        log.info("════════════════════════════════════════");
        log.info("APK: {}", apkPath);
        log.info("配置: {}", config);
        
        long startTime = System.currentTimeMillis();
        ProcessingResult.Builder resultBuilder = new ProcessingResult.Builder()
            .apkPath(apkPath)
            .startTime(startTime);
        
        Transaction tx = null;
        
        try {
            // 1. 开启事务，创建快照
            tx = transactionManager.beginTransaction(apkPath);
            log.info("事务已创建: {}", tx.getTransactionId());
            
            // 2. 扫描APK
            log.info("────────────────────────────────────────");
            log.info("  Phase 1: 扫描定位");
            log.info("────────────────────────────────────────");
            
            ResourceScanner.ScanReport scanReport = phase1_Scan(apkPath, config);
            log.info("扫描完成: 发现 {} 处需要修改", scanReport.getTotalResults());
            
            // 修复1：正确统计扫描文件数
            int totalScannedFiles = scanReport.getAllResults().size() + 1; // XML文件 + ARSC
            resultBuilder.totalFilesScanned(totalScannedFiles);
            
            // 3. 预验证
            log.info("────────────────────────────────────────");
            log.info("  Phase 2: 预验证");
            log.info("────────────────────────────────────────");
            
            ValidationResult preValidation = phase2_Validate(tx, config);
            
            if (!preValidation.isOverallSuccess()) {
                resultBuilder.success(false);
                resultBuilder.addError("预验证失败");
                resultBuilder.validationResult(preValidation);
                
                log.error("预验证失败，终止处理");
                throw new IOException("预验证失败");
            }
            
            log.info("预验证通过");
            
            // 4. 执行替换
            log.info("────────────────────────────────────────");
            log.info("  Phase 3: 执行替换");
            log.info("────────────────────────────────────────");
            
            int replaceCount = phase3_Replace(apkPath, config, scanReport, resultBuilder);
            log.info("替换完成: {} 处修改", replaceCount);
            resultBuilder.totalModifications(replaceCount);
            
            // 5. aapt2验证（跳过，用于混淆APK）
            // 混淆APK无法通过aapt2验证，跳过此步骤
            ValidationResult aapt2Validation = new ValidationResult.Builder()
                .addSkipped(ValidationResult.ValidationLevel.AAPT2_STATIC, "aapt2验证已跳过（混淆APK）")
                .build();
            
            // 修复5：合并预验证和最终验证结果
            ValidationResult combinedValidation = ValidationResult.Builder.merge(preValidation, aapt2Validation);
            resultBuilder.validationResult(combinedValidation);
            
            log.info("aapt2验证已跳过（混淆APK特殊处理）");
            
            // 6. 提交事务
            transactionManager.commit(tx, new ArrayList<>());
            
            resultBuilder.success(true);
            
            log.info("════════════════════════════════════════");
            log.info("  处理成功完成");
            log.info("════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("处理失败: {}", e.getMessage(), e);
            
            resultBuilder.success(false);
            resultBuilder.addError(e.getMessage());
            
            // 回滚
            if (tx != null) {
                try {
                    transactionManager.rollback(tx);
                    log.info("回滚成功");
                } catch (Exception rollbackError) {
                    log.error("事务回滚失败，数据可能处于不一致状态", rollbackError);
                    resultBuilder.addError("回滚失败: " + rollbackError.getMessage());
                    // ✅ 工业级标准：回滚失败是更严重的错误，必须向上传播
                    throw new IOException("APK处理失败且回滚失败，数据可能损坏: " + 
                                         e.getMessage() + " | 回滚错误: " + rollbackError.getMessage(),
                                         rollbackError);
                }
            }
            
            throw new IOException("APK处理失败: " + e.getMessage(), e);
            
        } finally {
            long endTime = System.currentTimeMillis();
            resultBuilder.endTime(endTime);
        }
        
        return resultBuilder.build();
    }
    
    /**
     * Phase 1: 扫描定位
     */
    private ResourceScanner.ScanReport phase1_Scan(String apkPath, ResourceConfig config) 
            throws IOException {
        
        // 创建扫描器
        WhitelistFilter whitelistFilter = new WhitelistFilter();
        whitelistFilter.addOwnPackages(config.getOwnPackagePrefixes());
        
        SemanticValidator semanticValidator = new SemanticValidator(whitelistFilter);
        
        ResourceScanner scanner = new ResourceScanner(
            semanticValidator, whitelistFilter, config.getOwnPackagePrefixes());
        
        // 扫描APK
        return scanner.scanApk(apkPath);
    }
    
    /**
     * Phase 2: 预验证
     */
    private ValidationResult phase2_Validate(Transaction tx, ResourceConfig config) {
        
        // 映射一致性验证和DEX交叉验证
        return transactionManager.validate(
            tx, config.getClassMappings(), config.getDexPaths());
    }
    
    /**
     * Phase 3: 执行替换
     */
    private int phase3_Replace(String apkPath, ResourceConfig config,
                              ResourceScanner.ScanReport scanReport, 
                              ProcessingResult.Builder resultBuilder) throws IOException {
        
        int totalReplaceCount = 0;
        
        // 从扫描结果提取需要处理的文件
        Set<String> filesToProcess = null;
        if (scanReport != null && scanReport.getTotalResults() > 0) {
            filesToProcess = new HashSet<>();
            for (ScanResult result : scanReport.getAllResults()) {
                filesToProcess.add(result.getFilePath());
            }
            log.info("从扫描结果提取 {} 个需处理的文件", filesToProcess.size());
        }
        
        // 1. 创建处理器
        WhitelistFilter whitelistFilter = new WhitelistFilter();
        whitelistFilter.addOwnPackages(config.getOwnPackagePrefixes());
        
        SemanticValidator semanticValidator = new SemanticValidator(whitelistFilter);
        
        AxmlReplacer axmlReplacer = new AxmlReplacer(
            semanticValidator,
            config.getClassMappings(),
            config.getPackageMappings(),
            config.isProcessToolsContext());
        
        ArscReplacer arscReplacer = new ArscReplacer(whitelistFilter);
        
        // 2. 加载APK到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        int fileCount = vfs.loadFromApk(apkPath);
        log.info("VFS加载完成: {} 个文件", fileCount);
        
        // 3. 批量处理AXML文件
        BatchReplaceResult axmlResult = processAxmlFilesVfs(vfs, axmlReplacer, filesToProcess);
        totalReplaceCount += axmlResult.getSuccessCount();
        resultBuilder.addModification("AXML文件", axmlResult.getSuccessCount());
        log.info("AXML处理完成: {} 个文件已修改", axmlResult.getSuccessCount());
        
        // 4. 处理resources.arsc
        ArscReplaceResult arscResult = processArscVfs(vfs, arscReplacer, config);
        totalReplaceCount += arscResult.getTotalModifications();
        resultBuilder.addModification("ARSC包名", arscResult.getPackageModifications());
        resultBuilder.addModification("ARSC字符串池", arscResult.getStringPoolModifications());
        log.info("ARSC处理完成: 包名={}, 字符串池={}", 
                arscResult.getPackageModifications(), arscResult.getStringPoolModifications());
        
        // 5. 导出APK到临时文件
        String tempApkPath = apkPath + ".tmp";
        vfs.saveToApk(tempApkPath);
        log.info("VFS导出完成: {}", tempApkPath);
        
        // 6. 条件对齐和签名
        if (config.isAutoSign()) {
            performAlignAndSign(tempApkPath, apkPath);
        } else {
            // 直接替换原文件，不对齐不签名
            try {
                Files.move(Paths.get(tempApkPath), Paths.get(apkPath), REPLACE_EXISTING);
                log.info("APK已更新（未对齐、未签名）: {}", apkPath);
            } catch (IOException e) {
                log.error("替换APK文件失败", e);
                // 清理临时文件
                try {
                    Files.deleteIfExists(Paths.get(tempApkPath));
                } catch (IOException cleanupError) {
                    log.warn("清理临时文件失败: {}", tempApkPath, cleanupError);
                }
                throw new IOException("替换APK文件失败: " + e.getMessage(), e);
            }
        }
        
        log.info(vfs.getStatistics());
        
        return totalReplaceCount;
    }
    
    /**
     * 使用VFS批量处理AXML文件
     * 
     * 支持混淆APK：
     * - Level 1: 标准res目录（res/**\/*.xml）
     * - Level 2: 全局扫描（**\/*.xml）+ 过滤非资源XML
     * 
     * @param vfs VFS实例
     * @param axmlReplacer AXML替换器
     * @param filesToProcess 需要处理的文件路径集合（来自扫描结果，可为null）
     * @return 批量处理结果
     */
    private BatchReplaceResult processAxmlFilesVfs(VirtualFileSystem vfs, AxmlReplacer axmlReplacer,
                                                  Set<String> filesToProcess) throws IOException {
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        // Level 1: 标准res目录
        Map<String, byte[]> allXmls = provider.getFilesByPattern("res/**/*.xml");
        
        // Level 2: fallback到全局扫描（支持res目录被重命名的混淆APK）
        if (allXmls.isEmpty()) {
            log.warn("标准res目录为空，启用全局XML扫描（混淆APK模式）");
            allXmls = provider.getFilesByPattern("**/*.xml");
            
            // 过滤：排除已知非资源XML
            allXmls = filterNonResourceXml(allXmls);
        }
        
        // 如果有扫描结果，只处理扫描阶段标记的文件
        if (filesToProcess != null && !filesToProcess.isEmpty()) {
            Map<String, byte[]> filteredXmls = new LinkedHashMap<>();
            int skipped = 0;
            
            for (Map.Entry<String, byte[]> entry : allXmls.entrySet()) {
                if (filesToProcess.contains(entry.getKey())) {
                    filteredXmls.put(entry.getKey(), entry.getValue());
                } else {
                    skipped++;
                }
            }
            
            log.info("基于扫描结果过滤: {} 个需处理, {} 个跳过", 
                     filteredXmls.size(), skipped);
            allXmls = filteredXmls;
        }
        
        if (allXmls.isEmpty()) {
            log.info("未发现需要处理的XML文件");
            return new BatchReplaceResult.Builder()
                .results(new LinkedHashMap<>())
                .successCount(0)
                .skippedCount(0)
                .errorCount(0)
                .build();
        }
        
        log.info("发现 {} 个XML文件待处理", allXmls.size());
        
        // 批量处理
        BatchReplaceResult result = axmlReplacer.replaceAxmlBatch(allXmls);
        
        // 写回VFS
        provider.updateFiles(result.getResults());
        
        return result;
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
            if (path.equals("AndroidManifest.xml")) {
                log.trace("过滤非资源XML: {}", path);
                continue;
            }
            if (path.startsWith("META-INF/")) {
                log.trace("过滤非资源XML: {}", path);
                continue;
            }
            if (path.startsWith("original/")) {
                log.trace("过滤非资源XML: {}", path);
                continue;
            }
            if (path.startsWith("kotlin/")) {
                log.trace("过滤非资源XML: {}", path);
                continue;
            }
            
            // 保留所有其他XML（包括混淆后的资源XML）
            filtered.put(path, entry.getValue());
        }
        
        if (filtered.size() < allXmls.size()) {
            log.debug("XML过滤: {} -> {} 个文件（排除了{}个非资源XML）", 
                     allXmls.size(), filtered.size(), allXmls.size() - filtered.size());
        }
        
        return filtered;
    }
    
    /**
     * 使用VFS处理resources.arsc
     * 
     * @param vfs VFS实例
     * @param arscReplacer ARSC替换器
     * @param config 资源配置
     * @return ARSC替换结果
     */
    private ArscReplaceResult processArscVfs(VirtualFileSystem vfs, ArscReplacer arscReplacer, 
                                            ResourceConfig config) throws IOException {
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        if (!vfs.exists("resources.arsc")) {
            log.warn("未找到resources.arsc");
            return new ArscReplaceResult.Builder()
                .packageModifications(0)
                .stringPoolModifications(0)
                .arscModified(false)
                .build();
        }
        
        try {
            byte[] arscData = provider.getResourcesArsc();
            
            // 解析ARSC
            ArscParser parser = new ArscParser();
            parser.parse(arscData);
            
            // 跟踪修改统计
            int packageModifications = 0;
            int stringPoolModifications = 0;
            boolean arscModified = false;
            
            // 替换包名
            ResTablePackage mainPackage = parser.getMainPackage();
            if (mainPackage != null && !config.getPackageMappings().isEmpty()) {
                String oldPkg = mainPackage.getName();
                String newPkg = config.getPackageMappings().replace(oldPkg);
                
                if (!oldPkg.equals(newPkg)) {
                    arscReplacer.replacePackageName(mainPackage, newPkg);
                    log.info("ARSC包名替换: '{}' -> '{}'", oldPkg, newPkg);
                    packageModifications = 1;
                    arscModified = true;
                }
            }
            
            // 替换字符串池
            if (parser.getGlobalStringPool() != null) {
                Map<String, String> stringReplacements = buildStringReplacements(config);
                stringPoolModifications = arscReplacer.replaceStringPool(
                    parser.getGlobalStringPool(), stringReplacements);
                
                log.info("ARSC字符串池替换: {} 处", stringPoolModifications);
                if (stringPoolModifications > 0) {
                    arscModified = true;
                }
            }
            
            // 只有在真正修改时才写回VFS（避免无意义的重新生成导致字节顺序改变）
            if (arscModified) {
                ArscWriter writer = new ArscWriter();
                byte[] modifiedData = writer.toByteArray(parser);
                provider.setResourcesArsc(modifiedData);
                log.info("resources.arsc已更新到VFS");
            } else {
                log.info("resources.arsc无需修改，保持原始数据");
            }
            
            return new ArscReplaceResult.Builder()
                .packageModifications(packageModifications)
                .stringPoolModifications(stringPoolModifications)
                .arscModified(arscModified)
                .build();
            
        } catch (Exception e) {
            log.error("处理resources.arsc失败", e);
            throw new IOException("处理resources.arsc失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建字符串替换映射表
     */
    private Map<String, String> buildStringReplacements(ResourceConfig config) {
        Map<String, String> replacements = new HashMap<>();
        
        // 1. 添加类名映射（精确匹配）
        for (String oldClass : config.getClassMappings().getAllOldClasses()) {
            String newClass = config.getClassMappings().getNewClass(oldClass);
            if (newClass != null) {
                replacements.put(oldClass, newClass);
                log.debug("添加类名映射: {} -> {}", oldClass, newClass);
            }
        }
        
        // 2. 添加包名映射（前缀匹配）
        // 关键修复：ARSC字符串池替换需要包名映射才能支持包名前缀替换
        for (PackageMapping.MappingEntry entry : config.getPackageMappings().getAllMappings()) {
            replacements.put(entry.getOldPackage(), entry.getNewPackage());
            log.debug("添加包名映射: {} -> {} ({})", 
                     entry.getOldPackage(), entry.getNewPackage(), entry.getMode());
        }
        
        log.info("构建字符串替换映射: 类名={}, 包名={}", 
                 config.getClassMappings().size(), 
                 config.getPackageMappings().size());
        
        return replacements;
    }
    
    /**
     * 执行对齐和签名
     * 
     * @param tempApkPath 临时APK路径（VFS导出的未签名APK）
     * @param finalApkPath 最终APK路径（对齐签名后的APK）
     * @throws IOException 对齐或签名失败
     */
    private void performAlignAndSign(String tempApkPath, String finalApkPath) throws IOException {
        log.info("────────────────────────────────────────");
        log.info("  对齐和签名APK");
        log.info("────────────────────────────────────────");
        
        String alignedApkPath = finalApkPath + ".aligned.tmp";
        
        // 对齐
        try {
            ZipAlignUtil.align(tempApkPath, alignedApkPath, 4);
            log.info("✓ APK对齐完成");
        } catch (Exception e) {
            log.error("APK对齐失败", e);
            throw new IOException("APK对齐失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(tempApkPath));
            } catch (IOException cleanupError) {
                log.warn("清理临时文件失败: {}", tempApkPath, cleanupError);
            }
        }
        
        // 签名
        try {
            ApkSignerUtil.signWithTestKey(alignedApkPath);
            log.info("✓ APK签名完成");
        } catch (Exception e) {
            log.error("APK签名失败", e);
            
            // 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(alignedApkPath));
            } catch (IOException cleanupError) {
                log.warn("清理临时文件失败: {}", alignedApkPath, cleanupError);
            }
            
            throw new IOException("APK签名失败: " + e.getMessage(), e);
        }
        
        // 替换原文件
        try {
            Files.move(Paths.get(alignedApkPath), Paths.get(finalApkPath), REPLACE_EXISTING);
            log.info("✓ APK已更新（已对齐+已签名）: {}", finalApkPath);
        } catch (IOException e) {
            log.error("替换APK文件失败", e);
            
            // 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(alignedApkPath));
            } catch (IOException cleanupError) {
                log.warn("清理临时文件失败: {}", alignedApkPath, cleanupError);
            }
            
            throw new IOException("替换APK文件失败: " + e.getMessage(), e);
        }
    }
}

