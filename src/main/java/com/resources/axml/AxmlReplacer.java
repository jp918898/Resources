package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.util.AxmlValidator;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;



/**
 * AXML统一替换引擎
 * 
 * 协调所有AXML处理器：
 * - LayoutProcessor (res/layout)
 * - MenuProcessor (res/menu)
 * - NavigationProcessor (res/navigation)
 * - XmlConfigProcessor (res/xml)
 * - DataBindingProcessor (Data Binding)
 * 
 * 根据文件路径自动选择合适的处理器
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class AxmlReplacer {
    
    private static final Logger log = LoggerFactory.getLogger(AxmlReplacer.class);
    
    private final LayoutProcessor layoutProcessor;
    private final MenuProcessor menuProcessor;
    private final NavigationProcessor navigationProcessor;
    private final XmlConfigProcessor xmlConfigProcessor;
    private final DataBindingProcessor dataBindingProcessor;
    
    public AxmlReplacer(SemanticValidator semanticValidator,
                       ClassMapping classMapping,
                       PackageMapping packageMapping,
                       boolean processToolsContext) {
        
        Objects.requireNonNull(semanticValidator, "semanticValidator不能为null");
        Objects.requireNonNull(classMapping, "classMapping不能为null");
        Objects.requireNonNull(packageMapping, "packageMapping不能为null");
        
        // 初始化所有处理器
        this.layoutProcessor = new LayoutProcessor(
            semanticValidator, classMapping, packageMapping, processToolsContext);
        
        this.menuProcessor = new MenuProcessor(
            semanticValidator, classMapping, packageMapping);
        
        this.navigationProcessor = new NavigationProcessor(
            semanticValidator, classMapping, packageMapping);
        
        this.xmlConfigProcessor = new XmlConfigProcessor(
            semanticValidator, classMapping, packageMapping);
        
        this.dataBindingProcessor = new DataBindingProcessor(
            semanticValidator, classMapping, packageMapping);
        
        log.info("AxmlReplacer初始化完成");
    }
    
    /**
     * 替换AXML文件
     * 根据文件路径自动选择合适的处理器
     * 
     * @param filePath 文件路径（ZIP entry路径，如"res/layout/activity_main.xml"）
     * @param xmlData 原始XML字节数据
     * @return 修改后的XML数据
     * @throws IOException 处理失败
     */
    public byte[] replaceAxml(String filePath, byte[] xmlData) throws IOException {
        Objects.requireNonNull(filePath, "filePath不能为null");
        Objects.requireNonNull(xmlData, "xmlData不能为null");
        
        log.debug("开始处理AXML: {}", filePath);
        
        // 预检AXML格式
        if (!AxmlValidator.isValidAxml(xmlData)) {
            log.debug("跳过非AXML文件: {} ({}字节)", filePath, xmlData.length);
            return xmlData;  // 返回原数据
        }
        
        // 1. 检查是否为Data Binding布局
        if (isDataBindingLayout(xmlData)) {
            log.debug("检测到Data Binding布局: {}", filePath);
            return dataBindingProcessor.process(filePath, xmlData);
        }
        
        // 2. 根据路径选择处理器（支持混淆后的APK）
        String normalizedPath = filePath.replace('\\', '/').toLowerCase();
        
        // 优先匹配标准路径
        if (normalizedPath.contains("/layout/") || normalizedPath.contains("/layout-")) {
            return layoutProcessor.process(filePath, xmlData);
        }
        else if (normalizedPath.contains("/menu/")) {
            return menuProcessor.process(filePath, xmlData);
        }
        else if (normalizedPath.contains("/navigation/")) {
            return navigationProcessor.process(filePath, xmlData);
        }
        else if (normalizedPath.contains("/xml/")) {
            return xmlConfigProcessor.process(filePath, xmlData);
        }
        else if (normalizedPath.contains("/animator/") || 
                 normalizedPath.contains("/anim/") || 
                 normalizedPath.contains("/transition/")) {
            // ✅ 新增：处理动画和过渡XML
            // 这些文件可能包含自定义Interpolator/Transition类名
            log.debug("处理animator/anim/transition文件: {}", filePath);
            return xmlConfigProcessor.process(filePath, xmlData);
        }
        else {
            // 混淆后的APK：res根目录下的XML
            // 通过XML内容判断类型
            log.debug("混淆路径，通过内容判断类型: {}", filePath);
            FileType contentType = detectFileTypeByContent(xmlData);
            
            switch (contentType) {
                case LAYOUT:
                    log.debug("内容类型：Layout（替换标签名+属性）");
                    return layoutProcessor.process(filePath, xmlData);
                case MENU:
                    log.debug("内容类型：Menu（替换属性）");
                    return menuProcessor.process(filePath, xmlData);
                case NAVIGATION:
                    log.debug("内容类型：Navigation（替换属性）");
                    return navigationProcessor.process(filePath, xmlData);
                case DRAWABLE:
                    log.debug("内容类型：Drawable（仅替换属性，保留系统标签）");
                    // Drawable XML使用XmlConfigProcessor：只替换属性值，不动标签名
                    // 这样可以保留<vector>/<path>等系统标签，同时替换可能存在的包名引用
                    return xmlConfigProcessor.process(filePath, xmlData);
                default:
                    log.debug("内容类型：未知（使用XmlConfigProcessor仅处理属性）");
                    return xmlConfigProcessor.process(filePath, xmlData);
            }
        }
    }
    
    /**
     * 检测是否为Data Binding布局
     * 
     * Data Binding布局的特征：
     * - 根节点为<layout>
     * - 包含<data>子节点
     * 
     * @param xmlData XML字节数据
     * @return true=Data Binding布局
     */
    private boolean isDataBindingLayout(byte[] xmlData) {
        try {
            AxmlParser parser = new AxmlParser(xmlData);
            
            while (true) {
                int event = parser.next();
                
                if (event == AxmlParser.END_FILE) {
                    break;
                }
                
                if (event == AxmlParser.START_TAG) {
                    String tagName = parser.getName();
                    
                    // 检查根节点是否为<layout>
                    if ("layout".equals(tagName)) {
                        return true;
                    }
                    
                    // 如果第一个标签不是layout，就不是Data Binding
                    break;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("检测Data Binding失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 批量处理AXML文件
     * 
     * @param files 文件路径到数据的映射
     * @return 文件路径到修改后数据的映射
     * @throws IOException 处理失败
     */
    public Map<String, byte[]> replaceAxmlBatch(Map<String, byte[]> files) throws IOException {
        Objects.requireNonNull(files, "files不能为null");
        
        log.info("批量处理AXML: {} 个文件", files.size());
        
        Map<String, byte[]> results = new LinkedHashMap<>();
        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String filePath = entry.getKey();
            byte[] xmlData = entry.getValue();
            
            try {
                byte[] modifiedData = replaceAxml(filePath, xmlData);
                results.put(filePath, modifiedData);
                
                // 精确判断是否真的修改了
                if (modifiedData == xmlData) {
                    skippedCount++;  // 返回原数据=跳过（包括非AXML和replaceCount==0）
                } else {
                    successCount++;  // 生成了新数据=成功
                }
                
            } catch (Exception e) {
                log.error("处理文件失败: {}", filePath, e);
                errorCount++;
                
                // 失败时保留原始数据（宽松模式）
                results.put(filePath, xmlData);
            }
        }
        
        log.info("批量处理完成: 成功={}, 跳过={}, 失败={}", 
                 successCount, skippedCount, errorCount);
        
        // 只在全部失败时才抛异常（与扫描阶段一致）
        if (errorCount > 0 && successCount == 0 && skippedCount == 0) {
            throw new IOException(
                String.format("批量处理全部失败: %d个文件", errorCount));
        }
        // 部分失败时不抛异常，只记录警告（已在catch块中log.error）
        
        return results;
    }
    
    /**
     * 检测文件类型
     * 
     * @param filePath 文件路径
     * @return 文件类型（LAYOUT, MENU, NAVIGATION, XML, UNKNOWN）
     */
    public FileType detectFileType(String filePath) {
        String normalizedPath = filePath.replace('\\', '/').toLowerCase();
        
        if (normalizedPath.contains("/layout/") || normalizedPath.contains("/layout-")) {
            return FileType.LAYOUT;
        }
        if (normalizedPath.contains("/menu/")) {
            return FileType.MENU;
        }
        if (normalizedPath.contains("/navigation/")) {
            return FileType.NAVIGATION;
        }
        if (normalizedPath.contains("/xml/")) {
            return FileType.XML;
        }
        
        return FileType.UNKNOWN;
    }
    
    /**
     * 通过XML内容判断文件类型
     * 
     * @param xmlData XML字节数据
     * @return 文件类型
     */
    private FileType detectFileTypeByContent(byte[] xmlData) {
        try {
            AxmlParser parser = new AxmlParser(xmlData);
            
            while (true) {
                int event = parser.next();
                
                if (event == AxmlParser.END_FILE) {
                    break;
                }
                
                if (event == AxmlParser.START_TAG) {
                    String tagName = parser.getName();
                    
                    // 检查根标签判断类型
                    if (tagName != null) {
                        tagName = tagName.toLowerCase();
                        
                        // Drawable XML: vector, selector, shape, layer-list, animated-vector等
                        if (tagName.equals("vector") || tagName.equals("selector") || 
                            tagName.equals("shape") || tagName.equals("layer-list") ||
                            tagName.equals("level-list") || tagName.equals("transition") ||
                            tagName.equals("animation-list") || tagName.equals("animated-vector") ||
                            tagName.equals("animated-selector") || tagName.equals("ripple") ||
                            tagName.equals("adaptive-icon") || tagName.equals("inset") ||
                            tagName.equals("scale") || tagName.equals("clip") ||
                            tagName.equals("rotate") || tagName.equals("bitmap")) {
                            return FileType.DRAWABLE;
                        }
                        
                        // Menu XML
                        if (tagName.equals("menu")) {
                            return FileType.MENU;
                        }
                        
                        // Navigation XML
                        if (tagName.equals("navigation")) {
                            return FileType.NAVIGATION;
                        }
                        
                        // Layout XML: 以下是常见的Android布局根标签
                        if (tagName.equals("linearlayout") || tagName.equals("relativelayout") ||
                            tagName.equals("framelayout") || tagName.equals("constraintlayout") ||
                            tagName.equals("coordinatorlayout") || tagName.equals("scrollview") ||
                            tagName.equals("nestedscrollview") || tagName.equals("recyclerview") ||
                            tagName.equals("viewpager") || tagName.equals("viewpager2") ||
                            tagName.equals("tablayout") || tagName.equals("appbarlayout") ||
                            tagName.equals("collapsingtoolbarlayout") || tagName.equals("toolbar") ||
                            tagName.equals("merge") || tagName.contains(".")) {
                            // 包含"."表示自定义View（如com.mcxtzhang.CustomView）
                            return FileType.LAYOUT;
                        }
                    }
                    
                    // 第一个标签就结束检测
                    break;
                }
            }
            
            // 无法判断，默认为UNKNOWN
            return FileType.UNKNOWN;
            
        } catch (Exception e) {
            log.warn("通过内容检测文件类型失败: {}", e.getMessage());
            return FileType.UNKNOWN;
        }
    }
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        LAYOUT,
        MENU,
        NAVIGATION,
        XML,
        DRAWABLE,
        UNKNOWN
    }
}

