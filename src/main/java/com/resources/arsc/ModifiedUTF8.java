package com.resources.arsc;

import java.io.IOException;
import java.io.UTFDataFormatException;

/**
 * Modified UTF-8 (MUTF-8) 编解码工具
 * 
 * 基于 Java DataOutputStream.writeUTF() 的实现
 * 符合 Android ARSC 规范
 * 
 * MUTF-8 vs UTF-8 差异:
 * 1. NULL字符(U+0000): MUTF-8使用 0xC0 0x80, UTF-8使用 0x00
 * 2. 代理对(U+10000-U+10FFFF): MUTF-8编码为6字节(2个3字节序列)
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ModifiedUTF8 {
    
    /**
     * 将字符串编码为 MUTF-8 字节数组
     * 参考 java.io.DataOutputStream.writeUTF() 实现
     * 
     * MUTF-8编码规则：
     * - NULL(U+0000): 0xC0 0x80 (2字节)
     * - U+0001-U+007F: 0xxxxxxx (1字节)
     * - U+0080-U+07FF: 110xxxxx 10xxxxxx (2字节)
     * - U+0800-U+FFFF: 1110xxxx 10xxxxxx 10xxxxxx (3字节)
     * - U+10000-U+10FFFF: 代理对，各3字节，共6字节
     * 
     * @param str 待编码的字符串
     * @return MUTF-8编码的字节数组
     * @throws IOException 编码失败
     */
    public static byte[] encode(String str) throws IOException {
        if (str == null) {
            throw new NullPointerException("str不能为null");
        }
        
        int strlen = str.length();
        int utflen = 0;
        
        // 1. 计算MUTF-8编码后的字节长度（一次遍历）
        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            if (c == 0) {
                // NULL: 2字节 (0xC0 0x80)
                utflen += 2;
            } else if (c >= 0x0001 && c <= 0x007F) {
                // ASCII: 1字节
                utflen += 1;
            } else if (c <= 0x07FF) {
                // 2字节字符
                utflen += 2;
            } else if (Character.isHighSurrogate((char) c)) {
                // 高代理（U+D800-U+DBFF）- 处理代理对
                // MUTF-8编码代理对：每个代理字符单独编码为3字节，共6字节
                utflen += 3; // 高代理3字节
                if (i + 1 < strlen && Character.isLowSurrogate(str.charAt(i + 1))) {
                    utflen += 3; // 低代理3字节
                    i++; // 跳过低代理
                }
            } else {
                // 3字节字符（BMP字符）
                utflen += 3;
            }
        }
        
        // 2. 编码
        byte[] bytes = new byte[utflen];
        int index = 0;
        
        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            
            if (c == 0) {
                // NULL字符的MUTF-8编码: 0xC0 0x80
                bytes[index++] = (byte) 0xC0;
                bytes[index++] = (byte) 0x80;
            } else if (c >= 0x0001 && c <= 0x007F) {
                // 1字节: 0xxxxxxx
                bytes[index++] = (byte) c;
            } else if (c <= 0x07FF) {
                // 2字节: 110xxxxx 10xxxxxx
                bytes[index++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytes[index++] = (byte) (0x80 | (c & 0x3F));
            } else if (Character.isHighSurrogate((char) c)) {
                // 处理代理对（U+10000-U+10FFFF）
                // MUTF-8将代理对编码为6字节（每个代理字符3字节）
                if (i + 1 < strlen && Character.isLowSurrogate(str.charAt(i + 1))) {
                    // 高代理（U+D800-U+DBFF）
                    bytes[index++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    bytes[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytes[index++] = (byte) (0x80 | (c & 0x3F));
                    
                    // 低代理（U+DC00-U+DFFF）
                    i++;
                    c = str.charAt(i);
                    bytes[index++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    bytes[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytes[index++] = (byte) (0x80 | (c & 0x3F));
                } else {
                    // 孤立的高代理（无效），按3字节编码
                    bytes[index++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    bytes[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytes[index++] = (byte) (0x80 | (c & 0x3F));
                }
            } else {
                // 3字节: 1110xxxx 10xxxxxx 10xxxxxx（BMP字符）
                bytes[index++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytes[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[index++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        
        if (index != utflen) {
            throw new IOException(
                String.format("MUTF-8编码长度不匹配: 预期=%d, 实际=%d", utflen, index));
        }
        
        return bytes;
    }
    
    /**
     * 将 MUTF-8 字节数组解码为字符串
     * 参考 java.io.DataInputStream.readUTF() 实现
     * 
     * @param bytes MUTF-8编码的字节数组
     * @return 解码后的字符串
     * @throws IOException 解码失败
     */
    public static String decode(byte[] bytes) throws IOException {
        return decode(bytes, 0, bytes.length);
    }
    
    /**
     * 将 MUTF-8 字节数组解码为字符串（指定范围）
     * 
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @param length 字节长度
     * @return 解码后的字符串
     * @throws IOException 解码失败
     */
    public static String decode(byte[] bytes, int offset, int length) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("bytes不能为null");
        }
        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            throw new IndexOutOfBoundsException(
                String.format("无效的范围: offset=%d, length=%d, array.length=%d", 
                             offset, length, bytes.length));
        }
        
        char[] chars = new char[length];
        int charCount = 0;
        int byteIndex = offset;
        int byteLimit = offset + length;
        
        while (byteIndex < byteLimit) {
            int b1 = bytes[byteIndex++] & 0xFF;
            
            if ((b1 & 0x80) == 0) {
                // 1字节序列: 0xxxxxxx
                chars[charCount++] = (char) b1;
            } else if ((b1 & 0xE0) == 0xC0) {
                // 2字节序列: 110xxxxx 10xxxxxx
                if (byteIndex >= byteLimit) {
                    throw new UTFDataFormatException("MUTF-8格式错误: 2字节序列不完整");
                }
                int b2 = bytes[byteIndex++] & 0xFF;
                if ((b2 & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("MUTF-8格式错误: 无效的续字节");
                }
                int c = ((b1 & 0x1F) << 6) | (b2 & 0x3F);
                chars[charCount++] = (char) c;
            } else if ((b1 & 0xF0) == 0xE0) {
                // 3字节序列: 1110xxxx 10xxxxxx 10xxxxxx
                if (byteIndex + 1 > byteLimit) {
                    throw new UTFDataFormatException("MUTF-8格式错误: 3字节序列不完整");
                }
                int b2 = bytes[byteIndex++] & 0xFF;
                int b3 = bytes[byteIndex++] & 0xFF;
                if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("MUTF-8格式错误: 无效的续字节");
                }
                int c = ((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
                chars[charCount++] = (char) c;
            } else {
                throw new UTFDataFormatException(
                    String.format("MUTF-8格式错误: 无效的起始字节 0x%02X at position %d", 
                                 b1, byteIndex - 1));
            }
        }
        
        return new String(chars, 0, charCount);
    }
    
    /**
     * 计算MUTF-8编码的字符数
     * 
     * 注意：这里的"字符数"是指UTF-8字符数，不是Java char数量
     * 遍历字节数组，统计非续字节(non-continuation bytes)的数量
     * 
     * UTF-8编码规则：
     * - 起始字节: 0xxxxxxx, 110xxxxx, 1110xxxx, 11110xxx
     * - 续字节: 10xxxxxx
     * 
     * @param utf8Bytes UTF-8/MUTF-8编码的字节数组
     * @return UTF-8字符数
     */
    public static int countCharacters(byte[] utf8Bytes) {
        if (utf8Bytes == null) {
            return 0;
        }
        
        int count = 0;
        for (byte b : utf8Bytes) {
            // UTF-8续字节格式: 10xxxxxx (0x80 - 0xBF)
            // 非续字节：最高位不是10
            if ((b & 0xC0) != 0x80) {
                count++;
            }
        }
        return count;
    }
}

