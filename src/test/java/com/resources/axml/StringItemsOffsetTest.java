package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringItems偏移量计算测试
 * 验证prepare()方法的offset计算是否正确
 */
@DisplayName("StringItems偏移量计算测试")
public class StringItemsOffsetTest {

    @Test
    @DisplayName("测试简单ASCII字符串的offset计算")
    void testSimpleAsciiOffset() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("a"));
        items.add(new StringItem("ab"));
        items.add(new StringItem("abc"));
        
        items.prepare();
        
        // 验证每个StringItem的dataOffset
        assertEquals(0, items.get(0).dataOffset, "第0个字符串offset应为0");
        
        // 计算第1个字符串的预期offset
        // 第0个字符串"a"的编码：
        // - charLen: 1字节 (0x01)
        // - byteLen: 1字节 (0x01)
        // - 数据: 1字节 ('a')
        // - 终止符: 1字节 (0x00)
        // 总计: 4字节
        assertEquals(4, items.get(1).dataOffset, "第1个字符串offset应为4");
        
        // 第1个字符串"ab"的编码：
        // - charLen: 1字节 (0x02)
        // - byteLen: 1字节 (0x02)
        // - 数据: 2字节 ('ab')
        // - 终止符: 1字节 (0x00)
        // 总计: 5字节
        assertEquals(4 + 5, items.get(2).dataOffset, "第2个字符串offset应为9");
        
        // 写入并读回验证
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putInt(0x00010001);  // chunk type
        buffer.putInt(size + 8);     // chunk size
        
        items.write(buffer);
        
        buffer.flip();
        buffer.getInt();  // skip type
        buffer.getInt();  // skip size
        
        String[] strings = StringItems.read(buffer);
        
        assertEquals(3, strings.length, "应读取3个字符串");
        
        System.out.println("读取结果:");
        for (int i = 0; i < strings.length; i++) {
            System.out.println("  [" + i + "] = '" + strings[i] + "' (length=" + strings[i].length() + ")");
        }
        
        System.out.println("\nStringItem偏移量:");
        for (int i = 0; i < items.size(); i++) {
            System.out.println("  [" + i + "] offset=" + items.get(i).dataOffset + ", data='" + items.get(i).data + "'");
        }
        
        assertEquals("a", strings[0], "第0个字符串");
        assertEquals("ab", strings[1], "第1个字符串");
        assertEquals("abc", strings[2], "第2个字符串");
    }
}

