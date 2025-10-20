package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * ResTable_type - 资源类型数据结构
 * 
 * 最小化实现：
 * - 只解析chunk头部获取id和chunkSize
 * - 保留完整原始字节数据，不解析具体entry内容
 * - 写入时直接使用原始数据
 * 
 * 设计原因：
 * ResTable_type结构极其复杂，包含：
 * - ResTable_config（设备配置，48+字节）
 * - entry offsets数组
 * - ResTable_entry（simple或complex，可变长度）
 * - Res_value或ResTable_map_entry
 * 
 * 完整解析需要实现几十个类，且当前场景只需要修改字符串池，
 * 不需要修改entry内容，因此保留原始数据是最安全的方案。
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResTableType {
    
    private static final Logger log = LoggerFactory.getLogger(ResTableType.class);
    
    public static final int RES_TABLE_TYPE_TYPE = 0x0201;
    
    private byte[] originalData;  // 保留完整原始数据
    private int chunkSize;        // chunk总大小
    private int id;               // type ID (1-based)
    
    public ResTableType() {
    }
    
    /**
     * 解析ResTable_type chunk
     * 
     * 只解析头部获取id和size，其余内容保留原始字节
     * 
     * @param buffer ByteBuffer（position指向chunk开始）
     * @throws IllegalArgumentException 解析失败
     */
    public void parse(ByteBuffer buffer) throws IllegalArgumentException {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPos = buffer.position();
        
        try {
            // 边界检查：至少需要8字节读取type和size
            if (buffer.remaining() < 8) {
                throw new IllegalArgumentException(
                    String.format("无法读取type chunk头部: 需要8字节，剩余%d字节",
                                buffer.remaining()));
            }
            
            // 1. 读取chunk类型和大小
            int type = buffer.getShort() & 0xFFFF;
            buffer.getShort();  // headerSize (不需要)
            chunkSize = buffer.getInt();
            
            if (type != RES_TABLE_TYPE_TYPE) {
                throw new IllegalArgumentException(
                    String.format("无效的chunk类型: 期望0x%04X，实际0x%04X",
                                RES_TABLE_TYPE_TYPE, type));
            }
            
            // 验证chunk大小
            if (chunkSize < 8) {
                throw new IllegalArgumentException(
                    String.format("type chunk大小太小: size=%d, 至少需要8", chunkSize));
            }
            
            if (chunkSize > buffer.limit() - startPos) {
                throw new IllegalArgumentException(
                    String.format("type chunk超出buffer: chunkSize=%d, available=%d",
                                chunkSize, buffer.limit() - startPos));
            }
            
            // 验证大小合理性（type chunk通常不超过10MB）
            if (chunkSize > 10 * 1024 * 1024) {
                log.warn("type chunk异常大: {} MB", chunkSize / 1024 / 1024);
            }
            
            // 2. 读取id（在offset 8）
            id = buffer.get() & 0xFF;
            
            // 3. 回退到chunk开始，保存完整原始数据
            buffer.position(startPos);
            
            // 边界检查：确保有足够的字节读取整个chunk
            if (buffer.remaining() < chunkSize) {
                throw new IllegalArgumentException(
                    String.format("无法读取type chunk完整数据: 需要%d字节，剩余%d字节",
                                chunkSize, buffer.remaining()));
            }
            
            originalData = new byte[chunkSize];
            buffer.get(originalData);
            
            log.debug("type解析完成: id={}, chunkSize={}", id, chunkSize);
            
        } catch (Exception e) {
            log.error("type解析失败 at position={}", startPos, e);
            throw new IllegalArgumentException("type解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 写入ResTable_type chunk
     * 
     * 直接写入原始数据，不做任何修改
     * 
     * @param buffer ByteBuffer
     * @return 写入的字节数
     */
    public int write(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer不能为null");
        
        // 验证数据完整性
        if (originalData == null) {
            throw new IllegalStateException("originalData为null，无法写入");
        }
        
        if (originalData.length != chunkSize) {
            throw new IllegalStateException(
                String.format("originalData大小不匹配: 期望%d, 实际%d",
                            chunkSize, originalData.length));
        }
        
        // 直接写入原始数据
        buffer.put(originalData);
        
        log.trace("type写入: id={}, {} 字节", id, originalData.length);
        
        return originalData.length;
    }
    
    /**
     * 验证type完整性
     * 
     * @return true=有效, false=无效
     */
    public boolean validate() {
        if (id <= 0 || id > 255) {
            log.error("无效的type ID: {}", id);
            return false;
        }
        
        if (chunkSize <= 0) {
            log.error("无效的chunkSize: {}", chunkSize);
            return false;
        }
        
        if (originalData == null || originalData.length != chunkSize) {
            log.error("originalData大小不匹配");
            return false;
        }
        
        return true;
    }
    
    // Getters
    public int getChunkSize() { return chunkSize; }
    public int getId() { return id; }
    public byte[] getOriginalData() { return originalData != null ? originalData.clone() : null; }
    
    @Override
    public String toString() {
        return String.format("ResTableType{id=%d, chunkSize=%d}",
                           id, chunkSize);
    }
}

