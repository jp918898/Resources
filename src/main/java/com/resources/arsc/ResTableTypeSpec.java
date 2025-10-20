package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * ResTable_typeSpec - 资源类型规格结构
 * 
 * 最小化实现：
 * - 解析chunk头部和entryCount
 * - 保留entryFlags原始字节数据，不解析具体内容
 * - 支持精确读取和写入
 * 
 * 结构（AAPT2规范）:
 * - type (2 bytes): 0x0202
 * - headerSize (2 bytes): 16
 * - size (4 bytes): chunk总大小
 * - id (1 byte): type ID (1-based)
 * - res0 (1 byte): 保留字段
 * - res1 (2 bytes): 保留字段
 * - entryCount (4 bytes): entry数量
 * - entryFlags (entryCount * 4 bytes): 每个entry的flags
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResTableTypeSpec {
    
    private static final Logger log = LoggerFactory.getLogger(ResTableTypeSpec.class);
    
    public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    private static final int HEADER_SIZE = 16;
    
    private int id;                      // type ID (1-based)
    private int entryCount;              // entry数量
    private byte[] entryFlagsData;       // entryFlags原始字节，不解析
    private int chunkSize;               // chunk总大小
    
    public ResTableTypeSpec() {
    }
    
    /**
     * 解析ResTable_typeSpec chunk
     * 
     * @param buffer ByteBuffer（position指向chunk开始）
     * @throws IllegalArgumentException 解析失败
     */
    public void parse(ByteBuffer buffer) throws IllegalArgumentException {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPos = buffer.position();
        
        try {
            // 边界检查：至少需要16字节头部
            if (buffer.remaining() < HEADER_SIZE) {
                throw new IllegalArgumentException(
                    String.format("无法读取typeSpec头部: 需要%d字节，剩余%d字节",
                                HEADER_SIZE, buffer.remaining()));
            }
            
            // 1. 读取chunk头部
            int type = buffer.getShort() & 0xFFFF;
            int headerSize = buffer.getShort() & 0xFFFF;
            chunkSize = buffer.getInt();
            
            if (type != RES_TABLE_TYPE_SPEC_TYPE) {
                throw new IllegalArgumentException(
                    String.format("无效的chunk类型: 期望0x%04X，实际0x%04X",
                                RES_TABLE_TYPE_SPEC_TYPE, type));
            }
            
            if (headerSize != HEADER_SIZE) {
                log.warn("typeSpec headerSize不标准: 期望{}, 实际{}", HEADER_SIZE, headerSize);
            }
            
            // 验证chunk大小
            if (chunkSize < HEADER_SIZE) {
                throw new IllegalArgumentException(
                    String.format("typeSpec chunk大小太小: size=%d, 至少需要%d",
                                chunkSize, HEADER_SIZE));
            }
            
            if (chunkSize > buffer.limit() - startPos) {
                throw new IllegalArgumentException(
                    String.format("typeSpec chunk超出buffer: chunkSize=%d, available=%d",
                                chunkSize, buffer.limit() - startPos));
            }
            
            // 2. 读取typeSpec字段
            id = buffer.get() & 0xFF;
            buffer.get();  // res0 (保留字段)
            buffer.getShort();  // res1 (保留字段)
            entryCount = buffer.getInt();
            
            // 验证entryCount合理性
            if (entryCount < 0) {
                throw new IllegalArgumentException("entryCount为负数: " + entryCount);
            }
            
            if (entryCount > 100000) {  // 10万个entry已经非常大
                throw new IllegalArgumentException(
                    String.format("entryCount异常: %d（超过10万，可能数据损坏）", entryCount));
            }
            
            // 3. 读取entryFlags原始数据
            int flagsSize = entryCount * 4;
            int expectedChunkSize = HEADER_SIZE + flagsSize;
            
            if (chunkSize != expectedChunkSize) {
                log.warn("typeSpec chunk大小不匹配: 期望={}, 实际={}", 
                        expectedChunkSize, chunkSize);
            }
            
            // 边界检查：确保有足够的字节读取entryFlags
            if (buffer.remaining() < flagsSize) {
                throw new IllegalArgumentException(
                    String.format("无法读取entryFlags: 需要%d字节，剩余%d字节",
                                flagsSize, buffer.remaining()));
            }
            
            entryFlagsData = new byte[flagsSize];
            buffer.get(entryFlagsData);
            
            // 4. 移动position到chunk末尾
            buffer.position(startPos + chunkSize);
            
            log.debug("typeSpec解析完成: id={}, entryCount={}, chunkSize={}", 
                     id, entryCount, chunkSize);
            
        } catch (Exception e) {
            log.error("typeSpec解析失败 at position={}", startPos, e);
            throw new IllegalArgumentException("typeSpec解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 写入ResTable_typeSpec chunk
     * 
     * @param buffer ByteBuffer
     * @return 写入的字节数
     */
    public int write(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPos = buffer.position();
        
        // 验证数据完整性
        if (entryFlagsData == null) {
            throw new IllegalStateException("entryFlagsData为null，无法写入");
        }
        
        if (entryFlagsData.length != entryCount * 4) {
            throw new IllegalStateException(
                String.format("entryFlagsData大小不匹配: 期望%d, 实际%d",
                            entryCount * 4, entryFlagsData.length));
        }
        
        // 1. 写入chunk头部
        buffer.putShort((short) RES_TABLE_TYPE_SPEC_TYPE);
        buffer.putShort((short) HEADER_SIZE);
        buffer.putInt(chunkSize);
        
        // 2. 写入typeSpec字段
        buffer.put((byte) id);
        buffer.put((byte) 0);       // res0
        buffer.putShort((short) 0);  // res1
        buffer.putInt(entryCount);
        
        // 3. 写入entryFlags原始数据
        buffer.put(entryFlagsData);
        
        int bytesWritten = buffer.position() - startPos;
        
        log.trace("typeSpec写入: id={}, {} 字节", id, bytesWritten);
        
        return bytesWritten;
    }
    
    /**
     * 验证typeSpec完整性
     * 
     * @return true=有效, false=无效
     */
    public boolean validate() {
        if (id <= 0 || id > 255) {
            log.error("无效的type ID: {}", id);
            return false;
        }
        
        if (entryCount < 0) {
            log.error("无效的entryCount: {}", entryCount);
            return false;
        }
        
        if (entryFlagsData == null || entryFlagsData.length != entryCount * 4) {
            log.error("entryFlagsData大小不匹配");
            return false;
        }
        
        return true;
    }
    
    // Getters
    public int getChunkSize() { return chunkSize; }
    public int getId() { return id; }
    public int getEntryCount() { return entryCount; }
    
    @Override
    public String toString() {
        return String.format("ResTableTypeSpec{id=%d, entryCount=%d, chunkSize=%d}",
                           id, entryCount, chunkSize);
    }
}

