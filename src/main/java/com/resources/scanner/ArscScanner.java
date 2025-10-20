package com.resources.scanner;

import com.resources.arsc.ArscParser;
import com.resources.arsc.ResStringPool;
import com.resources.arsc.ResTablePackage;
import com.resources.model.ScanResult;
import com.resources.mapping.WhitelistFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ARSC扫描器 - 扫描resources.arsc，定位所有类名/包名
 * 
 * 扫描目标：
 * - 全局字符串池中的类名/包名
 * - ResTable_package.name（包名）
 * - 包内字符串池（type strings, key strings）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscScanner {
    
    private static final Logger log = LoggerFactory.getLogger(ArscScanner.class);
    
    private final WhitelistFilter whitelistFilter;
    private final Set<String> ownPackagePrefixes;
    
    public ArscScanner(WhitelistFilter whitelistFilter, Set<String> ownPackagePrefixes) {
        this.whitelistFilter = Objects.requireNonNull(whitelistFilter);
        this.ownPackagePrefixes = new HashSet<>(ownPackagePrefixes);
    }
    
    /**
     * 扫描resources.arsc文件
     * 
     * @param arscData ARSC字节数据
     * @return 扫描结果列表
     */
    public List<ScanResult> scan(byte[] arscData) {
        Objects.requireNonNull(arscData, "arscData不能为null");
        
        log.info("扫描resources.arsc: {} 字节", arscData.length);
        
        List<ScanResult> results = new ArrayList<>();
        
        try {
            // 1. 解析ARSC
            ArscParser parser = new ArscParser();
            parser.parse(arscData);
            
            // 2. 扫描全局字符串池
            if (parser.getGlobalStringPool() != null) {
                scanGlobalStringPool(parser.getGlobalStringPool(), results);
            }
            
            // 3. 扫描所有包
            for (ResTablePackage pkg : parser.getPackages()) {
                scanPackage(pkg, results);
            }
            
            log.info("ARSC扫描完成: 发现{}处", results.size());
            
        } catch (Exception e) {
            log.error("ARSC扫描失败", e);
        }
        
        return results;
    }
    
    /**
     * 扫描全局字符串池
     */
    private void scanGlobalStringPool(ResStringPool pool, List<ScanResult> results) {
        log.debug("扫描全局字符串池: {} 个字符串", pool.getStringCount());
        
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            
            // 1. 语义验证：是否像类名/包名
            if (!looksLikeClassOrPackage(str)) {
                continue;
            }
            
            // 2. 白名单过滤：只处理自有包
            if (!whitelistFilter.shouldReplace(str)) {
                log.trace("跳过系统/三方包: '{}'", str);
                continue;
            }
            
            // 3. 添加到结果
            ScanResult result = new ScanResult.Builder()
                .filePath("resources.arsc")
                .semanticType(ScanResult.SemanticType.ARSC_STRING)
                .location(String.format("globalStringPool[%d]", i))
                .originalValue(str)
                .stringPoolIndex(i)
                .build();
            
            results.add(result);
            log.debug("发现ARSC字符串[{}]: '{}'", i, str);
        }
    }
    
    /**
     * 扫描资源包
     */
    private void scanPackage(ResTablePackage pkg, List<ScanResult> results) {
        log.debug("扫描资源包: {}", pkg);
        
        // 1. 扫描包名
        String packageName = pkg.getName();
        if (whitelistFilter.shouldReplace(packageName)) {
            ScanResult result = new ScanResult.Builder()
                .filePath("resources.arsc")
                .semanticType(ScanResult.SemanticType.PACKAGE_NAME)
                .location(String.format("package[0x%02X].name", pkg.getId()))
                .originalValue(packageName)
                .build();
            
            results.add(result);
            log.debug("发现包名: '{}'", packageName);
        }
        
        // 2. 扫描类型字符串池（通常不包含类名，跳过）
        // 类型字符串池存储的是"attr", "layout", "string"等类型名，不是类名
        
        // 3. 扫描资源名字符串池
        if (pkg.getKeyStrings() != null) {
            scanKeyStringPool(pkg.getKeyStrings(), pkg.getId(), results);
        }
    }
    
    /**
     * 扫描资源名字符串池
     */
    private void scanKeyStringPool(ResStringPool pool, int packageId, 
                                   List<ScanResult> results) {
        log.debug("扫描资源名字符串池: {} 个资源", pool.getStringCount());
        
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            
            // 资源名通常是"app_name", "main_activity"等，不是类名
            // 但如果确实包含类名前缀，也应该扫描
            
            if (looksLikeClassOrPackage(str) && whitelistFilter.shouldReplace(str)) {
                ScanResult result = new ScanResult.Builder()
                    .filePath("resources.arsc")
                    .semanticType(ScanResult.SemanticType.ARSC_STRING)
                    .location(String.format("package[0x%02X].keyStrings[%d]", packageId, i))
                    .originalValue(str)
                    .stringPoolIndex(i)
                    .build();
                
                results.add(result);
                log.debug("发现资源名字符串[{}]: '{}'", i, str);
            }
        }
    }
    
    /**
     * 判断字符串是否看起来像类名或包名
     */
    private boolean looksLikeClassOrPackage(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // 1. 至少包含一个点号
        if (!str.contains(".")) {
            return false;
        }
        
        // 2. 不能是资源引用
        if (str.startsWith("@")) {
            return false;
        }
        
        // 3. 不能包含非Java标识符字符
        if (str.contains("/") || str.contains("-") || str.contains(":") || 
            str.contains(" ") || str.contains("=")) {
            return false;
        }
        
        // 4. 符合Java标识符规范
        String[] parts = str.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        
        for (String part : parts) {
            if (part.isEmpty() || !isValidJavaIdentifier(part)) {
                return false;
            }
        }
        
        // 5. 检查是否为自有包前缀
        for (String ownPrefix : ownPackagePrefixes) {
            if (str.startsWith(ownPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证是否为有效的Java标识符
     */
    private boolean isValidJavaIdentifier(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
}

