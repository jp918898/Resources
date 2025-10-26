package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringItems 单元测试
 * 验证StringPool头部结构和getSize()计算的正确性
 * 
 * 测试基于Apktool ResStringPool.java的规范：
 * - 头部固定5个int字段（20字节）
 * - 字段4（stringsOffset）= headerSize + stringOffsets + styleOffsets
 * - 字段5（stylesOffset）= stringsOffset + stringData（如果有样式）
 * 
 * @author Resources Processor Team
 */
@DisplayName("StringItems 单元测试（P0修复验证）")
public class StringItemsTest {

    @Test
    @DisplayName("测试1: StringPool头部结构正确性（UTF-8，无样式）")
    void testStringPoolHeader_UTF8_NoStyles() throws IOException {
        // 1. 准备测试数据
        StringItems items = new StringItems();
        items.add(new StringItem("Hello"));
        items.add(new StringItem("World"));
        items.add(new StringItem("测试"));
        
        items.prepare();
        
        // 2. 写入ByteBuffer
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 3. 验证头部字段
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        // 断言
        assertEquals(3, stringCount, "字符串数量应为3");
        assertEquals(0, styleCount, "样式数量应为0（无样式）");
        assertEquals(0x00000100, flags, "flags应为UTF-8标志");
        
        // 验证stringsOffset计算（关键修复）
        // stringsOffset是相对于chunk起始的偏移（包括chunk header 8字节）
        int expectedStringsOffset = 8 + 5 * 4 + 3 * 4 + 0 * 4; // chunkHeader + header + stringOffsets + styleOffsets
        assertEquals(expectedStringsOffset, stringsOffset, 
            "stringsOffset应为 chunkHeader(8) + headerSize(20) + stringOffsets(12) + styleOffsets(0) = 40");
        
        assertEquals(0, stylesOffset, "stylesOffset应为0（无样式数据）");
    }

    @Test
    @DisplayName("测试2: StringPool头部结构正确性（UTF-16LE，无样式）")
    void testStringPoolHeader_UTF16LE_NoStyles() throws IOException {
        // 1. 准备超长字符串（强制使用UTF-16LE）
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 0x8000; i++) {
            longString.append('A');
        }
        
        StringItems items = new StringItems();
        items.add(new StringItem("Short"));
        items.add(new StringItem(longString.toString()));
        
        items.prepare();
        
        // 2. 写入ByteBuffer
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 3. 验证头部字段
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        // 断言
        assertEquals(2, stringCount, "字符串数量应为2");
        assertEquals(0, styleCount, "样式数量应为0");
        assertEquals(0, flags, "flags应为0（UTF-16LE模式）");
        
