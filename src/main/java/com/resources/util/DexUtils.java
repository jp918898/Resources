package com.resources.util;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DEX工具类 - 统一的DEX文件处理工具
 * 
 * 提供DEX文件加载和类名转换的通用方法，避免代码重复
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class DexUtils {
    
    private static final Logger log = LoggerFactory.getLogger(DexUtils.class);
    
    /**
     * 加载DEX文件中的所有类
     * 
     * @param dexPath DEX文件路径
     * @return 类名集合（Java格式：com.example.MainActivity）
     * @throws IOException 加载失败
     */
    public static Set<String> loadDexClasses(String dexPath) throws IOException {
        Objects.requireNonNull(dexPath, "dexPath不能为null");
        
        if (dexPath.isEmpty()) {
            throw new IllegalArgumentException("dexPath不能为空字符串");
        }
        
        log.debug("加载DEX文件: {}", dexPath);
        
        File dexFile = new File(dexPath);
        if (!dexFile.exists()) {
            throw new FileNotFoundException("DEX文件不存在: " + dexPath);
        }
        
        if (!dexFile.canRead()) {
            throw new IOException("DEX文件不可读: " + dexPath);
        }
        
        try {
            DexFile dex = DexFileFactory.loadDexFile(dexFile, null);
            
            Set<String> classes = dex.getClasses().stream()
                .map(ClassDef::getType)
                .map(DexUtils::descriptorToClassName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            log.debug("DEX类加载完成: {} -> {} 个类", dexPath, classes.size());
            return classes;
            
        } catch (IOException e) {
            log.error("DEX文件加载失败: {}", dexPath, e);
            throw e;
        } catch (Exception e) {
            log.error("DEX文件解析失败: {}", dexPath, e);
            throw new IOException("DEX文件解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将DEX类型描述符转换为Java类名
     * 
     * 转换规则：
     * - Lcom/example/MainActivity; -> com.example.MainActivity
     * - Lcom/example/Outer$Inner; -> com.example.Outer$Inner
     * - [Ljava/lang/String; -> 保持不变（数组类型）
     * - I, V, Z等基本类型 -> 保持不变
     * 
     * @param descriptor DEX类型描述符
     * @return Java类名，如果描述符无效则返回null
     */
    public static String descriptorToClassName(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return null;
        }
        
        // 数组类型：保持不变或仅转换元素类型（这里选择保持不变以简化）
        if (descriptor.startsWith("[")) {
            return descriptor;
        }
        
        // 基本类型：I, V, Z, B, S, C, J, F, D
        if (descriptor.length() == 1) {
            return descriptor;
        }
        
        // 类类型：Lcom/example/MainActivity;
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            String className = descriptor.substring(1, descriptor.length() - 1);
            // 将/替换为.
            return className.replace('/', '.');
        }
        
        // 无效格式：返回null
        log.warn("无效的DEX描述符: {}", descriptor);
        return null;
    }
    
    /**
     * 将Java类名转换为DEX类型描述符
     * 
     * @param className Java类名（如com.example.MainActivity）
     * @return DEX描述符（如Lcom/example/MainActivity;）
     */
    public static String classNameToDescriptor(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        
        // 将.替换为/，并添加L和;
        return "L" + className.replace('.', '/') + ";";
    }
    
    /**
     * 验证DEX文件是否可访问
     * 
     * @param dexPath DEX文件路径
     * @return true=可访问，false=不可访问
     */
    public static boolean isDexFileAccessible(String dexPath) {
        if (dexPath == null || dexPath.isEmpty()) {
            return false;
        }
        
        File dexFile = new File(dexPath);
        return dexFile.exists() && dexFile.canRead() && dexFile.isFile();
    }
}

