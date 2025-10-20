package com.resources.arsc;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResTableTypeSpec单元测试
 */
class ResTableTypeSpecTest {
    
    @Test
    void testParseAndWrite() {
        // 创建测试数据
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        
        // 写入typeSpec chunk
        buffer.putShort((short) 0x0202);  // type
        buffer.putShort((short) 16);      // headerSize
        buffer.putInt(28);                // chunkSize (16 + 12 bytes entryFlags for 3 entries)
        buffer.put((byte) 1);             // id
        buffer.put((byte) 0);             // res0
        buffer.putShort((short) 0);       // res1
        buffer.putInt(3);                 // entryCount
        
        // 写入entryFlags (3 entries * 4 bytes)
        buffer.putInt(0x00000001);
        buffer.putInt(0x00000002);
        buffer.putInt(0x00000003);
        
        // 准备读取
        buffer.flip();
        
        // 解析
        ResTableTypeSpec spec = new ResTableTypeSpec();
        spec.parse(buffer);
        
        // 验证解析结果
        assertEquals(1, spec.getId());
        assertEquals(28, spec.getChunkSize());
        assertEquals(3, spec.getEntryCount());
        assertTrue(spec.validate());
        
        // 写回
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int bytesWritten = spec.write(writeBuffer);
        
        // 验证写入结果
        assertEquals(28, bytesWritten);
        
        // 验证写入的数据与原始数据一致
        writeBuffer.flip();
        buffer.rewind();
        
        for (int i = 0; i < bytesWritten; i++) {
            assertEquals(buffer.get(), writeBuffer.get(), 
                        "Byte " + i + " mismatch");
        }
    }
    
    @Test
    void testInvalidChunkType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putShort((short) 0x9999);  // 错误的type
        buffer.putShort((short) 16);
        buffer.putInt(28);
        
        buffer.flip();
        
        ResTableTypeSpec spec = new ResTableTypeSpec();
        
        assertThrows(IllegalArgumentException.class, () -> spec.parse(buffer));
    }
    
    @Test
    void testChunkTooSmall() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putShort((short) 0x0202);
        buffer.putShort((short) 16);
        buffer.putInt(28);
        // 没有足够的数据
        
        buffer.flip();
        
        ResTableTypeSpec spec = new ResTableTypeSpec();
        
        assertThrows(IllegalArgumentException.class, () -> spec.parse(buffer));
    }
}

