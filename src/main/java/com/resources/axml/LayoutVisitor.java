package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Layout文件Visitor - 工业级AXML层级结构保护
 * 
 * 功能：
 * - 替换自定义View标签名（如com.mcxtzhang.View → com.zerozhang.View）
 * - 替换类名属性（android:name、class、app:layoutManager、tools:context）
 * - 自动维护XML层级结构（由AxmlReader管理栈）
 * 
 * 架构：
 * - 继承NodeVisitor
 * - 委托模式：接收child NodeVisitor并包装
 * - 在child()和attr()中实现替换逻辑
 * - 不维护栈（由AxmlReader负责）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class LayoutVisitor extends NodeVisitor {
    
    private static final Logger log = LoggerFactory.getLogger(LayoutVisitor.class);
    
    private final SemanticValidator semanticValidator;
    private final ClassMapping classMapping;
    private final PackageMapping packageMapping;
    private final boolean processToolsContext;
    private final int[] replaceCount;  // 替换计数器（数组用于在匿名类中修改）
    private final String filePath;
    
    /**
     * 构造函数
     * 
     * @param child 子NodeVisitor（委托目标）
     * @param semanticValidator 语义验证器
     * @param classMapping 类名映射
     * @param packageMapping 包名映射
     * @param processToolsContext 是否处理tools:context
     * @param replaceCount 替换计数器
     * @param filePath 文件路径
     */
    public LayoutVisitor(NodeVisitor child,
                        SemanticValidator semanticValidator,
                        ClassMapping classMapping,
                        PackageMapping packageMapping,
                        boolean processToolsContext,
                        int[] replaceCount,
                        String filePath) {
        super(child);
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        this.classMapping = Objects.requireNonNull(classMapping);
        this.packageMapping = Objects.requireNonNull(packageMapping);
        this.processToolsContext = processToolsContext;
        this.replaceCount = replaceCount;
        this.filePath = filePath;
    }
    
    /**
     * 处理子节点
     * 
     * 关键逻辑：
     * 1. 替换标签名（如果是自定义View的FQCN）
     * 2. 调用super.child()创建节点（自动维护层级）
     * 3. 返回LayoutVisitor包装的child，继续处理嵌套节点
     * 
     * @param ns 命名空间
     * @param name 标签名
     * @return NodeVisitor 子节点访问器
     */
    @Override
    public NodeVisitor child(String ns, String name) {
        // 🔍 诊断日志：记录每个标签
        log.trace("[LayoutVisitor.child] 处理标签: {} (文件: {})", name, filePath);
        
        // 1. 检查并替换标签名（自定义View）
        String newName = replaceTagNameIfNeeded(name);
        
        if (!name.equals(newName)) {
            replaceCount[0]++;
            log.info("[LayoutVisitor.child] ✅ 替换标签名: {} -> {} (文件: {})", name, newName, filePath);
        }
        
        // 2. 调用父类child()创建节点（自动维护层级）
        NodeVisitor child = super.child(ns, newName);
        
        // 3. 返回包装的Visitor，继续处理子节点
        return new LayoutVisitor(child, semanticValidator, classMapping, 
                                packageMapping, processToolsContext, 
                                replaceCount, filePath);
    }
    
    /**
     * 处理属性
     * 
     * 关键逻辑：
     * 1. 检查属性名是否为类名语义（android:name/class/app:layoutManager/tools:context）
     * 2. 替换属性值（如果是类名）
     * 3. 调用super.attr()写入
     * 
     * @param ns 命名空间
     * @param name 属性名
     * @param resourceId 资源ID
     * @param type 类型
     * @param obj 属性值
     */
    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        // 1. 检查并替换属性值（类名属性）
        Object newObj = replaceAttributeIfNeeded(name, obj);
        
        if (!Objects.equals(obj, newObj)) {
            replaceCount[0]++;
            log.debug("替换属性值: {} = '{}' -> '{}'", name, obj, newObj);
        }
        
        // 2. 调用父类attr()写入
        super.attr(ns, name, resourceId, type, newObj);
    }
    
    /**
     * 替换标签名（如果是自定义View的FQCN）
     */
    private String replaceTagNameIfNeeded(String tagName) {
        // 检查是否为FQCN（完全限定类名）
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .tagName(tagName)
            .isTagName(true)
            .build();
        
        if (!semanticValidator.validateAndFilter(context, tagName)) {
            log.info("[诊断] 标签名未通过验证: {} (文件: {})", tagName, filePath);
            return tagName; // 不是FQCN或不应替换
        }
        
        log.info("[诊断] 标签名通过验证: {} (文件: {})", tagName, filePath);
        
        // 1. 尝试精确匹配（类名映射）
        String replacement = classMapping.getNewClass(tagName);
        if (replacement != null) {
            log.info("✓ 精确匹配替换: {} -> {}", tagName, replacement);
            return replacement;
        }
        
        // 2. 尝试前缀匹配（包名映射）
        replacement = packageMapping.replace(tagName);
        if (!replacement.equals(tagName)) {
            log.info("✓ 前缀匹配替换: {} -> {} (文件: {})", tagName, replacement, filePath);
            return replacement;
        }
        
        log.info("[诊断] 包名映射未匹配: {} (返回原值, 文件: {})", tagName, filePath);
        return tagName;
    }
    
    /**
     * 替换属性值（如果是类名属性）
     */
    private Object replaceAttributeIfNeeded(String attrName, Object attrValue) {
        if (!(attrValue instanceof String)) {
            return attrValue; // 只处理字符串类型
        }
        
        String strValue = (String) attrValue;
        
        // 检查属性名是否应该处理
        if (!shouldProcessAttribute(attrName)) {
            return attrValue;
        }
        
        // 语义验证
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .attributeName(attrName)
            .isTagName(false)
            .build();
        
        if (!semanticValidator.validateAndFilter(context, strValue)) {
            return attrValue;
        }
        
        // 1. 尝试精确匹配
        String replacement = classMapping.getNewClass(strValue);
        if (replacement != null) {
            return replacement;
        }
        
        // 2. 尝试前缀匹配
        replacement = packageMapping.replace(strValue);
        if (!replacement.equals(strValue)) {
            return replacement;
        }
        
        return attrValue;
    }
    
    /**
     * 判断属性是否应该处理
     */
    private boolean shouldProcessAttribute(String attrName) {
        // 标准属性
        if ("android:name".equals(attrName) || 
            "class".equals(attrName) ||
            "app:layoutManager".equals(attrName)) {
            return true;
        }
        
        // tools:context（可选）
        if ("tools:context".equals(attrName)) {
            return processToolsContext;
        }
        
        return false;
    }
}

