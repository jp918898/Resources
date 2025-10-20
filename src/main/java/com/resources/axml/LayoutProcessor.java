package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * res/layout 处理器
 * 
 * 处理目标：
 * - 自定义View的标签名（如<com.example.MyView>）
 * - android:name属性（Fragment）
 * - class属性（显式类名）
 * - app:layoutManager（RecyclerView）
 * - tools:context（可选）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class LayoutProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(LayoutProcessor.class);
    
    private final SemanticValidator semanticValidator;
    private final ClassMapping classMapping;
    private final PackageMapping packageMapping;
    private final boolean processToolsContext;
    
    public LayoutProcessor(SemanticValidator semanticValidator,
                          ClassMapping classMapping,
                          PackageMapping packageMapping,
                          boolean processToolsContext) {
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        this.classMapping = Objects.requireNonNull(classMapping);
        this.packageMapping = Objects.requireNonNull(packageMapping);
        this.processToolsContext = processToolsContext;
    }
    
    /**
     * 处理layout文件
     * 
     * 工业级架构：使用AxmlReader.accept()模式
     * - 自动维护NodeVisitor栈（无需手写）
     * - 保护XML层级结构
     * - 极致鲁棒、零出错概率
     * 
     * @param filePath 文件路径
     * @param xmlData 原始XML数据
     * @return 修改后的XML数据
     * @throws IOException 处理失败
     */
    public byte[] process(String filePath, byte[] xmlData) throws IOException {
        Objects.requireNonNull(filePath, "filePath不能为null");
        Objects.requireNonNull(xmlData, "xmlData不能为null");
        
        log.info("处理layout文件: {}", filePath);
        
        try {
            // 1. 创建Reader和Writer
            AxmlReader reader = new AxmlReader(xmlData);
            AxmlWriter writer = new AxmlWriter();
            
            // 2. 替换计数器（数组用于在匿名类中修改）
            final int[] replaceCount = {0};
            
            // 3. 使用Visitor模式处理（AxmlReader自动管理栈）
            reader.accept(new AxmlVisitor(writer) {
                @Override
                public NodeVisitor child(String ns, String name) {
                    // 【关键修复】先替换标签名，再创建节点（解决根标签未替换问题）
                    String newName = replaceTagNameIfNeeded(name);
                    if (!name.equals(newName)) {
                        replaceCount[0]++;
                        log.info("[LayoutProcessor.child] ✅ 替换根/子标签: {} -> {} (文件: {})", name, newName, filePath);
                    }
                    NodeVisitor child = super.child(ns, newName);  // 用新名字创建节点
                    return new LayoutVisitor(child, semanticValidator, classMapping,
                                            packageMapping, processToolsContext,
                                            replaceCount, filePath);
                }
                
                // 复用LayoutVisitor的替换逻辑
                private String replaceTagNameIfNeeded(String tagName) {
                    SemanticValidator.Context context = new SemanticValidator.Context.Builder()
                        .filePath(filePath)
                        .tagName(tagName)
                        .isTagName(true)
                        .build();
                    
                    if (!semanticValidator.validateAndFilter(context, tagName)) {
                        return tagName;
                    }
                    
                    // 1. 精确匹配
                    String replacement = classMapping.getNewClass(tagName);
                    if (replacement != null) {
                        return replacement;
                    }
                    
                    // 2. 前缀匹配
                    replacement = packageMapping.replace(tagName);
                    return replacement;
                }
            });
            
            // 4. 如果没有修改，直接返回原始数据避免破坏文件结构
            if (replaceCount[0] == 0) {
                log.info("layout文件处理完成: {}, 替换0处（未修改，返回原始数据）", filePath);
                return xmlData;
            }
            
            // 🔧 4. 传递原始编码flags（新增，在toByteArray之前）
            writer.setStringPoolFlags(reader.getStringPoolFlags());
            
            // 5. 生成新的XML
            byte[] result = writer.toByteArray();
            log.info("layout文件处理完成: {}, 替换{}处", filePath, replaceCount[0]);
            return result;
            
        } catch (Exception e) {
            log.error("layout文件处理失败: {}", filePath, e);
            throw new IOException("layout文件处理失败: " + e.getMessage(), e);
        }
    }
}

