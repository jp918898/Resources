package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * resources.arsc 字符串池解析器
 * 
 * 支持：
 * - UTF-8和UTF-16两种编码
 * - 替换字符串（索引不变）
 * - 重新计算偏移
 * - 验证完整性
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResStringPool {
    
    private static final Logger log = LoggerFactory.getLogger(ResStringPool.class);
    
    // 结构常量
    public static final int RES_STRING_POOL_TYPE = 0x0001;
    public static final int UTF8_FLAG = 0x00000100;
    public static final int SORTED_FLAG = 0x00000001;
    
    // 头部大小（固定28字节）
    private static final int HEADER_SIZE = 28;
    
    /**
     * 验证模式枚举
     */
    public enum ValidationMode {
        /** 严格模式：UTF-8字符数不匹配时抛出异常 */
        STRICT,
        /** 宽松模式：自动修正不匹配的字符数 */
        LENIENT,
        /** 警告模式：仅记录警告但继续（默认） */
        WARN
    }
    
    // 数据成员
    private int stringCount;      // 字符串数量
    private int styleCount;       // 样式数量
    private int flags;            // 标志位（UTF8_FLAG, SORTED_FLAG）
    private int stringsStart;     // 字符串数据起始偏移（相对于chunk开始）
    
    private List<String> strings;      // 字符串列表
    private int[] stringOffsets;       // 字符串偏移数组
    private int[] styleOffsets;        // 样式偏移数组（可选）
    
    private boolean isUtf8;            // 是否UTF-8编码
    private boolean isSorted;          // 是否排序
    
    // 验证模式（默认STRICT以确保数据完整性）
    private ValidationMode validationMode = ValidationMode.STRICT;
    
    public ResStringPool() {
        this.strings = new ArrayList<>();
    }
    
    /**
     * 设置验证模式
     * 
     * @param mode 验证模式
     */
    public void setValidationMode(ValidationMode mode) {
        this.validationMode = Objects.requireNonNull(mode, "ValidationMode不能为null");
        log.debug("设置验证模式: {}", mode);
    }
    
    /**
     * 获取当前验证模式
     * 
     * @return 验证模式
     */
    public ValidationMode getValidationMode() {
        return validationMode;
    }
    
    /**
     * 解析字符串池
     * 
     * @param buffer ByteBuffer（position应指向chunk开始）
     * @throws IllegalArgumentException 解析失败
     */
    public void parse(ByteBuffer buffer) throws IllegalArgumentException {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPosition = buffer.position();
        
        try {
            // 边界检查：chunk头部至少需要8字节
            if (buffer.remaining() < 8) {
                throw new IllegalArgumentException(
                    String.format("无法读取字符串池chunk头部: position=%d, remaining=%d, 需要8字节",
                                buffer.position(), buffer.remaining()));
            }
            
            // 1. 读取chunk头部
            int type = buffer.getShort() & 0xFFFF;
            int headerSize = buffer.getShort() & 0xFFFF;
            int chunkSize = buffer.getInt();
            
            if (type != RES_STRING_POOL_TYPE) {
                throw new IllegalArgumentException(
                    String.format("无效的chunk类型: 期望0x%04X，实际0x%04X", 
                                 RES_STRING_POOL_TYPE, type));
            }
            
            log.debug("解析字符串池: headerSize={}, chunkSize={}", headerSize, chunkSize);
            
            // 验证chunk大小
            if (chunkSize < HEADER_SIZE) {
                throw new IllegalArgumentException(
                    String.format("字符串池chunk大小太小: size=%d, 至少需要%d（头部大小）",
                                chunkSize, HEADER_SIZE));
            }
            
            if (chunkSize > buffer.limit() - startPosition) {
                throw new IllegalArgumentException(
                    String.format("字符串池chunk超出buffer: chunkSize=%d, available=%d",
                                chunkSize, buffer.limit() - startPosition));
            }
            
            // 2. 读取字符串池头部（边界检查：需要20字节）
            if (buffer.remaining() < 20) {
                throw new IllegalArgumentException(
                    String.format("无法读取字符串池头部数据: 需要20字节，剩余%d字节",
                                buffer.remaining()));
            }
            
            this.stringCount = buffer.getInt();
            this.styleCount = buffer.getInt();
            this.flags = buffer.getInt();
            this.stringsStart = buffer.getInt();
            buffer.getInt(); // stylesStart (未使用)
            
            // 验证字符串数量合理性
            if (stringCount < 0) {
                throw new IllegalArgumentException("字符串数量为负数: " + stringCount);
            }
            
            if (stringCount > 1000000) { // 1M字符串 = 不合理
                throw new IllegalArgumentException(
                    String.format("字符串数量异常: %d（超过100万，可能数据损坏）", stringCount));
            }
            
            this.isUtf8 = (flags & UTF8_FLAG) != 0;
            this.isSorted = (flags & SORTED_FLAG) != 0;
            
            log.debug("字符串池: count={}, style={}, flags=0x{}, utf8={}, sorted={}", 
                     stringCount, styleCount, Integer.toHexString(flags), isUtf8, isSorted);
            
            // 3. 读取字符串偏移数组（边界检查）
            int offsetsNeeded = stringCount * 4;
            if (buffer.remaining() < offsetsNeeded) {
                throw new IllegalArgumentException(
                    String.format("无法读取字符串偏移数组: 需要%d字节，剩余%d字节",
                                offsetsNeeded, buffer.remaining()));
            }
            
            this.stringOffsets = new int[stringCount];
            for (int i = 0; i < stringCount; i++) {
                stringOffsets[i] = buffer.getInt();
                
                // 验证偏移值合理性
                if (stringOffsets[i] < 0) {
                    throw new IllegalArgumentException(
                        String.format("字符串偏移为负数: index=%d, offset=%d", 
                                    i, stringOffsets[i]));
                }
            }
            
            // 4. 读取样式偏移数组（如果有）
            if (styleCount > 0) {
                int stylesOffsetsNeeded = styleCount * 4;
                if (buffer.remaining() < stylesOffsetsNeeded) {
                    throw new IllegalArgumentException(
                        String.format("无法读取样式偏移数组: 需要%d字节，剩余%d字节",
                                    stylesOffsetsNeeded, buffer.remaining()));
                }
                
                this.styleOffsets = new int[styleCount];
                for (int i = 0; i < styleCount; i++) {
                    styleOffsets[i] = buffer.getInt();
                }
            }
            
            // 5. 读取字符串数据（边界检查）
            this.strings = new ArrayList<>(stringCount);
            int stringsDataPosition = startPosition + stringsStart;
            
            // 验证stringsStart合理性
            if (stringsStart < HEADER_SIZE) {
                throw new IllegalArgumentException(
                    String.format("stringsStart太小: %d（应该>=%d）", stringsStart, HEADER_SIZE));
            }
            
            if (stringsDataPosition >= buffer.limit()) {
                throw new IllegalArgumentException(
                    String.format("stringsStart超出buffer: position=%d, limit=%d",
                                stringsDataPosition, buffer.limit()));
            }
            
            for (int i = 0; i < stringCount; i++) {
                int stringPosition = stringsDataPosition + stringOffsets[i];
                
                // 验证字符串位置在buffer范围内
                if (stringPosition < 0 || stringPosition >= buffer.limit()) {
                    throw new IllegalArgumentException(
                        String.format("字符串[%d]位置越界: position=%d, limit=%d",
                                    i, stringPosition, buffer.limit()));
                }
                
                buffer.position(stringPosition);
                
                try {
                    String str = isUtf8 ? readUtf8String(buffer) : readUtf16String(buffer);
                    strings.add(str);
                    log.trace("字符串[{}]: '{}' (offset={})", i, str, stringOffsets[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        String.format("读取字符串[%d]失败 at position=%d: %s",
                                    i, stringPosition, e.getMessage()), e);
                }
            }
            
            // 6. 移动position到chunk末尾
            buffer.position(startPosition + chunkSize);
            
            log.info("字符串池解析完成: {} 个字符串, {} 个样式", stringCount, styleCount);
            
        } catch (Exception e) {
            log.error("字符串池解析失败", e);
            throw new IllegalArgumentException("字符串池解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 读取UTF-8字符串
     * 格式：
     * - u8Len (1-2字节): UTF-8字符数
     * - u8Bytes (1-2字节): 字节数
     * - data: MUTF-8数据
     * - 0x00: 终止符
     */
    private String readUtf8String(ByteBuffer buffer) {
        int startPos = buffer.position();
        
        // 读取UTF-8字符数（不要跳过！）
        int charLen = readLength(buffer);
        
        // 读取字节长度
        int byteLen = readLength(buffer);
        
        if (byteLen == 0) {
            // 验证字符数也为0
            if (charLen != 0) {
                log.warn("空字符串但字符数非零: charLen={}", charLen);
            }
            
            // 边界检查：需要读取终止符
            if (buffer.remaining() < 1) {
                throw new IllegalArgumentException(
                    String.format("无法读取空字符串终止符: position=%d, remaining=%d",
                                buffer.position(), buffer.remaining()));
            }
            
            buffer.get(); // 跳过终止符
            return "";
        }
        
        // 验证字节长度合理性
        if (byteLen < 0 || byteLen > 10 * 1024 * 1024) { // 10MB单个字符串 = 不合理
            throw new IllegalArgumentException(
                String.format("UTF-8字节长度异常: %d (position=%d)", byteLen, startPos));
        }
        
        // 边界检查：需要byteLen + 1字节（数据+终止符）
        if (buffer.remaining() < byteLen + 1) {
            throw new IllegalArgumentException(
                String.format("无法读取UTF-8字符串数据: 需要%d字节，剩余%d字节 (position=%d)",
                            byteLen + 1, buffer.remaining(), buffer.position()));
        }
        
        // 读取MUTF-8数据
        byte[] data = new byte[byteLen];
        buffer.get(data);
        
        // 验证终止符
        byte terminator = buffer.get();
        if (terminator != 0) {
            log.warn("UTF-8终止符无效: 0x{} at position={}", 
                    Integer.toHexString(terminator & 0xFF), buffer.position() - 1);
        }
        
        try {
            // 使用MUTF-8解码
            String result = ModifiedUTF8.decode(data);
            
            // 验证字符数并根据验证模式处理
            int actualCharLen = ModifiedUTF8.countCharacters(data);
            if (charLen != actualCharLen) {
                String strPreview = result.length() > 50 ? 
                    result.substring(0, 50) + "..." : result;
                
                switch (validationMode) {
                    case STRICT:
                        throw new IllegalStateException(
                            String.format("UTF-8字符数严格验证失败: header=%d, actual=%d, str='%s'. " +
                                        "请使用setValidationMode(LENIENT)启用自动修正，或WARN仅警告",
                                        charLen, actualCharLen, strPreview));
                    
                    case LENIENT:
                        log.info("UTF-8字符数不匹配（自动修正）: header={}, actual={}, str='{}'", 
                                charLen, actualCharLen, strPreview);
                        // 自动修正：write()时会使用actualCharLen
                        break;
                    
                    case WARN:
                    default:
                        log.warn("UTF-8字符数不匹配: header={}, actual={}, str='{}'", 
                                charLen, actualCharLen, strPreview);
                        break;
                }
            }
            
            return result;
        } catch (IOException e) {
            log.error("MUTF-8解码失败: byteLen={}", byteLen, e);
            // 降级：使用标准UTF-8
            return new String(data, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 读取UTF-16字符串
     * 格式：
     * - u16Len (2-4字节): 字符数
     * - data: UTF-16LE数据
     * - 0x0000: 终止符
     */
    private String readUtf16String(ByteBuffer buffer) {
        int startPos = buffer.position();
        
        // 读取字符长度
        int charLen = readLength16(buffer);
        
        if (charLen == 0) {
            // 边界检查：需要读取终止符（2字节）
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException(
                    String.format("无法读取空UTF-16字符串终止符: position=%d, remaining=%d",
                                buffer.position(), buffer.remaining()));
            }
            buffer.getChar(); // 跳过终止符
            return "";
        }
        
        // 验证字符长度合理性
        if (charLen < 0 || charLen > 5 * 1024 * 1024) { // 5M字符 = 不合理
            throw new IllegalArgumentException(
                String.format("UTF-16字符长度异常: %d (position=%d)", charLen, startPos));
        }
        
        // 边界检查：需要charLen*2 + 2字节（数据+终止符）
        int bytesNeeded = charLen * 2 + 2;
        if (buffer.remaining() < bytesNeeded) {
            throw new IllegalArgumentException(
                String.format("无法读取UTF-16字符串数据: 需要%d字节，剩余%d字节 (position=%d)",
                            bytesNeeded, buffer.remaining(), buffer.position()));
        }
        
        // 读取UTF-16数据
        char[] chars = new char[charLen];
        for (int i = 0; i < charLen; i++) {
            chars[i] = buffer.getChar();
        }
        
        // 跳过终止符
        buffer.getChar();
        
        return new String(chars);
    }
    
    /**
     * 读取长度（1-2字节编码）
     * 如果高位为1，则是2字节编码
     */
    private int readLength(ByteBuffer buffer) {
        if (buffer.remaining() < 1) {
            throw new IllegalArgumentException(
                String.format("无法读取长度: position=%d, remaining=%d, 需要1-2字节",
                            buffer.position(), buffer.remaining()));
        }
        
        int firstByte = buffer.get() & 0xFF;
        if ((firstByte & 0x80) != 0) {
            // 2字节编码 - 需要再读1字节
            if (buffer.remaining() < 1) {
                throw new IllegalArgumentException(
                    String.format("无法读取2字节长度: position=%d, remaining=%d",
                                buffer.position(), buffer.remaining()));
            }
            int secondByte = buffer.get() & 0xFF;
            return ((firstByte & 0x7F) << 8) | secondByte;
        } else {
            // 1字节编码
            return firstByte;
        }
    }
    
    /**
     * 读取UTF-16长度（2-4字节编码）
     */
    private int readLength16(ByteBuffer buffer) {
        if (buffer.remaining() < 2) {
            throw new IllegalArgumentException(
                String.format("无法读取UTF-16长度: position=%d, remaining=%d, 需要2-4字节",
                            buffer.position(), buffer.remaining()));
        }
        
        int firstWord = buffer.getChar();
        if ((firstWord & 0x8000) != 0) {
            // 4字节编码 - 需要再读2字节
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException(
                    String.format("无法读取4字节UTF-16长度: position=%d, remaining=%d",
                                buffer.position(), buffer.remaining()));
            }
            int secondWord = buffer.getChar();
            return ((firstWord & 0x7FFF) << 16) | secondWord;
        } else {
            // 2字节编码
            return firstWord;
        }
    }
    
    /**
     * 替换字符串（索引不变）
     * 
     * @param index 字符串索引
     * @param value 新值
     * @throws IndexOutOfBoundsException 索引越界
     */
    public void setString(int index, String value) {
        Objects.requireNonNull(value, "value不能为null");
        
        if (index < 0 || index >= stringCount) {
            throw new IndexOutOfBoundsException(
                String.format("索引越界: index=%d, size=%d", index, stringCount));
        }
        
        String oldValue = strings.get(index);
        strings.set(index, value);
        
        log.debug("替换字符串[{}]: '{}' -> '{}'", index, oldValue, value);
    }
    
    /**
     * 获取字符串
     */
    public String getString(int index) {
        if (index < 0 || index >= stringCount) {
            throw new IndexOutOfBoundsException(
                String.format("索引越界: index=%d, size=%d", index, stringCount));
        }
        return strings.get(index);
    }
    
    /**
     * 获取所有字符串
     */
    public List<String> getStrings() {
        return new ArrayList<>(strings);
    }
    
    /**
     * 获取字符串数量
     */
    public int getStringCount() {
        return stringCount;
    }
    
    /**
     * 获取样式数量
     */
    public int getStyleCount() {
        return styleCount;
    }
    
    /**
     * 重新计算所有偏移
     * 当字符串长度变化时调用
     */
    public void recalculateOffsets() {
        log.debug("重新计算字符串偏移...");
        
        int currentOffset = 0;
        for (int i = 0; i < stringCount; i++) {
            stringOffsets[i] = currentOffset;
            
            String str = strings.get(i);
            int strSize = calculateStringSize(str, isUtf8);
            currentOffset += strSize;
            
            log.trace("字符串[{}] offset={}, size={}", i, stringOffsets[i], strSize);
        }
        
        log.info("偏移重新计算完成: 总大小={}", currentOffset);
    }
    
    /**
     * 计算字符串编码后的大小
     */
    private int calculateStringSize(String str, boolean utf8) {
        if (utf8) {
            try {
                // 使用MUTF-8编码计算
                byte[] bytes = ModifiedUTF8.encode(str);
                int byteLen = bytes.length;
                int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
                
                // 字符长度编码 + 字节长度编码 + 数据 + 终止符
                int size = 0;
                size += (utf8CharLen >= 0x80) ? 2 : 1;  // 修复：UTF-8字符数
                size += (byteLen >= 0x80) ? 2 : 1;      // 字节长度
                size += byteLen;                         // 数据
                size += 1;                               // 终止符
                return size;
            } catch (IOException e) {
                log.error("计算MUTF-8大小失败", e);
                // 降级方案
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                int byteLen = bytes.length;
                int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
                
                int size = 0;
                size += (utf8CharLen >= 0x80) ? 2 : 1;
                size += (byteLen >= 0x80) ? 2 : 1;
                size += byteLen;
                size += 1;
                return size;
            }
        } else {
            // UTF-16逻辑保持不变
            int charLen = str.length();
            
            // 字符长度编码 + 数据 + 终止符
            int size = 0;
            size += (charLen >= 0x8000) ? 4 : 2; // charLen编码
            size += charLen * 2;                  // 数据（UTF-16）
            size += 2;                            // 终止符
            return size;
        }
    }
    
    /**
     * 写入字符串池到ByteBuffer
     * 
     * @param buffer ByteBuffer
     * @return 写入的字节数
     */
    public int write(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPosition = buffer.position();
        
        // 1. 重新计算偏移
        recalculateOffsets();
        
        // 2. 计算chunk大小
        int headerSize = HEADER_SIZE;
        int offsetsSize = stringCount * 4;
        int stylesOffsetsSize = styleCount * 4;
        
        int stringsDataSize = 0;
        for (int i = 0; i < stringCount; i++) {
            stringsDataSize += calculateStringSize(strings.get(i), isUtf8);
        }
        
        // 对齐到4字节
        int padding = (stringsDataSize % 4 == 0) ? 0 : (4 - stringsDataSize % 4);
        
        int chunkSize = headerSize + offsetsSize + stylesOffsetsSize + stringsDataSize + padding;
        
        log.debug("写入字符串池: chunkSize={}, strings={}, padding={}", 
                 chunkSize, stringsDataSize, padding);
        
        // 3. 写入头部
        buffer.putShort((short) RES_STRING_POOL_TYPE);
        buffer.putShort((short) headerSize);
        buffer.putInt(chunkSize);
        buffer.putInt(stringCount);
        buffer.putInt(styleCount);
        buffer.putInt(flags);
        buffer.putInt(headerSize + offsetsSize + stylesOffsetsSize); // stringsStart
        buffer.putInt(styleCount > 0 ? (headerSize + offsetsSize + stylesOffsetsSize + stringsDataSize + padding) : 0); // stylesStart
        
        // 4. 写入字符串偏移
        for (int offset : stringOffsets) {
            buffer.putInt(offset);
        }
        
        // 5. 写入样式偏移（如果有）
        if (styleCount > 0 && styleOffsets != null) {
            for (int offset : styleOffsets) {
                buffer.putInt(offset);
            }
        }
        
        // 6. 写入字符串数据
        for (String str : strings) {
            if (isUtf8) {
                writeUtf8String(buffer, str);
            } else {
                writeUtf16String(buffer, str);
            }
        }
        
        // 7. 写入padding
        for (int i = 0; i < padding; i++) {
            buffer.put((byte) 0);
        }
        
        int bytesWritten = buffer.position() - startPosition;
        log.info("字符串池写入完成: {} 字节", bytesWritten);
        
        return bytesWritten;
    }
    
    /**
     * 写入UTF-8字符串
     */
    private void writeUtf8String(ByteBuffer buffer, String str) {
        try {
            // 使用MUTF-8编码
            byte[] bytes = ModifiedUTF8.encode(str);
            int byteLen = bytes.length;
            int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
            
            // 写入UTF-8字符长度（不是Java char数量！）
            writeLength(buffer, utf8CharLen);
            
            // 写入字节长度
            writeLength(buffer, byteLen);
            
            // 写入数据
            buffer.put(bytes);
            
            // 写入终止符
            buffer.put((byte) 0);
            
        } catch (IOException e) {
            log.error("MUTF-8编码失败: str='{}'", str, e);
            // 降级：使用标准UTF-8
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            int byteLen = bytes.length;
            int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
            
            writeLength(buffer, utf8CharLen);
            writeLength(buffer, byteLen);
            buffer.put(bytes);
            buffer.put((byte) 0);
        }
    }
    
    /**
     * 写入UTF-16字符串
     */
    private void writeUtf16String(ByteBuffer buffer, String str) {
        int charLen = str.length();
        
        // 写入字符长度
        writeLength16(buffer, charLen);
        
        // 写入数据
        for (int i = 0; i < charLen; i++) {
            buffer.putChar(str.charAt(i));
        }
        
        // 写入终止符
        buffer.putChar((char) 0);
    }
    
    /**
     * 写入长度（1-2字节编码）
     */
    private void writeLength(ByteBuffer buffer, int length) {
        if (length >= 0x80) {
            // 2字节编码
            buffer.put((byte) (0x80 | ((length >> 8) & 0x7F)));
            buffer.put((byte) (length & 0xFF));
        } else {
            // 1字节编码
            buffer.put((byte) length);
        }
    }
    
    /**
     * 写入UTF-16长度（2-4字节编码）
     */
    private void writeLength16(ByteBuffer buffer, int length) {
        if (length >= 0x8000) {
            // 4字节编码
            buffer.putChar((char) (0x8000 | ((length >> 16) & 0x7FFF)));
            buffer.putChar((char) (length & 0xFFFF));
        } else {
            // 2字节编码
            buffer.putChar((char) length);
        }
    }
    
    /**
     * 验证字符串池完整性
     * 
     * @return true=有效, false=无效
     */
    public boolean validate() {
        try {
            // 1. 检查字符串数量
            if (stringCount < 0 || stringCount != strings.size()) {
                log.error("字符串数量不匹配: count={}, actual={}", stringCount, strings.size());
                return false;
            }
            
            // 2. 检查偏移数组长度
            if (stringOffsets == null || stringOffsets.length != stringCount) {
                log.error("偏移数组长度不匹配: count={}, array={}", 
                         stringCount, stringOffsets != null ? stringOffsets.length : 0);
                return false;
            }
            
            // 3. 检查所有字符串非null
            for (int i = 0; i < stringCount; i++) {
                if (strings.get(i) == null) {
                    log.error("字符串[{}]为null", i);
                    return false;
                }
            }
            
            log.debug("字符串池验证通过");
            return true;
            
        } catch (Exception e) {
            log.error("字符串池验证失败", e);
            return false;
        }
    }
    
    /**
     * 是否UTF-8编码
     */
    public boolean isUtf8() {
        return isUtf8;
    }
    
    /**
     * 是否排序
     */
    public boolean isSorted() {
        return isSorted;
    }
    
    @Override
    public String toString() {
        return String.format("ResStringPool{count=%d, utf8=%b, sorted=%b}", 
                           stringCount, isUtf8, isSorted);
    }
}

