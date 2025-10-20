package com.resources.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 白名单过滤器 - 区分自有包vs系统/三方包
 * 
 * 功能：
 * - 定义系统包前缀（android., androidx., com.google.等）
 * - 定义自有包前缀
 * - 判断类名/包名是否应该被替换
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class WhitelistFilter {
    
    private static final Logger log = LoggerFactory.getLogger(WhitelistFilter.class);
    
    // 系统包前缀（不应修改）
    private static final Set<String> SYSTEM_PREFIXES = Set.of(
        "android.",
        "androidx.",
        "com.google.",
        "com.android.",
        "kotlin.",
        "kotlinx.",
        "java.",
        "javax.",
        "dalvik.",
        "org.apache.",
        "org.json.",
        "org.xml.",
        "org.w3c."
    );
    
    // 常见第三方库前缀（不应修改）
    private static final Set<String> COMMON_THIRD_PARTY_PREFIXES = Set.of(
        "com.squareup.",
        "com.facebook.",
        "com.tencent.",
        "com.alibaba.",
        "com.alipay.",
        "com.baidu.",
        "okhttp3.",
        "retrofit2.",
        "io.reactivex.",
        "org.greenrobot.",
        "com.github.",
        "org.jetbrains."
    );
    
    // 自有包前缀（应该修改）
    private final Set<String> ownPackagePrefixes;
    
    // 额外的排除前缀（用户自定义）
    private final Set<String> excludePrefixes;
    
    public WhitelistFilter() {
        this.ownPackagePrefixes = ConcurrentHashMap.newKeySet();
        this.excludePrefixes = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * 添加自有包前缀
     * 
     * @param prefix 包前缀（如"com.example."）
     */
    public void addOwnPackage(String prefix) {
        Objects.requireNonNull(prefix, "prefix不能为null");
        
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        
        ownPackagePrefixes.add(prefix);
        log.debug("添加自有包前缀: '{}'", prefix);
    }
    
    /**
     * 批量添加自有包前缀
     * 
     * @param prefixes 包前缀列表
     */
    public void addOwnPackages(Collection<String> prefixes) {
        Objects.requireNonNull(prefixes, "prefixes不能为null");
        
        for (String prefix : prefixes) {
            addOwnPackage(prefix);
        }
        
        log.info("添加 {} 个自有包前缀", prefixes.size());
    }
    
    /**
     * 添加额外的排除前缀（用户不想修改的第三方库）
     * 
     * @param prefix 排除前缀
     */
    public void addExcludePrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix不能为null");
        
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        
        excludePrefixes.add(prefix);
        log.debug("添加排除前缀: '{}'", prefix);
    }
    
    /**
     * 判断类名/包名是否应该被替换
     * 
     * 规则：
     * 1. 系统包 -> 不替换
     * 2. 常见第三方库 -> 不替换
     * 3. 用户排除的前缀 -> 不替换
     * 4. 自有包 -> 替换
     * 5. 其他 -> 不替换（默认保守策略）
     * 
     * @param className 完整类名或包名
     * @return true=应该替换, false=不应替换
     */
    public boolean shouldReplace(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        // 1. 系统包 - 绝对不改
        for (String sysPrefix : SYSTEM_PREFIXES) {
            if (className.startsWith(sysPrefix)) {
                log.trace("系统包，不替换: '{}'", className);
                return false;
            }
        }
        
        // 2. 常见第三方库 - 绝对不改
        for (String thirdPartyPrefix : COMMON_THIRD_PARTY_PREFIXES) {
            if (className.startsWith(thirdPartyPrefix)) {
                log.trace("第三方库，不替换: '{}'", className);
                return false;
            }
        }
        
        // 3. 用户排除的前缀 - 不改
        for (String excludePrefix : excludePrefixes) {
            if (className.startsWith(excludePrefix)) {
                log.trace("用户排除，不替换: '{}'", className);
                return false;
            }
        }
        
        // 4. 自有包 - 应该改
        for (String ownPrefix : ownPackagePrefixes) {
            // 修复：支持纯包名匹配
            // "com.example".startsWith("com.example.") = false (错误)
            // 应该同时检查完全相等的情况
            if (className.startsWith(ownPrefix)) {
                log.trace("自有包，应替换: '{}'", className);
                return true;
            }
            
            // 检查是否为纯包名（去掉尾部点号后的精确匹配）
            if (ownPrefix.endsWith(".")) {
                String prefixWithoutDot = ownPrefix.substring(0, ownPrefix.length() - 1);
                if (className.equals(prefixWithoutDot)) {
                    log.trace("自有包（纯包名），应替换: '{}'", className);
                    return true;
                }
            }
        }
        
        // 5. 未知包 - 默认不改（保守策略）
        log.trace("未知包，默认不替换: '{}'", className);
        return false;
    }
    
    /**
     * 判断是否为系统包
     * 
     * @param className 类名
     * @return true=系统包
     */
    public boolean isSystemPackage(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        for (String sysPrefix : SYSTEM_PREFIXES) {
            if (className.startsWith(sysPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否为常见第三方库
     * 
     * @param className 类名
     * @return true=第三方库
     */
    public boolean isCommonThirdParty(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        for (String thirdPartyPrefix : COMMON_THIRD_PARTY_PREFIXES) {
            if (className.startsWith(thirdPartyPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否为自有包
     * 
     * @param className 类名
     * @return true=自有包
     */
    public boolean isOwnPackage(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        
        for (String ownPrefix : ownPackagePrefixes) {
            if (className.startsWith(ownPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取自有包前缀列表
     */
    public Set<String> getOwnPackagePrefixes() {
        return new HashSet<>(ownPackagePrefixes);
    }
    
    /**
     * 获取排除前缀列表
     */
    public Set<String> getExcludePrefixes() {
        return new HashSet<>(excludePrefixes);
    }
    
    /**
     * 清空自有包前缀
     */
    public void clearOwnPackages() {
        ownPackagePrefixes.clear();
        log.debug("清空自有包前缀");
    }
    
    /**
     * 清空排除前缀
     */
    public void clearExcludePrefixes() {
        excludePrefixes.clear();
        log.debug("清空排除前缀");
    }
    
    @Override
    public String toString() {
        return String.format("WhitelistFilter{ownPackages=%d, excludes=%d}", 
                           ownPackagePrefixes.size(), excludePrefixes.size());
    }
}

