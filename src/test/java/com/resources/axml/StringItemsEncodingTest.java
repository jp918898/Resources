package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringItems编码格式保持测试
 * 验证UTF-8/UTF-16编码在读取-写入循环中的保持性
 */
@DisplayName("StringItems编码格式保持测试")
public class StringItemsEncodingTest {

    @Test
    @DisplayName("测试UTF-16编码保持：读取UTF-16 AXML，写出后仍为UTF-16")
    void testPreserveUtf16Encoding() throws IOException {
        // 1. 创建UTF-16格式的StringItems
        StringItems items = new StringItems();
        items.add(new StringItem("android"));
        items.add(new StringItem("layout"));
        items.setForceEncoding(false);  // 强制UTF-16
        items.prepare();
        
        // 2. 写入ByteBuffer
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x00010001);  // chunk type
        buffer.putInt(size + 8);     // chunk size
        items.write(buffer);
        
        // 3. 读回并验证flags保持
        buffer.flip();
        buffer.getInt();  // skip type
        buffer.getInt();  // skip size
        
        StringItems receiver = new StringItems();
        String[] strings = StringItems.read(buffer, receiver);
        
        // 验证flags
        assertEquals(0, receiver.getOriginalFlags() & 0x100, "UTF-16格式的flags应为0");
        assertEquals(2, strings.length, "应读取2个字符串");
        assertEquals("android", strings[0], "第0个字符串");
        assertEquals("layout", strings[1], "第1个字符串");
        
        // 4. 再次写入，验证编码保持
        StringItems items2 = new StringItems();
        for (String s : strings) {
            items2.add(new StringItem(s));
        }
        items2.setOriginalFlags(receiver.getOriginalFlags());
        items2.prepare();
        
        // 5. 验证useUTF8=false（通过检查写入的flags）
        ByteBuffer buffer2 = ByteBuffer.allocate(items2.getSize() + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer2.putInt(0x00010001);
        buffer2.putInt(items2.getSize() + 8);
        items2.write(buffer2);
        
        buffer2.flip();
        buffer2.getInt();
        buffer2.getInt();
        buffer2.getInt();  // stringCount
        buffer2.getInt();  // styleCount
        int flags = buffer2.getInt();
        
        assertEquals(0, flags & 0x100, "重新写入后应保持UTF-16编码");
    }

    @Test
    @DisplayName("测试UTF-8编码保持：读取UTF-8 AXML，写出后仍为UTF-8")
    void testPreserveUtf8Encoding() throws IOException {
        // 1. 创建UTF-8格式的StringItems
        StringItems items = new StringItems();
        items.add(new StringItem("hello"));
        items.add(new StringItem("world"));
        items.setForceEncoding(true);  // 强制UTF-8
        items.prepare();
        
        // 2. 写入ByteBuffer
        int size = items.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(size + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x00010001);  // chunk type
        buffer.putInt(size + 8);     // chunk size
        items.write(buffer);
        
        // 3. 读回并验证flags保持
        buffer.flip();
        buffer.getInt();  // skip type
        buffer.getInt();  // skip size
        
        StringItems receiver = new StringItems();
        String[] strings = StringItems.read(buffer, receiver);
        
        // 验证flags
        assertEquals(0x100, receiver.getOriginalFlags() & 0x100, "UTF-8格式的flags应为0x100");
        assertEquals(2, strings.length, "应读取2个字符串");
        assertEquals("hello", strings[0], "第0个字符串");
        assertEquals("world", strings[1], "第1个字符串");
        
        // 4. 再次写入，验证编码保持
        StringItems items2 = new StringItems();
        for (String s : strings) {
            items2.add(new StringItem(s));
        }
        items2.setOriginalFlags(receiver.getOriginalFlags());
        items2.prepare();
        
        // 5. 验证useUTF8=true（通过检查写入的flags）
        ByteBuffer buffer2 = ByteBuffer.allocate(items2.getSize() + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer2.putInt(0x00010001);
        buffer2.putInt(items2.getSize() + 8);
        items2.write(buffer2);
        
        buffer2.flip();
        buffer2.getInt();
        buffer2.getInt();
        buffer2.getInt();  // stringCount
        buffer2.getInt();  // styleCount
        int flags = buffer2.getInt();
        
        assertEquals(0x100, flags & 0x100, "重新写入后应保持UTF-8编码");
    }

    @Test
    @DisplayName("测试超长字符串自动降级UTF-16（功能完整性）")
    void testLongStringFallbackToUtf16() throws IOException {
        StringItems items = new StringItems();
        
        // 创建超长字符串（>32767字符）
        String longString = "a".repeat(0x8000);
        items.add(new StringItem(longString));
        
        // 即使originalFlags为UTF-8，也应降级UTF-16
        items.setOriginalFlags(0x100);
        items.prepare();
        
        // 验证实际使用UTF-16
        ByteBuffer buffer = ByteBuffer.allocate(items.getSize() + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x00010001);
        buffer.putInt(items.getSize() + 8);
        items.write(buffer);
        
        buffer.flip();
        buffer.position(16);  // skip headers
        int flags = buffer.getInt();
        
        assertEquals(0, flags & 0x100, "超长字符串应强制降级UTF-16");
    }

    @Test
    @DisplayName("测试强制编码覆盖：setForceEncoding可覆盖原始编码")
    void testForceEncodingOverride() throws IOException {
        StringItems items = new StringItems();
        items.add(new StringItem("test"));
        
        // 设置原始为UTF-16
        items.setOriginalFlags(0);
        
        // 强制覆盖为UTF-8
        items.setForceEncoding(true);
        items.prepare();
        
        // 验证实际使用UTF-8
        ByteBuffer buffer = ByteBuffer.allocate(items.getSize() + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x00010001);
        buffer.putInt(items.getSize() + 8);
        items.write(buffer);
        
        buffer.flip();
        buffer.position(16);
        int flags = buffer.getInt();
        
        assertEquals(0x100, flags & 0x100, "强制覆盖应生效");
    }

    @Test
    @DisplayName("测试体积一致性：重建后StringPool大小变化<5%")
    void testSizeConsistency() throws IOException {
        // 创建一个包含多个字符串的StringItems（模拟真实场景）
        StringItems original = new StringItems();
        original.add(new StringItem("android"));
        original.add(new StringItem("layout"));
        original.add(new StringItem("activity_main"));
        original.add(new StringItem("TextView"));
        original.add(new StringItem("Button"));
        original.setForceEncoding(false);  // 使用UTF-16
        original.prepare();
        
        // 写入
        int originalSize = original.getSize();
        ByteBuffer buffer = ByteBuffer.allocate(originalSize + 100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x00010001);
        buffer.putInt(originalSize + 8);
        original.write(buffer);
        
        // 读回
        buffer.flip();
        buffer.getInt();
        buffer.getInt();
        StringItems receiver = new StringItems();
        String[] strings = StringItems.read(buffer, receiver);
        
        // 重建
        StringItems rebuilt = new StringItems();
        for (String s : strings) {
            rebuilt.add(new StringItem(s));
        }
        rebuilt.setOriginalFlags(receiver.getOriginalFlags());
        rebuilt.prepare();
        
        int rebuiltSize = rebuilt.getSize();
        
        // 验证大小变化<5%
        double changeRatio = Math.abs(rebuiltSize - originalSize) / (double)originalSize;
        assertTrue(changeRatio < 0.05, 
            String.format("StringPool大小变化%.2f%%超过5%%阈值 (原始:%d, 重建:%d)", 
                changeRatio * 100, originalSize, rebuiltSize));
    }
}


