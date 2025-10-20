package com.resources.arsc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ModifiedUTF8 工具类测试
 * 
 * 测试范围：
 * - NULL字符处理
 * - 中文字符编解码
 * - Emoji字符处理
 * - 长字符串处理
 * - 边界情况
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ModifiedUTF8Test {
    
    @Test
    public void testNullCharacter() throws Exception {
        String input = "Test\u0000String";
        byte[] encoded = ModifiedUTF8.encode(input);
        
        // 验证NULL字符编码为 0xC0 0x80
        boolean found = false;
        for (int i = 0; i < encoded.length - 1; i++) {
            if (encoded[i] == (byte)0xC0 && encoded[i+1] == (byte)0x80) {
                found = true;
                break;
            }
        }
        assertTrue(found, "NULL字符应编码为 0xC0 0x80");
        
        String decoded = ModifiedUTF8.decode(encoded);
        assertEquals(input, decoded, "解码后应与原始字符串相同");
    }
    
    @Test
    public void testChineseCharacters() throws Exception {
        String chinese = "你好世界";
        byte[] encoded = ModifiedUTF8.encode(chinese);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(chinese, decoded, "中文字符应正确编解码");
        
        // 验证字符数
        int charCount = ModifiedUTF8.countCharacters(encoded);
        assertEquals(4, charCount, "4个中文字符应计数为4");
        
        // 验证字节长度（每个中文字符3字节）
        assertEquals(12, encoded.length, "4个中文字符应占12字节");
    }
    
    @Test
    public void testEmoji() throws Exception {
        // 😀 是代理对 (U+1F600 = D83D DE00)
        String emoji = "Hello😀World";
        byte[] encoded = ModifiedUTF8.encode(emoji);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(emoji, decoded, "Emoji应正确编解码");
        
        // 验证: Hello(5) + 😀(2个char,各3字节=6字节) + World(5) = 16字节
        assertTrue(encoded.length >= 16, "Emoji编码后应包含代理对");
    }
    
    @Test
    public void testLongChineseString() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("中");
        }
        String longStr = sb.toString();
        
        byte[] encoded = ModifiedUTF8.encode(longStr);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(longStr, decoded, "长中文字符串应正确编解码");
        
        int charCount = ModifiedUTF8.countCharacters(encoded);
        assertEquals(200, charCount, "200个中文字符应计数为200");
        
        // 验证字节长度（每个中文字符3字节）
        assertEquals(600, encoded.length, "200个中文字符应占600字节");
    }
    
    @Test
    public void testASCIIString() throws Exception {
        String ascii = "Hello World 123";
        byte[] encoded = ModifiedUTF8.encode(ascii);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(ascii, decoded, "ASCII字符串应正确编解码");
        
        // ASCII字符应为1字节编码
        assertEquals(ascii.length(), encoded.length, "ASCII应为1字节/字符");
        
        int charCount = ModifiedUTF8.countCharacters(encoded);
        assertEquals(ascii.length(), charCount, "ASCII字符数应等于长度");
    }
    
    @Test
    public void testMixedString() throws Exception {
        String mixed = "Hello世界😀Test中文";
        byte[] encoded = ModifiedUTF8.encode(mixed);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(mixed, decoded, "混合字符串应正确编解码");
    }
    
    @Test
    public void testEmptyString() throws Exception {
        String empty = "";
        byte[] encoded = ModifiedUTF8.encode(empty);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(empty, decoded, "空字符串应正确处理");
        assertEquals(0, encoded.length, "空字符串编码长度应为0");
        assertEquals(0, ModifiedUTF8.countCharacters(encoded), "空字符串字符数应为0");
    }
    
    @Test
    public void testTwoByteCharacters() throws Exception {
        // 测试2字节UTF-8字符 (U+0080 - U+07FF)
        String twoBytes = "ñáéíó"; // 西班牙语字符
        byte[] encoded = ModifiedUTF8.encode(twoBytes);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(twoBytes, decoded, "2字节字符应正确编解码");
        
        int charCount = ModifiedUTF8.countCharacters(encoded);
        assertEquals(5, charCount, "5个2字节字符应计数为5");
    }
    
    @Test
    public void testNullInput() {
        assertThrows(NullPointerException.class, () -> {
            ModifiedUTF8.encode(null);
        }, "null输入应抛出NullPointerException");
        
        assertThrows(NullPointerException.class, () -> {
            ModifiedUTF8.decode(null);
        }, "null输入应抛出NullPointerException");
    }
    
    @Test
    public void testInvalidRange() {
        byte[] bytes = new byte[10];
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ModifiedUTF8.decode(bytes, -1, 5);
        }, "负数偏移应抛出异常");
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ModifiedUTF8.decode(bytes, 0, 20);
        }, "超出范围应抛出异常");
    }
    
    @Test
    public void testCountCharactersEdgeCases() {
        // NULL输入
        assertEquals(0, ModifiedUTF8.countCharacters(null), "null应返回0");
        
        // 空数组
        assertEquals(0, ModifiedUTF8.countCharacters(new byte[0]), "空数组应返回0");
        
        // 单字节
        byte[] singleByte = {0x41}; // 'A'
        assertEquals(1, ModifiedUTF8.countCharacters(singleByte), "单字节应计数为1");
        
        // 多字节字符
        byte[] multiBytes = {
            (byte)0xE4, (byte)0xB8, (byte)0xAD  // "中" (U+4E2D)
        };
        assertEquals(1, ModifiedUTF8.countCharacters(multiBytes), "3字节字符应计数为1");
    }
    
    @Test
    public void testPackageNameEncoding() throws Exception {
        // 实际场景：包名编码
        String packageName = "com.example.测试应用";
        byte[] encoded = ModifiedUTF8.encode(packageName);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(packageName, decoded, "包名应正确编解码");
        
        // 验证字符数
        int charCount = ModifiedUTF8.countCharacters(encoded);
        assertTrue(charCount > 0, "包名字符数应大于0");
    }
    
    @Test
    public void testClassNameEncoding() throws Exception {
        // 实际场景：类名编码
        String className = "com.example.MyView中文类名";
        byte[] encoded = ModifiedUTF8.encode(className);
        String decoded = ModifiedUTF8.decode(encoded);
        
        assertEquals(className, decoded, "类名应正确编解码");
    }
}

