package com.resources.validator;

import com.resources.model.ClassMapping;
import com.resources.model.ValidationResult;
import com.resources.util.DexUtils;
import com.resources.util.DexClassCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DEX交叉验证器
 * 
 * 功能：
 * - 加载DEX文件中的所有类（带缓存）
 * - 验证映射表中的新类是否在DEX中存在
 * - 查找缺失的类
 * 
 * 线程安全
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class DexCrossValidator {
    
    private static final Logger log = LoggerFactory.getLogger(DexCrossValidator.class);
    
    // DEX类加载缓存（共享实例）
    private final DexClassCache dexCache;
    
    /**
     * 默认构造函数（使用默认缓存）
     */
    public DexCrossValidator() {
        this(new DexClassCache());
    }
    
    /**
     * 指定缓存的构造函数
     * 
     * @param dexCache DEX类缓存实例
     */
    public DexCrossValidator(DexClassCache dexCache) {
        this.dexCache = Objects.requireNonNull(dexCache, "dexCache不能为null");
    }
    
    /**
     * 验证类映射
     * 
     * @param mapping 类名映射
     * @param dexPaths DEX文件路径列表
     * @return 验证结果
     */
    public ValidationResult validate(ClassMapping mapping, List<String> dexPaths) {
        Objects.requireNonNull(mapping, "mapping不能为null");
        Objects.requireNonNull(dexPaths, "dexPaths不能为null");
        
        log.info("DEX交叉验证: {} 个映射, {} 个DEX文件", 
                mapping.size(), dexPaths.size());
        
        ValidationResult.Builder builder = new ValidationResult.Builder();
        
        // 1. 加载所有DEX类（使用缓存）
        Set<String> dexClasses = new HashSet<>();
        for (String dexPath : dexPaths) {
            try {
                Set<String> classes = dexCache.getDexClasses(dexPath);
                dexClasses.addAll(classes);
                
                log.debug("DEX文件 {} 包含 {} 个类", dexPath, classes.size());
                
            } catch (Exception e) {
                log.warn("加载DEX失败: {}", dexPath, e);
                builder.addWarning(ValidationResult.ValidationLevel.DEX_CROSS,
                                 "DEX加载失败",
                                 dexPath + ": " + e.getMessage());
            }
        }
        
        log.info("总共加载 {} 个类 ({})", dexClasses.size(), dexCache.getStatistics());
        
        // 2. 验证所有新类是否存在
        List<String> missingClasses = findMissingClasses(mapping, dexClasses);
        
        if (missingClasses.isEmpty()) {
            builder.addPassed(ValidationResult.ValidationLevel.DEX_CROSS,
                            String.format("所有 %d 个新类都在DEX中存在", 
                                        mapping.getAllNewClasses().size()));
        } else {
            StringBuilder details = new StringBuilder();
            details.append("缺失 ").append(missingClasses.size()).append(" 个类:\n");
            for (String missing : missingClasses) {
                details.append("  - ").append(missing).append("\n");
            }
            
            builder.addFailed(ValidationResult.ValidationLevel.DEX_CROSS,
                            "部分新类在DEX中不存在",
                            details.toString());
        }
        
        return builder.build();
    }
    
    /**
     * 加载DEX文件中的所有类
     * 
     * @param dexPath DEX文件路径
     * @return 类名集合
     * @throws Exception 加载失败
     */
    public Set<String> loadDexClasses(String dexPath) throws Exception {
        // 使用统一的DexUtils工具类，避免代码重复
        return DexUtils.loadDexClasses(dexPath);
    }
    
    /**
     * 查找缺失的类
     * 
     * @param mapping 类名映射
     * @param dexClasses DEX中的所有类
     * @return 缺失的类列表
     */
    public List<String> findMissingClasses(ClassMapping mapping, Set<String> dexClasses) {
        Objects.requireNonNull(mapping, "mapping不能为null");
        Objects.requireNonNull(dexClasses, "dexClasses不能为null");
        
        List<String> missing = new ArrayList<>();
        
        for (String newClass : mapping.getAllNewClasses()) {
            if (!dexClasses.contains(newClass)) {
                missing.add(newClass);
                log.debug("缺失类: {}", newClass);
            }
        }
        
        return missing;
    }
    
    /**
     * 检查类是否存在于DEX中
     * 
     * @param className 类名
     * @param dexPaths DEX文件路径列表
     * @return true=存在
     */
    public boolean classExists(String className, List<String> dexPaths) {
        Objects.requireNonNull(className, "className不能为null");
        Objects.requireNonNull(dexPaths, "dexPaths不能为null");
        
        for (String dexPath : dexPaths) {
            try {
                Set<String> classes = loadDexClasses(dexPath);
                if (classes.contains(className)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("检查类存在性失败: {} in {}", className, dexPath);
            }
        }
        
        return false;
    }
}

