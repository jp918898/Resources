package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * ARSC写入器 - 生成修改后的resources.arsc文件
 * 
 * 功能：
 * - 将ArscParser生成的结构写入字节数组
 * - 严格按照AAPT2二进制格式规范
 * - 写入后立即验证
 * - 支持写入文件
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscWriter {
    
    private static final Logger log = LoggerFactory.getLogger(ArscWriter.class);
    
    // Chunk类型常量
    private static final int RES_TABLE_TYPE = 0x0002;
    
    // 安全边界百分比（防止大小计算误差）
    private static final double SAFETY_MARGIN = 0.10; // 10%
    
    // 是否启用严格大小验证
    private boolean strictSizeValidation = true;
    
    /**
     * 将ArscParser转换为字节数组
     * 
     * @param parser ARSC解析器
     * @return 完整的resources.arsc字节数据
     * @throws IllegalStateException 写入失败
     */
    public byte[] toByteArray(ArscParser parser) throws IllegalStateException {
        Objects.requireNonNull(parser, "parser不能为null");
        
        try {
            log.info("开始生成resources.arsc字节数据");
            
            // 1. 计算总大小（精确计算）
            int calculatedSize = calculateTotalSize(parser);
            
            // 2. 添加安全边界（防止大小计算误差）
            int safetyBytes = (int) (calculatedSize * SAFETY_MARGIN);
            int bufferSize = calculatedSize + safetyBytes;
            
            log.debug("预计总大小: {} 字节, 安全边界: {} 字节, Buffer: {} 字节", 
                    calculatedSize, safetyBytes, bufferSize);
            
            // 3. 创建ByteBuffer（添加边界保护）
            ByteBuffer buffer;
            try {
                buffer = ByteBuffer.allocate(bufferSize);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            } catch (OutOfMemoryError e) {
                throw new IllegalStateException(
                    String.format("无法分配ByteBuffer: 需要%d MB内存", 
                                bufferSize / 1024 / 1024), e);
            }
            
            // 4. 写入ResTable头部（占位符，稍后更新实际大小）
            int headerPosition = buffer.position();
            writeResTableHeader(buffer, parser, calculatedSize); // 先写预计大小
            
            // 5. 写入全局字符串池
            if (parser.getGlobalStringPool() != null) {
                int poolBefore = buffer.position();
                try {
                    parser.getGlobalStringPool().write(buffer);
                } catch (Exception e) {
                    throw new IllegalStateException(
                        String.format("写入全局字符串池失败 at position=%d", poolBefore), e);
                }
            }
            
            // 6. 写入所有资源包
            for (int i = 0; i < parser.getPackages().size(); i++) {
                ResTablePackage pkg = parser.getPackages().get(i);
                int pkgBefore = buffer.position();
                try {
                    pkg.write(buffer);
                } catch (Exception e) {
                    throw new IllegalStateException(
                        String.format("写入资源包%d失败 at position=%d: %s", 
                                    i, pkgBefore, pkg), e);
                }
            }
            
            // 7. 验证实际大小
            int actualSize = buffer.position();
            int sizeDiff = actualSize - calculatedSize;
            
            if (sizeDiff != 0) {
                String level = Math.abs(sizeDiff) > safetyBytes ? "ERROR" : "WARN";
                String msg = String.format(
                    "[%s] ARSC大小不匹配: 预计=%d, 实际=%d, 差异=%+d (%.2f%%)",
                    level, calculatedSize, actualSize, sizeDiff, 
                    (sizeDiff * 100.0 / calculatedSize));
                
                if (Math.abs(sizeDiff) > safetyBytes && strictSizeValidation) {
                    log.error(msg);
                    throw new IllegalStateException(
                        "ARSC大小差异超过安全边界，可能存在严重错误。" +
                        "请检查ResTablePackage和ResStringPool的write()实现");
                } else {
                    log.warn(msg);
                }
            }
            
            // 8. 返回实际大小的字节数组（去除多余的安全边界）
            byte[] result = new byte[actualSize];
            buffer.position(0);
            buffer.get(result);
            
            // 9. 更新ResTable头部的实际大小
            ByteBuffer resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
            resultBuffer.position(headerPosition + 4); // 跳到size字段
            resultBuffer.putInt(actualSize);
            
            log.info("resources.arsc生成完成: {} 字节 (预计={}, 差异={})", 
                    actualSize, calculatedSize, actualSize - calculatedSize);
            
            return result;
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成resources.arsc失败", e);
            throw new IllegalStateException("生成resources.arsc失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置严格大小验证模式
     * 
     * @param strict true=严格模式（大小差异超限时抛异常），false=宽松模式（仅警告）
     */
    public void setStrictSizeValidation(boolean strict) {
        this.strictSizeValidation = strict;
        log.debug("设置严格大小验证: {}", strict);
    }
    
    /**
     * 写入ResTable头部
     */
    private void writeResTableHeader(ByteBuffer buffer, ArscParser parser, int totalSize) {
        // ResTable_header结构：
        // - type (2 bytes): 0x0002
        // - headerSize (2 bytes): 12
        // - size (4 bytes): totalSize
        // - packageCount (4 bytes): 包数量
        
        buffer.putShort((short) RES_TABLE_TYPE);
        buffer.putShort((short) 12); // header size
        buffer.putInt(totalSize);
        buffer.putInt(parser.getPackageCount());
        
        log.debug("写入ResTable头部: size={}, packageCount={}", 
                 totalSize, parser.getPackageCount());
    }
    
    /**
     * 计算总大小
     */
    private int calculateTotalSize(ArscParser parser) {
        int size = 12; // ResTable_header
        
        // 全局字符串池（精确计算，无临时buffer）
        if (parser.getGlobalStringPool() != null) {
            size += calculateStringPoolSize(parser.getGlobalStringPool());
        }
        
        // 所有资源包（使用原始数据大小）
        for (ResTablePackage pkg : parser.getPackages()) {
            size += estimatePackageSize(pkg);
        }
        
        return size;
    }
    
    /**
     * 精确计算字符串池大小（不使用临时buffer）
     */
    private int calculateStringPoolSize(ResStringPool pool) {
        int headerSize = 28;
        int offsetsSize = pool.getStringCount() * 4;
        int stylesOffsetsSize = pool.getStyleCount() * 4;
        
        int stringsDataSize = 0;
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            stringsDataSize += calculateStringSize(str, pool.isUtf8());
        }
        
        int padding = (stringsDataSize % 4 == 0) ? 0 : (4 - stringsDataSize % 4);
        
        return headerSize + offsetsSize + stylesOffsetsSize + stringsDataSize + padding;
    }
    
    /**
     * 计算单个字符串编码后的大小
     */
    private int calculateStringSize(String str, boolean utf8) {
        if (utf8) {
            try {
                byte[] bytes = ModifiedUTF8.encode(str);
                int byteLen = bytes.length;
                int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
                
                int size = 0;
                size += (utf8CharLen >= 0x80) ? 2 : 1;  // UTF-8字符数编码
                size += (byteLen >= 0x80) ? 2 : 1;      // 字节长度编码
                size += byteLen;                         // 数据
                size += 1;                               // 终止符
                return size;
            } catch (IOException e) {
                throw new IllegalStateException("MUTF-8编码失败: " + str, e);
            }
        } else {
            // UTF-16
            int charLen = str.length();
            
            int size = 0;
            size += (charLen >= 0x8000) ? 4 : 2;  // charLen编码
            size += charLen * 2;                   // 数据（UTF-16）
            size += 2;                             // 终止符
            return size;
        }
    }
    
    /**
     * 估算包大小（支持重建模式）
     */
    private int estimatePackageSize(ResTablePackage pkg) {
        if (pkg.needsRebuild()) {
            // 重建模式：使用calculateRebuildSize()精确计算
            int rebuildSize = pkg.calculateRebuildSize();
            log.debug("Package需要重建: packageId=0x{}, 大小={}", 
                     Integer.toHexString(pkg.getId()), rebuildSize);
            return rebuildSize;
        } else {
            // 原始模式：使用解析时保存的原始大小
            // 这与writeWithOriginalData()的行为完全匹配
            return pkg.getOriginalSize();
        }
    }
    
    /**
     * 验证生成的ARSC数据
     * 
     * @param data ARSC字节数据
     * @return true=有效, false=无效
     */
    public boolean validate(byte[] data) {
        Objects.requireNonNull(data, "data不能为null");
        
        try {
            log.debug("验证生成的resources.arsc数据");
            
            // 1. 尝试重新解析
            ArscParser validator = new ArscParser();
            validator.parse(data);
            
            // 2. 验证解析结果
            boolean valid = validator.validate();
            
            if (valid) {
                log.info("ARSC数据验证通过");
            } else {
                log.error("ARSC数据验证失败");
            }
            
            return valid;
            
        } catch (Exception e) {
            log.error("ARSC数据验证失败", e);
            return false;
        }
    }
    
    /**
     * 写入文件
     * 
     * @param path 文件路径
     * @param data ARSC字节数据
     * @throws IOException 写入失败
     */
    public void writeToFile(String path, byte[] data) throws IOException {
        Objects.requireNonNull(path, "path不能为null");
        Objects.requireNonNull(data, "data不能为null");
        
        log.info("写入resources.arsc到文件: {}", path);
        
        Files.write(Paths.get(path), data);
        
        log.info("文件写入完成: {} 字节", data.length);
    }
    
    /**
     * 直接从ArscParser写入文件（便捷方法）
     * 
     * @param parser ARSC解析器
     * @param path 文件路径
     * @throws IOException 写入失败
     */
    public void writeToFile(ArscParser parser, String path) throws IOException {
        byte[] data = toByteArray(parser);
        
        // 验证
        if (!validate(data)) {
            throw new IOException("生成的ARSC数据验证失败");
        }
        
        writeToFile(path, data);
    }
}

