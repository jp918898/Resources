package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * resources.arsc 完整解析器
 * 
 * 解析AAPT2生成的完整resources.arsc文件，包括：
 * - ResTable header
 * - 全局字符串池
 * - 资源包（一个或多个）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscParser {
    
    private static final Logger log = LoggerFactory.getLogger(ArscParser.class);
    
    // Chunk类型常量
    public static final int RES_TABLE_TYPE = 0x0002;
    
    // 数据成员
    private int packageCount;                    // 包数量
    private ResStringPool globalStringPool;      // 全局字符串池
    private List<ResTablePackage> packages;      // 资源包列表
    
    // 原始数据
    private byte[] originalData;
    
    public ArscParser() {
        this.packages = new ArrayList<>();
    }
    
    /**
     * 解析resources.arsc文件
     * 
     * @param data resources.arsc文件的完整字节数据
     * @throws IllegalArgumentException 解析失败
     */
    public void parse(byte[] data) throws IllegalArgumentException {
        Objects.requireNonNull(data, "data不能为null");
        
        if (data.length == 0) {
            throw new IllegalArgumentException("ARSC数据为空");
        }
        
        if (data.length < 12) {
            throw new IllegalArgumentException(
                String.format("ARSC数据太小: 至少需要12字节（ResTable头部），实际%d字节", 
                            data.length));
        }
        
        this.originalData = data;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        
        try {
            log.info("开始解析resources.arsc: {} 字节", data.length);
            
            // 1. 读取ResTable头部（添加边界检查）
            if (buffer.remaining() < 12) {
                throw new IllegalArgumentException(
                    String.format("无法读取ResTable头部: 需要12字节，剩余%d字节", 
                                buffer.remaining()));
            }
            
            int type = buffer.getShort() & 0xFFFF;
            int headerSize = buffer.getShort() & 0xFFFF;
            int fileSize = buffer.getInt();
            
            if (type != RES_TABLE_TYPE) {
                throw new IllegalArgumentException(
                    String.format("无效的文件类型: 期望0x%04X，实际0x%04X", 
                                 RES_TABLE_TYPE, type));
            }
            
            if (fileSize != data.length) {
                log.warn("文件大小不匹配: header={}, actual={}", fileSize, data.length);
            }
            
            // 2. 读取包数量
            this.packageCount = buffer.getInt();
            
            log.debug("ResTable: headerSize={}, fileSize={}, packageCount={}", 
                     headerSize, fileSize, packageCount);
            
            // 3. 解析所有chunk
            while (buffer.position() < fileSize) {
                // 防御#1: 检查是否有足够字节读取chunk头部
                if (buffer.remaining() < 2) {
                    log.warn("剩余字节不足2，停止解析。位置: {}, 文件大小: {}", 
                            buffer.position(), fileSize);
                    break;
                }
                
                int chunkStartPos = buffer.position();
                
                // 读取chunk类型（但不移动position）
                buffer.mark();
                int chunkType = buffer.getShort() & 0xFFFF;
                buffer.reset();
                
                log.debug("发现chunk: type=0x{}, position={}", 
                         Integer.toHexString(chunkType), chunkStartPos);
                
                switch (chunkType) {
                    case ResStringPool.RES_STRING_POOL_TYPE:
                        // 全局字符串池
                        if (globalStringPool == null) {
                            globalStringPool = new ResStringPool();
                            globalStringPool.parse(buffer);
                            log.info("全局字符串池解析完成: {} 个字符串", 
                                    globalStringPool.getStringCount());
                        } else {
                            // 跳过其他字符串池（可能是包内的）
                            skipChunk(buffer);
                        }
                        break;
                        
                    case ResTablePackage.RES_TABLE_PACKAGE_TYPE:
                        // 资源包
                        ResTablePackage pkg = new ResTablePackage();
                        pkg.parse(buffer);
                        packages.add(pkg);
                        log.info("资源包解析完成: {}", pkg);
                        break;
                        
                    default:
                        // 未知chunk，跳过
                        log.debug("跳过未知chunk: type=0x{}", Integer.toHexString(chunkType));
                        skipChunk(buffer);
                        break;
                }
            }
            
            log.info("resources.arsc解析完成: {} 个包, 全局字符串池={}", 
                    packages.size(), 
                    globalStringPool != null ? globalStringPool.getStringCount() : 0);
            
            // 4. 验证
            if (packages.size() != packageCount) {
                log.warn("包数量不匹配: header={}, actual={}", packageCount, packages.size());
            }
            
        } catch (Exception e) {
            log.error("resources.arsc解析失败", e);
            throw new IllegalArgumentException("resources.arsc解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 跳过当前chunk
     */
    private void skipChunk(ByteBuffer buffer) {
        int currentPos = buffer.position();
        
        // 边界检查：至少需要8字节读取chunk头部
        if (buffer.remaining() < 8) {
            throw new IllegalArgumentException(
                String.format("无法读取chunk头部: position=%d, remaining=%d, 需要8字节",
                            currentPos, buffer.remaining()));
        }
        
        int type = buffer.getShort() & 0xFFFF;
        buffer.getShort(); // headerSize (未使用)
        int chunkSize = buffer.getInt();
        
        // 验证chunk大小合理性
        if (chunkSize < 8) {
            throw new IllegalArgumentException(
                String.format("无效的chunk大小: type=0x%04X, size=%d（至少需要8字节）",
                            type, chunkSize));
        }
        
        // 计算目标位置
        int targetPos = currentPos + chunkSize;
        
        // 边界检查：目标位置不能超过buffer容量
        if (targetPos > buffer.limit()) {
            throw new IllegalArgumentException(
                String.format("chunk超出边界: type=0x%04X, size=%d, position=%d, limit=%d",
                            type, chunkSize, currentPos, buffer.limit()));
        }
        
        // 移动到chunk末尾
        buffer.position(targetPos);
        
        log.trace("跳过chunk: type=0x{}, size={}", Integer.toHexString(type), chunkSize);
    }
    
    /**
     * 查找包含指定模式的字符串索引
     * 
     * @param pattern 搜索模式（可以是简单字符串或正则表达式）
     * @return 字符串索引列表
     */
    public List<Integer> findStringIndices(String pattern) {
        Objects.requireNonNull(pattern, "pattern不能为null");
        
        List<Integer> indices = new ArrayList<>();
        
        if (globalStringPool != null) {
            for (int i = 0; i < globalStringPool.getStringCount(); i++) {
                String str = globalStringPool.getString(i);
                if (str.contains(pattern) || str.matches(pattern)) {
                    indices.add(i);
                }
            }
        }
        
        log.debug("找到 {} 个匹配字符串: pattern='{}'", indices.size(), pattern);
        return indices;
    }
    
    /**
     * 查找包含指定前缀的字符串
     * 
     * @param prefix 前缀
     * @return 匹配的字符串及其索引
     */
    public Map<Integer, String> findStringsByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix不能为null");
        
        Map<Integer, String> results = new LinkedHashMap<>();
        
        if (globalStringPool != null) {
            for (int i = 0; i < globalStringPool.getStringCount(); i++) {
                String str = globalStringPool.getString(i);
                if (str.startsWith(prefix)) {
                    results.put(i, str);
                }
            }
        }
        
        log.debug("找到 {} 个前缀匹配字符串: prefix='{}'", results.size(), prefix);
        return results;
    }
    
    /**
     * 获取主资源包（通常是packageId=0x7f的包）
     * 
     * @return 主资源包，如果没有则返回null
     */
    public ResTablePackage getMainPackage() {
        for (ResTablePackage pkg : packages) {
            if (pkg.getId() == 0x7f) {
                return pkg;
            }
        }
        
        // 如果没有0x7f，返回第一个包
        return packages.isEmpty() ? null : packages.get(0);
    }
    
    /**
     * 根据packageId获取资源包
     * 
     * @param packageId 包ID
     * @return 资源包，如果没有则返回null
     */
    public ResTablePackage getPackageById(int packageId) {
        for (ResTablePackage pkg : packages) {
            if (pkg.getId() == packageId) {
                return pkg;
            }
        }
        return null;
    }
    
    /**
     * 根据包名获取资源包
     * 
     * @param packageName 包名
     * @return 资源包，如果没有则返回null
     */
    public ResTablePackage getPackageByName(String packageName) {
        Objects.requireNonNull(packageName, "packageName不能为null");
        
        for (ResTablePackage pkg : packages) {
            if (packageName.equals(pkg.getName())) {
                return pkg;
            }
        }
        return null;
    }
    
    /**
     * 验证ARSC完整性
     * 
     * @return true=有效, false=无效
     */
    public boolean validate() {
        try {
            // 1. 检查全局字符串池
            if (globalStringPool != null && !globalStringPool.validate()) {
                log.error("全局字符串池验证失败");
                return false;
            }
            
            // 2. 检查所有包
            for (ResTablePackage pkg : packages) {
                if (!pkg.validate()) {
                    log.error("资源包验证失败: {}", pkg);
                    return false;
                }
            }
            
            // 3. 检查包数量
            if (packages.size() != packageCount) {
                log.error("包数量不匹配: header={}, actual={}", packageCount, packages.size());
                return false;
            }
            
            log.debug("ARSC验证通过");
            return true;
            
        } catch (Exception e) {
            log.error("ARSC验证失败", e);
            return false;
        }
    }
    
    // Getters
    public ResStringPool getGlobalStringPool() { return globalStringPool; }
    public List<ResTablePackage> getPackages() { return new ArrayList<>(packages); }
    public int getPackageCount() { return packageCount; }
    public byte[] getOriginalData() { return originalData != null ? originalData.clone() : null; }
    
    @Override
    public String toString() {
        return String.format("ArscParser{packages=%d, globalStrings=%d}", 
                           packages.size(), 
                           globalStringPool != null ? globalStringPool.getStringCount() : 0);
    }
}

