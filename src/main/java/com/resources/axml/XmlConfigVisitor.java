package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * XmlConfig文件Visitor - 工业级AXML层级结构保护
 * 
 * 功能：
 * - 替换android:name属性（Preference等）
 * - 替换android:fragment属性
 * - 替换class属性
 * - 自动维护XML层级结构（由AxmlReader管理栈）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class XmlConfigVisitor extends NodeVisitor {
    
    private static final Logger log = LoggerFactory.getLogger(XmlConfigVisitor.class);
    
    private final SemanticValidator semanticValidator;
    private final ClassMapping classMapping;
    private final PackageMapping packageMapping;
    private final int[] replaceCount;
    private final String filePath;
    
    public XmlConfigVisitor(NodeVisitor child,
                           SemanticValidator semanticValidator,
                           ClassMapping classMapping,
                           PackageMapping packageMapping,
                           int[] replaceCount,
                           String filePath) {
        super(child);
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        this.classMapping = Objects.requireNonNull(classMapping);
        this.packageMapping = Objects.requireNonNull(packageMapping);
        this.replaceCount = replaceCount;
        this.filePath = filePath;
    }
    
    @Override
    public NodeVisitor child(String ns, String name) {
        NodeVisitor child = super.child(ns, name);
        return new XmlConfigVisitor(child, semanticValidator, classMapping,
                                   packageMapping, replaceCount, filePath);
    }
    
    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        Object newObj = replaceAttributeIfNeeded(name, obj);
        
        if (!Objects.equals(obj, newObj)) {
            replaceCount[0]++;
            log.debug("替换xml属性: {} = '{}' -> '{}'", name, obj, newObj);
        }
        
        super.attr(ns, name, resourceId, type, newObj);
    }
    
    private Object replaceAttributeIfNeeded(String attrName, Object attrValue) {
        if (!(attrValue instanceof String)) {
            return attrValue;
        }
        
        // 检查属性名
        if (!"android:name".equals(attrName) && 
            !"android:fragment".equals(attrName) &&
            !"class".equals(attrName)) {
            return attrValue;
        }
        
        String strValue = (String) attrValue;
        
        // 语义验证
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .attributeName(attrName)
            .isTagName(false)
            .build();
        
        if (!semanticValidator.validateAndFilter(context, strValue)) {
            return attrValue;
        }
        
        // 1. 精确匹配
        String replacement = classMapping.getNewClass(strValue);
        if (replacement != null) {
            return replacement;
        }
        
        // 2. 前缀匹配
        replacement = packageMapping.replace(strValue);
        return replacement;
    }
}




