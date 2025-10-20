package com.resources.scanner;

import com.resources.axml.AxmlParser;
import com.resources.model.ScanResult;
import com.resources.util.AxmlValidator;
import com.resources.validator.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AXML扫描器 - 扫描二进制XML文件，定位所有类名/包名
 * 
 * 扫描目标：
 * - 标签名（自定义View）
 * - 属性值（android:name, class等）
 * - Data Binding（variable type, import type, T()表达式）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class AxmlScanner {
    
    private static final Logger log = LoggerFactory.getLogger(AxmlScanner.class);
    
    // Data Binding表达式模式
    private static final Pattern CLASS_EXPR_PATTERN = Pattern.compile("T\\(([a-zA-Z0-9._]+)\\)");
    
    private final SemanticValidator semanticValidator;
    
    public AxmlScanner(SemanticValidator semanticValidator, Set<String> ownPackagePrefixes) {
        this.semanticValidator = Objects.requireNonNull(semanticValidator);
        // ownPackagePrefixes 参数保留以保持API兼容性
    }
    
    /**
     * 扫描AXML文件
     * 
     * @param filePath 文件路径
     * @param xmlData XML字节数据
     * @return 扫描结果列表
     * @throws IOException 扫描失败
     */
    public List<ScanResult> scan(String filePath, byte[] xmlData) throws IOException {
        Objects.requireNonNull(filePath, "filePath不能为null");
        Objects.requireNonNull(xmlData, "xmlData不能为null");
        
        log.debug("扫描AXML文件: {}", filePath);
        
        // 预检AXML格式
        if (!AxmlValidator.isValidAxml(xmlData)) {
            log.debug("跳过非AXML文件: {} ({}字节)", filePath, xmlData.length);
            return Collections.emptyList();  // 返回空结果，不抛异常
        }
        
        List<ScanResult> results = new ArrayList<>();
        
        try {
            AxmlParser parser = new AxmlParser(xmlData);
            boolean inDataSection = false;
            
            while (true) {
                int event = parser.next();
                
                if (event == AxmlParser.END_FILE) {
                    break;
                }
                
                if (event == AxmlParser.START_TAG) {
                    String tagName = parser.getName();
                    int lineNumber = parser.getLineNumber();
                    
                    // 1. 检测<data>节点
                    if ("data".equals(tagName)) {
                        inDataSection = true;
                    }
                    
                    // 2. 扫描标签名（自定义View）
                    scanTagName(filePath, tagName, lineNumber, results);
                    
                    // 3. 扫描属性
                    scanAttributes(filePath, parser, tagName, lineNumber, inDataSection, results);
                    
                    if ("data".equals(tagName)) {
                        inDataSection = false;
                    }
                }
            }
            
            log.info("AXML扫描完成: {}, 发现{}处", filePath, results.size());
            
        } catch (Exception e) {
            log.error("AXML扫描失败: {}", filePath, e);
            throw new IOException("AXML扫描失败: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * 扫描标签名
     */
    private void scanTagName(String filePath, String tagName, int lineNumber, 
                            List<ScanResult> results) {
        
        // 创建语义上下文
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .tagName(tagName)
            .isTagName(true)
            .build();
        
        // 验证是否为类名且应替换
        if (semanticValidator.validateAndFilter(context, tagName)) {
            ScanResult result = new ScanResult.Builder()
                .filePath(filePath)
                .semanticType(ScanResult.SemanticType.TAG_NAME)
                .location(String.format("line %d, tag <%s>", lineNumber, tagName))
                .originalValue(tagName)
                .build();
            
            results.add(result);
            log.debug("发现自定义View标签: <{}> at line {}", tagName, lineNumber);
        }
    }
    
    /**
     * 扫描属性
     */
    private void scanAttributes(String filePath, AxmlParser parser, String tagName,
                               int lineNumber, boolean inDataSection, 
                               List<ScanResult> results) {
        
        for (int i = 0; i < parser.getAttrCount(); i++) {
            String attrName = parser.getAttrName(i);
            Object attrValue = parser.getAttrValue(i);
            
            if (!(attrValue instanceof String)) {
                continue;
            }
            
            String strValue = (String) attrValue;
            
            // 1. Data Binding的type属性
            if (inDataSection && "type".equals(attrName)) {
                scanDataBindingType(filePath, tagName, attrName, strValue, 
                                   lineNumber, results);
            }
            // 2. Data Binding表达式
            else if (containsClassExpression(strValue)) {
                scanDataBindingExpression(filePath, tagName, attrName, strValue, 
                                         lineNumber, results);
            }
            // 3. 普通属性值
            else {
                scanAttributeValue(filePath, tagName, attrName, strValue, 
                                  lineNumber, results);
            }
        }
    }
    
    /**
     * 扫描Data Binding的type属性
     */
    private void scanDataBindingType(String filePath, String tagName, String attrName,
                                     String value, int lineNumber, List<ScanResult> results) {
        
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .tagName(tagName)
            .attributeName(attrName)
            .isTagName(false)
            .build();
        
        if (semanticValidator.validateAndFilter(context, value)) {
            ScanResult result = new ScanResult.Builder()
                .filePath(filePath)
                .semanticType(ScanResult.SemanticType.DATABINDING_TYPE)
                .location(String.format("line %d, <%s> %s", lineNumber, tagName, attrName))
                .originalValue(value)
                .build();
            
            results.add(result);
            log.debug("发现Data Binding type: {} = '{}' at line {}", 
                     attrName, value, lineNumber);
        }
    }
    
    /**
     * 扫描Data Binding表达式
     */
    private void scanDataBindingExpression(String filePath, String tagName, String attrName,
                                          String expression, int lineNumber, 
                                          List<ScanResult> results) {
        
        Matcher matcher = CLASS_EXPR_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            String className = matcher.group(1);
            
            SemanticValidator.Context context = new SemanticValidator.Context.Builder()
                .filePath(filePath)
                .tagName(tagName)
                .isDataBindingExpression(true)
                .build();
            
            if (semanticValidator.validateAndFilter(context, className)) {
                ScanResult result = new ScanResult.Builder()
                    .filePath(filePath)
                    .semanticType(ScanResult.SemanticType.DATABINDING_EXPR)
                    .location(String.format("line %d, <%s> %s expression", 
                                          lineNumber, tagName, attrName))
                    .originalValue(className)
                    .build();
                
                results.add(result);
                log.debug("发现Data Binding表达式: T({}) at line {}", className, lineNumber);
            }
        }
    }
    
    /**
     * 扫描普通属性值
     */
    private void scanAttributeValue(String filePath, String tagName, String attrName,
                                   String value, int lineNumber, List<ScanResult> results) {
        
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath(filePath)
            .tagName(tagName)
            .attributeName(attrName)
            .isTagName(false)
            .build();
        
        if (semanticValidator.validateAndFilter(context, value)) {
            ScanResult result = new ScanResult.Builder()
                .filePath(filePath)
                .semanticType(ScanResult.SemanticType.ATTRIBUTE_VALUE)
                .location(String.format("line %d, <%s> %s", lineNumber, tagName, attrName))
                .originalValue(value)
                .build();
            
            results.add(result);
            log.debug("发现类名属性: {} = '{}' at line {}", attrName, value, lineNumber);
        }
    }
    
    /**
     * 判断字符串是否包含类表达式
     */
    private boolean containsClassExpression(String value) {
        return value != null && value.contains("T(") && CLASS_EXPR_PATTERN.matcher(value).find();
    }
    
    /**
     * 批量扫描AXML文件
     * 
     * @param files 文件路径到数据的映射
     * @return 所有扫描结果
     * @throws IOException 扫描失败
     */
    public List<ScanResult> scanBatch(Map<String, byte[]> files) throws IOException {
        Objects.requireNonNull(files, "files不能为null");
        
        log.info("批量扫描AXML: {} 个文件", files.size());
        
        List<ScanResult> allResults = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            try {
                List<ScanResult> results = scan(entry.getKey(), entry.getValue());
                allResults.addAll(results);
                successCount++;
                
            } catch (Exception e) {
                log.error("扫描文件失败: {}", entry.getKey(), e);
                errorCount++;
            }
        }
        
        log.info("批量扫描完成: 成功={}, 失败={}, 发现{}处", 
                successCount, errorCount, allResults.size());
        
        return allResults;
    }
}