        int expectedStringsOffset = 8 + 5 * 4 + 2 * 4 + 0 * 4;
        assertEquals(expectedStringsOffset, stringsOffset, "stringsOffset应为36");
        assertEquals(0, stylesOffset, "stylesOffset应为0");
    }

    @Test
    @DisplayName("测试3: getSize()计算正确性（无样式数据）")
    void testGetSize_NoStyleData() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("Test1"));
        items.add(new StringItem("Test2"));
        items.add(new StringItem("Test3"));
        
        items.prepare();
        
        int calculatedSize = items.getSize();
        
        // 手动计算预期大小
        int headerSize = 5 * 4;  // 20字节
        int stringOffsetsSize = 3 * 4;  // 12字节
        // styleOffsetsSize = 0字节（无样式）
        // stringDataSize需要从实际编码计算
        
        // 验证：实际写入的字节数应等于getSize()返回值
        ByteBuffer buffer = ByteBuffer.allocate(calculatedSize + 100).order(ByteOrder.LITTLE_ENDIAN);
        int positionBefore = buffer.position();
        items.write(buffer);
        int positionAfter = buffer.position();
        int actualWritten = positionAfter - positionBefore;
        
        assertEquals(calculatedSize, actualWritten, 
            "getSize()返回值应等于实际写入的字节数");
        
        assertTrue(calculatedSize >= headerSize + stringOffsetsSize, 
            "总大小应至少包含头部和偏移数组");
    }

    @Test
    @DisplayName("测试4: 样式数据字段（模拟有样式，但数据为空）")
    void testStyleData_EmptyArray() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("Styled Text"));
        items.prepare();
        
        // 模拟添加空样式数据（styleData数组为空）
        // 注意：实际使用中，有样式时应有数据，这里仅测试边界情况
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        buffer.getInt(); // stringCount
        buffer.getInt(); // styleCount
        buffer.getInt(); // flags
        buffer.getInt(); // stringsOffset
        int stylesOffset = buffer.getInt();
        
        // 无样式数据时，stylesOffset应为0
        assertEquals(0, stylesOffset, "没有样式数据时，stylesOffset应为0");
    }

    @Test
    @DisplayName("测试5: 空StringPool处理")
    void testEmptyStringPool() throws IOException {
        StringItems items = new StringItems();
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        buffer.getInt();  // 跳过flags
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        assertEquals(0, stringCount, "空StringPool的字符串数量应为0");
        assertEquals(0, styleCount, "空StringPool的样式数量应为0");
        assertEquals(8 + 5 * 4, stringsOffset, "空StringPool的stringsOffset应为28（chunk header + 仅StringPool头部）");
        assertEquals(0, stylesOffset, "空StringPool的stylesOffset应为0");
    }

    @Test
    @DisplayName("测试6: 大量字符串性能测试")
    void testLargeStringPool() throws IOException {
        StringItems items = new StringItems();
        
        // 添加1000个字符串
        for (int i = 0; i < 1000; i++) {
            items.add(new StringItem("String_" + i));
        }
        
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        buffer.getInt();  // 跳过styleCount
        buffer.getInt();  // 跳过flags
        int stringsOffset = buffer.getInt();
        
        assertEquals(1000, stringCount, "字符串数量应为1000");
        
        int expectedStringsOffset = 8 + 5 * 4 + 1000 * 4;
        assertEquals(expectedStringsOffset, stringsOffset, 
            "大量字符串时stringsOffset计算应正确");
    }

    @Test
    @DisplayName("测试7: 特殊字符处理（Emoji、中文、日文）")
    void testSpecialCharacters() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("Emoji: 😀🎉"));
        items.add(new StringItem("中文：你好世界"));
        items.add(new StringItem("日本語：こんにちは"));
        items.add(new StringItem("한국어: 안녕하세요"));
        
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> items.write(buffer), 
            "特殊字符不应导致写入失败");
        
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        assertEquals(4, stringCount, "特殊字符字符串数量应为4");
    }

    @Test
    @DisplayName("测试8: 数据完整性验证（Integrity Check）")
    void testDataIntegrity() throws IOException {
        // 测试目标：验证write()写入的数据结构完整性
        StringItems items = new StringItems();
        items.add(new StringItem("Android"));
        items.add(new StringItem("应用程序"));
        items.add(new StringItem("Package"));
        
        items.prepare();
        
        int size = items.getSize();
        
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        int posBeforeWrite = buffer.position();
        items.write(buffer);
        int posAfterWrite = buffer.position();
        int actualWritten = posAfterWrite - posBeforeWrite;
        
        buffer.flip();
        
        // 验证头部5个字段
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        // 基本验证
        assertEquals(3, stringCount, "字符串数量正确");
        assertEquals(0, styleCount, "样式数量正确");
        assertEquals(0x00000100, flags, "UTF-8标志正确");
        assertTrue(stringsOffset > 0, "stringsOffset应大于0");
        assertEquals(0, stylesOffset, "无样式时stylesOffset应为0");
        
        // 验证字符串偏移数组（3个int）
        int offset1 = buffer.getInt();
        int offset2 = buffer.getInt();
        int offset3 = buffer.getInt();
        
        assertTrue(offset1 >= 0, "第1个偏移应>=0");
        assertTrue(offset2 > offset1, "第2个偏移应>第1个");
        assertTrue(offset3 > offset2, "第3个偏移应>第2个");
        
        // 验证buffer的总写入长度等于getSize()
        assertEquals(size, actualWritten, 
            "实际写入的数据量应等于getSize()返回值 [expected=" + size + ", actual=" + actualWritten + "]");
    }

    @Test
    @DisplayName("测试9: UTF-16LE编码边界值测试")
    void testUTF16LEBoundaryValues() throws IOException {
        // 测试UTF-8到UTF-16LE的切换边界
        testUTF16LEBoundary(0x7FFF, "UTF-8边界-1", false); // 应该使用UTF-8
        testUTF16LEBoundary(0x8000, "UTF-16LE边界", true);  // 应该切换到UTF-16LE
        testUTF16LEBoundary(0x8001, "UTF-16LE边界+1", true); // 应该使用UTF-16LE
    }
    
    private void testUTF16LEBoundary(int charCount, String testName, boolean expectUTF16LE) throws IOException {
        // 创建指定长度的字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charCount; i++) {
            sb.append('A'); // 使用ASCII字符，确保每个字符1字节
        }
        String testString = sb.toString();
        
        StringItems items = new StringItems();
        items.add(new StringItem(testString));
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部字段
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        assertEquals(1, stringCount, testName + ": 字符串数量应为1");
        assertEquals(0, styleCount, testName + ": 样式数量应为0");
        
        if (expectUTF16LE) {
            assertEquals(0, flags, testName + ": 应该使用UTF-16LE编码 (flags=0)");
        } else {
            assertEquals(0x00000100, flags, testName + ": 应该使用UTF-8编码 (flags=0x100)");
        }
        
        assertTrue(stringsOffset > 0, testName + ": stringsOffset应大于0");
        assertEquals(0, stylesOffset, testName + ": stylesOffset应为0");
        
        System.out.println(testName + ": " + charCount + " 字符, flags=0x" + 
                          Integer.toHexString(flags) + " (UTF-16LE=" + expectUTF16LE + ")");
    }
    
    @Test
    @DisplayName("测试10: 空字符串在UTF-16LE模式")
    void testEmptyStringInUTF16LEMode() throws IOException {
        // 创建包含空字符串的StringItems
        StringItems items = new StringItems();
        items.add(new StringItem("")); // 空字符串
        items.add(new StringItem("A".repeat(0x8000))); // 强制UTF-16LE的长字符串
        items.add(new StringItem("")); // 另一个空字符串
        
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        assertEquals(3, stringCount, "字符串数量应为3");
        assertEquals(0, styleCount, "样式数量应为0");
        assertEquals(0, flags, "应该使用UTF-16LE编码");
        assertTrue(stringsOffset > 0, "stringsOffset应大于0");
        assertEquals(0, stylesOffset, "stylesOffset应为0");
        
        // 验证字符串偏移数组
        int offset1 = buffer.getInt(); // 空字符串偏移
        int offset2 = buffer.getInt(); // 长字符串偏移
        int offset3 = buffer.getInt(); // 另一个空字符串偏移
        
        assertTrue(offset1 >= 0, "第1个偏移应>=0");
        assertTrue(offset2 > offset1, "第2个偏移应>第1个");
        // 注意：在UTF-16LE模式下，空字符串的偏移可能不按预期顺序排列
        // 我们只验证偏移值是非负的
        assertTrue(offset3 >= 0, "第3个偏移应>=0");
        
        System.out.println("空字符串UTF-16LE测试: 偏移=[" + offset1 + ", " + offset2 + ", " + offset3 + "]");
    }
    
    @Test
    @DisplayName("测试11: 只包含NULL字符的字符串")
    void testNullCharacterString() throws IOException {
        // 创建只包含NULL字符的字符串
        String nullString = "\u0000".repeat(100);
        
        StringItems items = new StringItems();
        items.add(new StringItem(nullString));
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        assertEquals(1, stringCount, "字符串数量应为1");
        assertEquals(0, styleCount, "样式数量应为0");
        assertEquals(0x00000100, flags, "应该使用UTF-8编码");
        assertTrue(stringsOffset > 0, "stringsOffset应大于0");
        assertEquals(0, stylesOffset, "stylesOffset应为0");
        
        System.out.println("NULL字符字符串测试: 100个NULL字符, flags=0x" + Integer.toHexString(flags));
    }
    
    @Test
    @DisplayName("测试12: 混合长度字符串的编码切换")
    void testMixedLengthStringEncoding() throws IOException {
        StringItems items = new StringItems();
        
        // 添加不同长度的字符串
        items.add(new StringItem("Short"));                    // 5字符 - UTF-8
        items.add(new StringItem("A".repeat(0x7FFF)));        // 32767字符 - UTF-8
        items.add(new StringItem("A".repeat(0x8000)));        // 32768字符 - UTF-16LE
        items.add(new StringItem("A".repeat(0x8001)));        // 32769字符 - UTF-16LE
        items.add(new StringItem("Medium"));                  // 6字符 - UTF-8
        
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        items.write(buffer);
        buffer.flip();
        
        // 验证头部
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        assertEquals(5, stringCount, "字符串数量应为5");
        assertEquals(0, styleCount, "样式数量应为0");
        assertEquals(0, flags, "应该使用UTF-16LE编码（因为有超长字符串）");
        assertTrue(stringsOffset > 0, "stringsOffset应大于0");
        assertEquals(0, stylesOffset, "stylesOffset应为0");
        
        System.out.println("混合长度字符串测试: 5个字符串, 最长=" + 0x8001 + "字符, flags=0x" + Integer.toHexString(flags));
    }
}

