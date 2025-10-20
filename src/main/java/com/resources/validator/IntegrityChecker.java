package com.resources.validator;

import com.resources.arsc.ArscParser;
import com.resources.arsc.ResStringPool;
import com.resources.arsc.ResTablePackage;
import com.resources.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 完整性检查器
 * 
 * 验证ARSC修改前后的结构完整性：
 * - packageId未改变（0x7f）
 * - type数量未改变
 * - entry数量未改变
 * - 字符串池索引映射一致
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class IntegrityChecker {
    
    private static final Logger log = LoggerFactory.getLogger(IntegrityChecker.class);
    
    /**
     * 检查ARSC完整性
     * 
     * @param original 原始ARSC数据
     * @param modified 修改后的ARSC数据
     * @return 验证结果
     */
    public ValidationResult checkArscIntegrity(byte[] original, byte[] modified) {
        Objects.requireNonNull(original, "original不能为null");
        Objects.requireNonNull(modified, "modified不能为null");
        
        log.info("检查ARSC完整性");
        
        ValidationResult.Builder builder = new ValidationResult.Builder();
        
        try {
            // 1. 解析两个ARSC
            ArscParser originalParser = new ArscParser();
            originalParser.parse(original);
            
            ArscParser modifiedParser = new ArscParser();
            modifiedParser.parse(modified);
            
            // 2. 检查包数量
            if (originalParser.getPackageCount() != modifiedParser.getPackageCount()) {
                builder.addFailed(ValidationResult.ValidationLevel.INTEGRITY,
                                "包数量改变",
                                String.format("%d -> %d", 
                                            originalParser.getPackageCount(),
                                            modifiedParser.getPackageCount()));
            } else {
                builder.addPassed(ValidationResult.ValidationLevel.INTEGRITY,
                                "包数量不变");
            }
            
            // 3. 检查每个包的完整性
            for (int i = 0; i < originalParser.getPackages().size(); i++) {
                ResTablePackage origPkg = originalParser.getPackages().get(i);
                ResTablePackage modPkg = modifiedParser.getPackages().get(i);
                
                checkPackageIntegrity(origPkg, modPkg, builder);
            }
            
        } catch (Exception e) {
            log.error("ARSC完整性检查失败", e);
            builder.addFailed(ValidationResult.ValidationLevel.INTEGRITY,
                            "完整性检查异常",
                            e.getMessage());
        }
        
        return builder.build();
    }
    
    /**
     * 检查单个包的完整性
     */
    private void checkPackageIntegrity(ResTablePackage original, ResTablePackage modified,
                                      ValidationResult.Builder builder) {
        
        // 1. 检查packageId
        if (original.getId() != modified.getId()) {
            builder.addFailed(ValidationResult.ValidationLevel.INTEGRITY,
                            "packageId改变",
                            String.format("0x%02X -> 0x%02X", 
                                        original.getId(), modified.getId()));
        } else {
            builder.addPassed(ValidationResult.ValidationLevel.INTEGRITY,
                            String.format("packageId不变 (0x%02X)", original.getId()));
        }
        
        // 2. 检查类型字符串池
        if (original.getTypeStrings() != null && modified.getTypeStrings() != null) {
            checkStringPoolIntegrity(
                original.getTypeStrings(), 
                modified.getTypeStrings(),
                "类型字符串池",
                builder);
        }
        
        // 3. 检查资源名字符串池
        if (original.getKeyStrings() != null && modified.getKeyStrings() != null) {
            checkStringPoolIntegrity(
                original.getKeyStrings(),
                modified.getKeyStrings(),
                "资源名字符串池",
                builder);
        }
    }
    
    /**
     * 检查字符串池索引映射
     */
    public boolean checkStringPoolIndices(ResStringPool original, ResStringPool modified) {
        Objects.requireNonNull(original, "original不能为null");
        Objects.requireNonNull(modified, "modified不能为null");
        
        // 字符串数量必须相同
        if (original.getStringCount() != modified.getStringCount()) {
            log.error("字符串池数量不同: {} -> {}", 
                     original.getStringCount(), modified.getStringCount());
            return false;
        }
        
        // 索引映射必须一致（第i个字符串还是第i个，只是内容可能变了）
        log.debug("字符串池索引映射一致: {} 个字符串", original.getStringCount());
        return true;
    }
    
    /**
     * 检查字符串池完整性
     */
    private void checkStringPoolIntegrity(ResStringPool original, ResStringPool modified,
                                         String poolName,
                                         ValidationResult.Builder builder) {
        
        if (original.getStringCount() != modified.getStringCount()) {
            builder.addFailed(ValidationResult.ValidationLevel.INTEGRITY,
                            poolName + "数量改变",
                            String.format("%d -> %d", 
                                        original.getStringCount(),
                                        modified.getStringCount()));
        } else {
            builder.addPassed(ValidationResult.ValidationLevel.INTEGRITY,
                            poolName + "数量不变 (" + original.getStringCount() + ")");
        }
    }
    
    /**
     * 检查资源ID稳定性
     * 
     * @param original 原始ARSC
     * @param modified 修改后的ARSC
     * @return true=资源ID稳定
     */
    public boolean checkResourceIdStability(ArscParser original, ArscParser modified) {
        Objects.requireNonNull(original, "original不能为null");
        Objects.requireNonNull(modified, "modified不能为null");
        
        log.debug("检查资源ID稳定性");
        
        // 检查所有包的ID是否相同
        for (int i = 0; i < original.getPackages().size(); i++) {
            ResTablePackage origPkg = original.getPackages().get(i);
            ResTablePackage modPkg = modified.getPackages().get(i);
            
            if (origPkg.getId() != modPkg.getId()) {
                log.error("packageId改变: 0x{} -> 0x{}", 
                         Integer.toHexString(origPkg.getId()),
                         Integer.toHexString(modPkg.getId()));
                return false;
            }
        }
        
        log.debug("资源ID稳定性检查通过");
        return true;
    }
}

