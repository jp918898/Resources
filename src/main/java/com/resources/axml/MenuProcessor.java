package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * res/menu 处理器
 * 
 * 处理目标：
 * - app:actionViewClass
 * - app:actionProviderClass
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class MenuProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(MenuProcessor.class);
    
    private final SemanticValidator semanticValidator;
    private final ClassMapping classMapping;
    private final PackageMapping packageMapping;
    
    public MenuProcessor(SemanticValidator semanticValidator,
                        ClassMapping classMapping,
                        PackageMapping packageMapping) {
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        this.classMapping = Objects.requireNonNull(classMapping);
        this.packageMapping = Objects.requireNonNull(packageMapping);
    }
    
    /**
     * 处理menu文件
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
        
        log.info("处理menu文件: {}", filePath);
        
        try {
            // 1. 创建Reader和Writer
            AxmlReader reader = new AxmlReader(xmlData);
            AxmlWriter writer = new AxmlWriter();
            
            // 2. 替换计数器
            final int[] replaceCount = {0};
            
            // 3. 使用Visitor模式处理（AxmlReader自动管理栈）
            reader.accept(new AxmlVisitor(writer) {
                @Override
                public NodeVisitor child(String ns, String name) {
                    NodeVisitor child = super.child(ns, name);
                    return new MenuVisitor(child, semanticValidator, classMapping,
                                          packageMapping, replaceCount, filePath);
                }
            });
            
            // 4. 如果没有修改，直接返回原始数据避免破坏文件结构
            if (replaceCount[0] == 0) {
                log.info("menu文件处理完成: {}, 替换0处（未修改，返回原始数据）", filePath);
                return xmlData;
            }
            
            // 🔧 传递原始编码flags（新增，在toByteArray之前）
            writer.setStringPoolFlags(reader.getStringPoolFlags());
            
            // 5. 生成新的XML
            byte[] result = writer.toByteArray();
            log.info("menu文件处理完成: {}, 替换{}处", filePath, replaceCount[0]);
            return result;
            
        } catch (Exception e) {
            log.error("menu文件处理失败: {}", filePath, e);
            throw new IOException("menu文件处理失败: " + e.getMessage(), e);
        }
    }
}

