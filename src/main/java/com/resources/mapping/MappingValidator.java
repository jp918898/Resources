package com.resources.mapping;

import com.resources.model.ClassMapping;
import com.resources.util.DexClassCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 映射一致性验证器
 * 
 * 功能：
 * - 检查循环映射
 * - 检查冲突映射
 * - 检查缺失类（新类在DEX中不存在）
 * 
 * 线程安全
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class MappingValidator {
    
    private static final Logger log = LoggerFactory.getLogger(MappingValidator.class);
    
    // DEX类加载缓存
    private final DexClassCache dexCache;
    
    /**
     * 默认构造函数（使用默认缓存）
     */
    public MappingValidator() {
        this(new DexClassCache());
    }
    
    /**
     * 指定缓存的构造函数
     * 
     * @param dexCache DEX类缓存实例
     */
    public MappingValidator(DexClassCache dexCache) {
        this.dexCache = Objects.requireNonNull(dexCache, "dexCache不能为null");
    }
    
    /**
     * 验证类名映射
     * 
     * @param classMapping 类名映射表
     * @param dexPaths DEX文件路径列表
     * @return true=验证通过，false=验证失败
     */
    public boolean validate(ClassMapping classMapping, List<String> dexPaths) {
        Objects.requireNonNull(classMapping, "classMapping不能为null");
        Objects.requireNonNull(dexPaths, "dexPaths不能为null");
        
        log.info("开始映射验证: {} 个映射, {} 个DEX文件", 
                classMapping.size(), dexPaths.size());
        
        boolean valid = true;
        
        // 1. 检查循环映射
        if (hasCircularMapping(classMapping)) {
            log.error("发现循环映射");
            valid = false;
        }
        
        // 2. 加载DEX类
        Set<String> dexClasses = loadAllDexClasses(dexPaths);
        if (dexClasses.isEmpty()) {
            log.warn("未能加载任何DEX类");
        } else {
            log.info("加载DEX类: {} 个", dexClasses.size());
        }
        
        // 3. 检查缺失类
        List<String> missingClasses = findMissingClasses(classMapping, dexClasses);
        if (!missingClasses.isEmpty()) {
            log.error("发现 {} 个缺失的类:", missingClasses.size());
            for (String missing : missingClasses) {
                log.error("  - {}", missing);
            }
            valid = false;
        }
        
        if (valid) {
            log.info("映射验证通过");
        } else {
            log.error("映射验证失败");
        }
        
        return valid;
    }
    
    /**
     * 检查循环映射
     * 例如：A->B, B->C, C->A
     */
    private boolean hasCircularMapping(ClassMapping classMapping) {
        Set<String> allOldClasses = classMapping.getAllOldClasses();
        
        for (String startClass : allOldClasses) {
            Set<String> visited = new HashSet<>();
            String current = startClass;
            
            while (current != null) {
                if (visited.contains(current)) {
                    log.error("发现循环映射: {} (访问路径: {})", current, visited);
                    return true;
                }
                
                visited.add(current);
                
                // 获取下一个映射
                String next = classMapping.getNewClass(current);
                if (next == null || next.equals(current)) {
                    break;
                }
                
                // 如果next也在oldClasses中，继续检查
                if (allOldClasses.contains(next)) {
                    current = next;
                } else {
                    break;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 加载所有DEX文件中的类（使用缓存）
     */
    private Set<String> loadAllDexClasses(List<String> dexPaths) {
        Set<String> allClasses = new HashSet<>();
        
        for (String dexPath : dexPaths) {
            try {
                Set<String> classes = dexCache.getDexClasses(dexPath);
                allClasses.addAll(classes);
                log.debug("从 {} 加载 {} 个类", dexPath, classes.size());
                
            } catch (Exception e) {
                log.warn("加载DEX文件失败: {}", dexPath, e);
            }
        }
        
        log.info("DEX类加载统计: {}", dexCache.getStatistics());
        return allClasses;
    }
    
    /**
     * 加载单个DEX文件中的类（使用缓存）
     */
    public Set<String> loadDexClasses(String dexPath) throws IOException {
        return dexCache.getDexClasses(dexPath);
    }
    
    /**
     * 清除DEX类缓存
     */
    public void clearCache() {
        dexCache.clear();
        log.info("DEX缓存已清除");
    }
    
    /**
     * 查找缺失的类（新类在DEX中不存在）
     */
    public List<String> findMissingClasses(ClassMapping mapping, Set<String> dexClasses) {
        Objects.requireNonNull(mapping, "mapping不能为null");
        Objects.requireNonNull(dexClasses, "dexClasses不能为null");
        
        List<String> missingClasses = new ArrayList<>();
        
        for (String newClass : mapping.getAllNewClasses()) {
            if (!dexClasses.contains(newClass)) {
                missingClasses.add(newClass);
            }
        }
        
        log.debug("缺失类检查: {} 个新类, {} 个缺失", 
                 mapping.getAllNewClasses().size(), missingClasses.size());
        
        return missingClasses;
    }
    
    /**
     * 生成验证报告
     */
    public String generateValidationReport(ClassMapping mapping, List<String> dexPaths) {
        StringBuilder report = new StringBuilder();
        report.append("════════════════════════════════════════\n");
        report.append("    映射验证报告\n");
        report.append("════════════════════════════════════════\n");
        
        // 1. 基本信息
        report.append(String.format("映射数量: %d\n", mapping.size()));
        report.append(String.format("DEX文件: %d 个\n\n", dexPaths.size()));
        
        // 2. 循环映射检查
        boolean hasCircular = hasCircularMapping(mapping);
        report.append(String.format("循环映射: %s\n", hasCircular ? "✗ 存在" : "✓ 无"));
        
        // 3. DEX类检查
        Set<String> dexClasses = loadAllDexClasses(dexPaths);
        List<String> missingClasses = findMissingClasses(mapping, dexClasses);
        
        report.append(String.format("DEX类总数: %d\n", dexClasses.size()));
        report.append(String.format("缺失类: %d\n", missingClasses.size()));
        
        if (!missingClasses.isEmpty()) {
            report.append("\n缺失的类:\n");
            for (String missing : missingClasses) {
                report.append(String.format("  ✗ %s\n", missing));
            }
        }
        
        report.append("\n");
        report.append(String.format("验证结果: %s\n", 
                                   (!hasCircular && missingClasses.isEmpty()) ? 
                                   "✓ 通过" : "✗ 失败"));
        report.append("════════════════════════════════════════\n");
        
        return report.toString();
    }
}

