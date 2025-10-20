package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataBinding文件Visitor - 工业级AXML层级结构保护
 * 
 * 功能：
 * - 替换<variable>和<import>的type属性
 * - 替换表达式中的T(FQCN)
 * - 追踪<data>节点上下文
 * - 自动维护XML层级结构（由AxmlReader管理栈）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class DataBindingVisitor extends NodeVisitor {
    
    private static final Logger log = LoggerFactory.getLogger(DataBindingVisitor.class);
    
    // Data Binding表达式模式：T(FQCN)
    private static final Pattern CLASS_EXPR_PATTERN = Pattern.compile("T\\(([a-zA-Z0-9._]+)\\)");
    
    private final SemanticValidator semanticValidator;
    private final ClassMapping classMapping;
    private final PackageMapping packageMapping;
    private final int[] replaceCount;
    private final String filePath;
    private final boolean inDataSection;  // 是否在<data>节点内
    private final String currentTagName;  // 当前标签名
    
    public DataBindingVisitor(NodeVisitor child,
                             SemanticValidator semanticValidator,
                             ClassMapping classMapping,
                             PackageMapping packageMapping,
                             int[] replaceCount,
                             String filePath,
                             boolean inDataSection,
                             String currentTagName) {
        super(child);
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        this.classMapping = Objects.requireNonNull(classMapping);
        this.packageMapping = Objects.requireNonNull(packageMapping);
        this.replaceCount = replaceCount;
        this.filePath = filePath;
        this.inDataSection = inDataSection;
        this.currentTagName = currentTagName;
    }
    
    @Override
    public NodeVisitor child(String ns, String name) {
        // 检测是否进入<data>节点
        boolean enteringDataSection = "data".equals(name);
        boolean newInDataSection = inDataSection || enteringDataSection;
        
        if (enteringDataSection) {
            log.debug("进入Data Binding <data>节点");
        }
        
        NodeVisitor child = super.child(ns, name);
        return new DataBindingVisitor(child, semanticValidator, classMapping,
                                     packageMapping, replaceCount, filePath,
                                     newInDataSection, name);
    }
    
    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        Object newObj = replaceAttributeIfNeeded(name, obj);
        
        if (!Objects.equals(obj, newObj)) {
            replaceCount[0]++;
            log.debug("替换Data Binding属性: {} = '{}' -> '{}'", name, obj, newObj);
        }
        
        super.attr(ns, name, resourceId, type, newObj);
    }
    
    @Override
    public void end() {
        // 检测是否退出<data>节点
        if ("data".equals(currentTagName)) {
            log.debug("退出Data Binding <data>节点");
        }
        
        super.end();
    }
    
    private Object replaceAttributeIfNeeded(String attrName, Object attrValue) {
        if (!(attrValue instanceof String)) {
            return attrValue;
        }
        
        String strValue = (String) attrValue;
        
        // 1. 处理<variable>和<import>的type属性
        if (inDataSection && "type".equals(attrName) && isDataBindingTypeTag(currentTagName)) {
            return replaceType(strValue);
        }
        
        // 2. 处理表达式中的T(FQCN)
        if (containsClassExpression(strValue)) {
            return replaceExpression(strValue);
        }
        
        return attrValue;
    }
    
    private boolean isDataBindingTypeTag(String tagName) {
        return "variable".equals(tagName) || "import".equals(tagName);
    }
    
    private boolean containsClassExpression(String value) {
        return value.contains("T(") && CLASS_EXPR_PATTERN.matcher(value).find();
    }
    
    private String replaceType(String className) {
        // 语义验证
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .attributeName("type")
            .isTagName(false)
            .build();
        
        if (!semanticValidator.validateAndFilter(context, className)) {
            return className;
        }
        
        // 1. 精确匹配
        String replacement = classMapping.getNewClass(className);
        if (replacement != null) {
            return replacement;
        }
        
        // 2. 前缀匹配
        replacement = packageMapping.replace(className);
        return replacement;
    }
    
    private String replaceExpression(String expression) {
        Matcher matcher = CLASS_EXPR_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        
        boolean replaced = false;
        
        while (matcher.find()) {
            String className = matcher.group(1);
            
            // 语义验证
            SemanticValidator.Context context = new SemanticValidator.Context.Builder()
                .filePath(filePath)
                .isDataBindingExpression(true)
                .build();
            
            if (semanticValidator.validateAndFilter(context, className)) {
                // 尝试替换
                String newClassName = classMapping.getNewClass(className);
                if (newClassName == null) {
                    newClassName = packageMapping.replace(className);
                }
                
                if (!className.equals(newClassName)) {
                    log.debug("表达式类名替换: '{}' -> '{}'", className, newClassName);
                    matcher.appendReplacement(result, "T(" + newClassName + ")");
                    replaced = true;
                } else {
                    matcher.appendReplacement(result, matcher.group(0));
                }
            } else {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        
        matcher.appendTail(result);
        
        return replaced ? result.toString() : expression;
    }
}




