package com.resources.arsc;

import com.resources.mapping.WhitelistFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ARSC替换引擎 - 核心替换逻辑
 * 
 * 功能：
 * - 替换ResTable_package.name
 * - 替换String Pool中的类名/包名
 * - 语义验证（区分类名vs普通文案）
 * - 白名单过滤（只改自有类/包）
 * - 完整性验证
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscReplacer {
    
    private static final Logger log = LoggerFactory.getLogger(ArscReplacer.class);
    
    private final WhitelistFilter whitelistFilter;
    
    public ArscReplacer(WhitelistFilter whitelistFilter) {
        this.whitelistFilter = Objects.requireNonNull(whitelistFilter, 
                                                      "whitelistFilter不能为null");
    }
    
    /**
     * 替换资源包名称
     * 
     * @param pkg 资源包
     * @param newName 新包名
     * @throws IllegalArgumentException 包名无效
     */
    public void replacePackageName(ResTablePackage pkg, String newName) {
        Objects.requireNonNull(pkg, "pkg不能为null");
        Objects.requireNonNull(newName, "newName不能为null");
        
        String oldName = pkg.getName();
        
        // 验证packageId不变
        if (pkg.getId() != 0x7f && pkg.getId() != 0x01) {
            log.warn("非标准packageId: 0x{}", Integer.toHexString(pkg.getId()));
        }
        
        // 执行替换
        pkg.setName(newName);
        
        log.info("资源包名称替换: '{}' -> '{}' (packageId=0x{})", 
                oldName, newName, Integer.toHexString(pkg.getId()));
    }
    
    /**
     * 替换字符串池中的类名/包名
     * 
     * @param pool 字符串池
     * @param replacements 替换映射表（Old -> New）
     * @return 替换数量
     */
    public int replaceStringPool(ResStringPool pool, Map<String, String> replacements) {
        Objects.requireNonNull(pool, "pool不能为null");
        Objects.requireNonNull(replacements, "replacements不能为null");
        
        if (replacements.isEmpty()) {
            log.debug("替换映射表为空，跳过");
            return 0;
        }
        
        // ✅ 性能优化：预排序，只执行一次（移到循环外）
        // 按前缀长度倒序排列，优先匹配最长前缀（避免com.example.ui被com.example误匹配）
        List<Map.Entry<String, String>> sortedReplacements = new ArrayList<>(replacements.entrySet());
        sortedReplacements.sort((a, b) -> Integer.compare(
            b.getKey().length(), a.getKey().length()));
        
        log.debug("替换映射表已排序: {} 个映射", sortedReplacements.size());
        
        int replaceCount = 0;
        
        for (int i = 0; i < pool.getStringCount(); i++) {
            String original = pool.getString(i);
            
            // 1. 语义验证：确认是类名/包名
            if (!looksLikeClassOrPackage(original)) {
                continue;
            }
            
            // 2. 白名单过滤：只处理自有类/包
            if (!whitelistFilter.shouldReplace(original)) {
                log.trace("跳过系统/三方包: '{}'", original);
                continue;
            }
            
            // 3. 查找精确匹配
            String replacement = replacements.get(original);
            if (replacement != null) {
                pool.setString(i, replacement);
                replaceCount++;
                
                log.info("替换字符串[{}]: '{}' -> '{}'", i, original, replacement);
                continue;
            }
            
            // 4. 前缀匹配（使用预排序的列表）
            for (Map.Entry<String, String> entry : sortedReplacements) {
                String oldPrefix = entry.getKey();
                String newPrefix = entry.getValue();
                
                if (original.startsWith(oldPrefix)) {
                    // 确保是完整的包名分隔
                    int prefixLen = oldPrefix.length();
                    if (original.length() == prefixLen || 
                        original.charAt(prefixLen) == '.') {
                        
                        String newValue = newPrefix + original.substring(prefixLen);
                        pool.setString(i, newValue);
                        replaceCount++;
                        
                        log.info("替换字符串[{}]（前缀）: '{}' -> '{}'", 
                                i, original, newValue);
                        break;  // 找到匹配后立即退出
                    }
                }
            }
        }
        
        log.info("字符串池替换完成: {} 个字符串被替换", replaceCount);
        return replaceCount;
    }
    
    /**
     * 判断字符串是否看起来像类名或包名
     * 
     * 特征：
     * - 至少包含一个点号
     * - 符合Java标识符规范
     * - 首字母大写（类名）或小写（包名）
     * 
     * @param str 字符串
     * @return true=可能是类名/包名
     */
    private boolean looksLikeClassOrPackage(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // 1. 至少包含一个点号
        if (!str.contains(".")) {
            return false;
        }
        
        // 2. 不能是资源引用（@string/xxx, @drawable/xxx等）
        if (str.startsWith("@")) {
            return false;
        }
        
        // 3. 不能包含非Java标识符字符
        if (str.contains("/") || str.contains("-") || str.contains(":") || 
            str.contains(" ") || str.contains("=")) {
            return false;
        }
        
        // 4. 符合Java标识符规范
        String[] parts = str.split("\\.");
        if (parts.length < 2) {
            return false; // 至少两个部分（如com.example）
        }
        
        for (String part : parts) {
            if (part.isEmpty() || !isValidJavaIdentifier(part)) {
                return false;
            }
        }
        
        // 5. 不应该是纯数字包名（如1.2.3）
        boolean allDigits = true;
        for (String part : parts) {
            if (!part.matches("\\d+")) {
                allDigits = false;
                break;
            }
        }
        if (allDigits) {
            return false; // 可能是版本号
        }
        
        return true;
    }
    
    /**
     * 验证是否为有效的Java标识符
     */
    private boolean isValidJavaIdentifier(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 验证ARSC完整性（修改前后对比）
     * 
     * @param original 原始ARSC
     * @param modified 修改后的ARSC
     * @return true=完整性保持, false=结构被破坏
     */
    public boolean validateIntegrity(ArscParser original, ArscParser modified) {
        Objects.requireNonNull(original, "original不能为null");
        Objects.requireNonNull(modified, "modified不能为null");
        
        boolean valid = true;
        
        // 1. 包数量不变
        if (original.getPackageCount() != modified.getPackageCount()) {
            log.error("包数量改变: {} -> {}", 
                     original.getPackageCount(), modified.getPackageCount());
            valid = false;
        }
        
        // 2. packageId不变
        for (int i = 0; i < original.getPackages().size(); i++) {
            ResTablePackage origPkg = original.getPackages().get(i);
            ResTablePackage modPkg = modified.getPackages().get(i);
            
            if (origPkg.getId() != modPkg.getId()) {
                log.error("packageId改变: 0x{} -> 0x{}", 
                         Integer.toHexString(origPkg.getId()), 
                         Integer.toHexString(modPkg.getId()));
                valid = false;
            }
        }
        
        // 3. 字符串池数量可以不同（内容变化），但索引必须一一对应
        ResStringPool origPool = original.getGlobalStringPool();
        ResStringPool modPool = modified.getGlobalStringPool();
        
        if (origPool != null && modPool != null) {
            if (origPool.getStringCount() != modPool.getStringCount()) {
                log.error("全局字符串池数量改变: {} -> {}", 
                         origPool.getStringCount(), modPool.getStringCount());
                valid = false;
            }
        }
        
        // 4. type/entry数量不变（通过包的字符串池检查）
        for (int i = 0; i < original.getPackages().size(); i++) {
            ResTablePackage origPkg = original.getPackages().get(i);
            ResTablePackage modPkg = modified.getPackages().get(i);
            
            ResStringPool origTypes = origPkg.getTypeStrings();
            ResStringPool modTypes = modPkg.getTypeStrings();
            
            if (origTypes != null && modTypes != null) {
                if (origTypes.getStringCount() != modTypes.getStringCount()) {
                    log.error("类型字符串池数量改变: {} -> {}", 
                             origTypes.getStringCount(), modTypes.getStringCount());
                    valid = false;
                }
            }
            
            ResStringPool origKeys = origPkg.getKeyStrings();
            ResStringPool modKeys = modPkg.getKeyStrings();
            
            if (origKeys != null && modKeys != null) {
                if (origKeys.getStringCount() != modKeys.getStringCount()) {
                    log.error("资源名字符串池数量改变: {} -> {}", 
                             origKeys.getStringCount(), modKeys.getStringCount());
                    valid = false;
                }
            }
        }
        
        if (valid) {
            log.info("ARSC完整性验证通过");
        } else {
            log.error("ARSC完整性验证失败");
        }
        
        return valid;
    }
    
    /**
     * 生成替换报告
     * 
     * @param pool 字符串池
     * @param replacements 替换映射表
     * @return 报告内容
     */
    public String generateReplaceReport(ResStringPool pool, Map<String, String> replacements) {
        StringBuilder report = new StringBuilder();
        report.append("════════════════════════════════════════\n");
        report.append("  ARSC字符串替换报告\n");
        report.append("════════════════════════════════════════\n");
        
        int candidateCount = 0;
        int replaceCount = 0;
        
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            
            if (looksLikeClassOrPackage(str) && whitelistFilter.shouldReplace(str)) {
                candidateCount++;
                
                String replacement = replacements.get(str);
                if (replacement != null) {
                    report.append(String.format("[%d] '%s' -> '%s'\n", i, str, replacement));
                    replaceCount++;
                }
            }
        }
        
        report.append("────────────────────────────────────────\n");
        report.append(String.format("候选字符串: %d\n", candidateCount));
        report.append(String.format("实际替换: %d\n", replaceCount));
        report.append("════════════════════════════════════════\n");
        
        return report.toString();
    }
}

