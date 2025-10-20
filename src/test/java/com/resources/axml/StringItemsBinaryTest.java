package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringItems二进制格式测试
 * 直接检查写入的二进制数据是否符合Android AXML规范
 */
@DisplayName("StringItems二进制格式测试")
public class StringItemsBinaryTest {

    @Test
    @DisplayName("测试单个字符串的二进制格式")
    void testSingleStringBinaryFormat() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("a"));
        
        items.prepare();
        
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        
        // 模拟实际场景：先写chunk header，再写StringPool body
        buffer.putInt(0x00010001);  // chunk type
        buffer.putInt(size + 8);     // chunk size
        
        items.write(buffer);
        
        buffer.flip();
        
        // 跳过chunk header
        buffer.getInt();  // type
        buffer.getInt();  // size
        
        // 读取头部
        int stringCount = buffer.getInt();
        int styleCount = buffer.getInt();
        int flags = buffer.getInt();
        int stringsOffset = buffer.getInt();
        int stylesOffset = buffer.getInt();
        
        System.out.println("头部信息:");
        System.out.println("  stringCount = " + stringCount);
        System.out.println("  styleCount = " + styleCount);
        System.out.println("  flags = 0x" + Integer.toHexString(flags));
        System.out.println("  stringsOffset = " + stringsOffset);
        System.out.println("  stylesOffset = " + stylesOffset);
        
        // 读取字符串偏移
        int offset0 = buffer.getInt();
        System.out.println("  offset[0] = " + offset0);
        
        // 定位到字符串数据
        buffer.position(stringsOffset);
        
        // 读取UTF-8编码的字符串"a"
        // 格式：charLen(1-2字节) + byteLen(1-2字节) + 数据 + 0x00
        
        System.out.println("\n字符串数据（从offset " + stringsOffset + "开始）:");
        
        // 手动读取字节
        byte b0 = buffer.get();
        System.out.println("  byte[0] = 0x" + Integer.toHexString(b0 & 0xFF) + " (" + (b0 & 0xFF) + ")");
        
        byte b1 = buffer.get();
        System.out.println("  byte[1] = 0x" + Integer.toHexString(b1 & 0xFF) + " (" + (b1 & 0xFF) + ")");
        
        byte b2 = buffer.get();
        System.out.println("  byte[2] = 0x" + Integer.toHexString(b2 & 0xFF) + " (" + (char)(b2 & 0xFF) + ")");
        
        byte b3 = buffer.get();
        System.out.println("  byte[3] = 0x" + Integer.toHexString(b3 & 0xFF) + " (终止符)");
        
        // 验证：对于字符串"a"，预期格式应该是：
        // byte[0] = 0x01 (charLen=1)
        // byte[1] = 0x01 (byteLen=1)
        // byte[2] = 0x61 ('a')
        // byte[3] = 0x00 (终止符)
        
        assertEquals(0x01, b0 & 0xFF, "charLen应为1");
        assertEquals(0x01, b1 & 0xFF, "byteLen应为1");
        assertEquals(0x61, b2 & 0xFF, "数据应为'a'(0x61)");
        assertEquals(0x00, b3 & 0xFF, "应有终止符");
    }
}

