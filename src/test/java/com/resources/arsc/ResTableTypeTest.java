package com.resources.arsc;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResTableType单元测试
 */
class ResTableTypeTest {
    
    @Test
    void testParseAndWrite() {
        // 创建测试数据（简化的type chunk）
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        
        int chunkSize = 100;
        
        // 写入type chunk头部
        buffer.putShort((short) 0x0201);  // type
        buffer.putShort((short) 84);      // headerSize (可变)
        buffer.putInt(chunkSize);         // chunkSize
        buffer.put((byte) 1);             // id
        
        // 填充剩余数据
        for (int i = 9; i < chunkSize; i++) {
            buffer.put((byte) 0xFF);
        }
        
        // 准备读取
        buffer.flip();
        
        // 解析
        ResTableType type = new ResTableType();
        type.parse(buffer);
        
        // 验证解析结果
        assertEquals(1, type.getId());
        assertEquals(chunkSize, type.getChunkSize());
        assertTrue(type.validate());
        
        // 写回
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int bytesWritten = type.write(writeBuffer);
        
        // 验证写入结果
        assertEquals(chunkSize, bytesWritten);
        
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
        buffer.putShort((short) 84);
        buffer.putInt(100);
        
        buffer.flip();
        
        ResTableType type = new ResTableType();
        
        assertThrows(IllegalArgumentException.class, () -> type.parse(buffer));
    }
    
    @Test
    void testChunkTooSmall() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putShort((short) 0x0201);
        buffer.putShort((short) 84);
        // 没有足够的数据
        
        buffer.flip();
        
        ResTableType type = new ResTableType();
        
        assertThrows(IllegalArgumentException.class, () -> type.parse(buffer));
    }
}

