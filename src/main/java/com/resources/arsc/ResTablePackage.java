package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * ResTable_package - 资源包结构
 * 
 * 对应AAPT2生成的package chunk，包含：
 * - packageId（必须保持0x7f不变）
 * - 包名（128字符，char16_t数组）
 * - 类型字符串池
 * - 资源名字符串池
 * - typeSpec和typeConfig数组
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResTablePackage {
    
    private static final Logger log = LoggerFactory.getLogger(ResTablePackage.class);
    
    // Chunk类型常量
    public static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
    
    // 包名最大长度（char16_t[128]）
    private static final int PACKAGE_NAME_MAX_LENGTH = 128;
    
    // 头部大小
    private static final int HEADER_SIZE = 288; // 12 + 4 + 256 + 16
    
    // 数据成员
    private int id;                          // packageId（0x7f）
    private String name;                     // 包名
    private int typeStringsOffset;           // 类型字符串池偏移
    private int keyStringsOffset;            // 资源名字符串池偏移
    
    private ResStringPool typeStrings;       // 类型字符串池（如"attr", "layout", "string"等）
    private ResStringPool keyStrings;        // 资源名字符串池（如"app_name", "main_activity"等）
    
    // 原始数据（用于写回时保持其他chunk不变）
    private byte[] originalData;
    private int originalSize;
    
    public ResTablePackage() {
        this.id = 0x7f; // 默认应用包ID
    }
    
    /**
     * 解析资源包
     * 
     * @param buffer ByteBuffer（position指向package chunk开始）
     * @throws IllegalArgumentException 解析失败
     */
    public void parse(ByteBuffer buffer) throws IllegalArgumentException {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPosition = buffer.position();
        
        try {
            // 1. 读取chunk头部
            int type = buffer.getShort() & 0xFFFF;
            int headerSize = buffer.getShort() & 0xFFFF;
            int chunkSize = buffer.getInt();
            
            if (type != RES_TABLE_PACKAGE_TYPE) {
                throw new IllegalArgumentException(
                    String.format("无效的chunk类型: 期望0x%04X，实际0x%04X", 
                                 RES_TABLE_PACKAGE_TYPE, type));
            }
            
            log.debug("解析资源包: headerSize={}, chunkSize={}", headerSize, chunkSize);
            
            // 2. 读取包ID
            this.id = buffer.getInt();
            if (this.id != 0x7f && this.id != 0x01) {
                log.warn("非标准packageId: 0x{} (期望0x7f或0x01)", 
                        Integer.toHexString(this.id));
            }
            
            // 3. 读取包名（char16_t[128]）
            char[] nameChars = new char[PACKAGE_NAME_MAX_LENGTH];
            for (int i = 0; i < PACKAGE_NAME_MAX_LENGTH; i++) {
                nameChars[i] = buffer.getChar();
            }
            
            // 转换为字符串（去除null终止符）
            StringBuilder nameBuilder = new StringBuilder();
            for (char c : nameChars) {
                if (c == 0) break;
                nameBuilder.append(c);
            }
            this.name = nameBuilder.toString();
            
            log.debug("包ID: 0x{}, 包名: '{}'", Integer.toHexString(id), name);
            
            // 4. 读取偏移信息
            this.typeStringsOffset = buffer.getInt();
            buffer.getInt(); // lastPublicType (未使用)
            this.keyStringsOffset = buffer.getInt();
            buffer.getInt(); // lastPublicKey (未使用)
            
            // 可选：typeIdOffset（新版AAPT2，未使用）
            if (headerSize >= HEADER_SIZE + 4) {
                buffer.getInt(); // typeIdOffset
            }
            
            log.debug("偏移: typeStrings={}, keyStrings={}", 
                     typeStringsOffset, keyStringsOffset);
            
            // 5. 跳到类型字符串池
            if (typeStringsOffset > 0) {
                buffer.position(startPosition + typeStringsOffset);
                this.typeStrings = new ResStringPool();
                this.typeStrings.parse(buffer);
                
                log.debug("类型字符串池: {} 个类型", typeStrings.getStringCount());
            }
            
            // 6. 跳到资源名字符串池
            if (keyStringsOffset > 0) {
                buffer.position(startPosition + keyStringsOffset);
                this.keyStrings = new ResStringPool();
                this.keyStrings.parse(buffer);
                
                log.debug("资源名字符串池: {} 个资源", keyStrings.getStringCount());
            }
            
            // 7. 保存原始数据（用于写回）
            this.originalSize = chunkSize;
            buffer.position(startPosition);
            this.originalData = new byte[chunkSize];
            buffer.get(this.originalData);
            
            log.info("资源包解析完成: id=0x{}, name='{}', types={}, keys={}", 
                    Integer.toHexString(id), name, 
                    typeStrings != null ? typeStrings.getStringCount() : 0,
                    keyStrings != null ? keyStrings.getStringCount() : 0);
            
        } catch (Exception e) {
            log.error("资源包解析失败", e);
            throw new IllegalArgumentException("资源包解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置包名（只改名称，不改ID）
     * 
     * @param newName 新包名
     * @throws IllegalArgumentException 包名无效
     */
    public void setName(String newName) {
        Objects.requireNonNull(newName, "newName不能为null");
        
        if (newName.isEmpty()) {
            throw new IllegalArgumentException("包名不能为空");
        }
        
        if (newName.length() > PACKAGE_NAME_MAX_LENGTH - 1) {
            log.warn("包名过长，将被截断: {} -> {} 字符", 
                    newName.length(), PACKAGE_NAME_MAX_LENGTH - 1);
            newName = newName.substring(0, PACKAGE_NAME_MAX_LENGTH - 1);
        }
        
        String oldName = this.name;
        this.name = newName;
        
        log.info("包名修改: '{}' -> '{}' (packageId保持0x{})", 
                oldName, newName, Integer.toHexString(id));
    }
    
    /**
     * 写入资源包
     * 
     * @param buffer ByteBuffer
     * @return 写入的字节数
     */
    public int write(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer不能为null");
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int startPosition = buffer.position();
        
        // 策略：
        // 1. 如果只修改了包名，使用原始数据并只更新包名部分
        // 2. 如果修改了字符串池，需要完整重建
        
        if (originalData != null && typeStrings != null && keyStrings != null) {
            // 使用原始数据修改包名
            // 
            // 设计说明：当前实现仅支持包名修改（修改offset 12-268的256字节）
            // 这对于包名/类名随机化场景已经足够
            // 
            // 完整重建需要处理：
            // - ResTable_typeSpec chunks
            // - ResTable_type chunks
            // - ResTable_config
            // 
            // 由于这些结构复杂且当前场景不需要，暂不实现
            writeWithOriginalData(buffer);
        } else {
            throw new IllegalStateException(
                "ResTablePackage缺少原始数据，无法写入。" +
                "这通常意味着解析失败或数据损坏。");
        }
        
        int bytesWritten = buffer.position() - startPosition;
        log.info("资源包写入完成: {} 字节", bytesWritten);
        
        return bytesWritten;
    }
    
    /**
     * 使用原始数据写入（仅修改包名）
     */
    private void writeWithOriginalData(ByteBuffer buffer) {
        // 复制原始数据
        byte[] modifiedData = originalData.clone();
        ByteBuffer modBuffer = ByteBuffer.wrap(modifiedData).order(ByteOrder.LITTLE_ENDIAN);
        
        // 跳过chunk头部（8字节）和packageId（4字节）
        modBuffer.position(12);
        
        // 写入新包名（char16_t[128]）
        for (int i = 0; i < PACKAGE_NAME_MAX_LENGTH; i++) {
            if (i < name.length()) {
                modBuffer.putChar(name.charAt(i));
            } else {
                modBuffer.putChar((char) 0); // 填充null
            }
        }
        
        // 写入修改后的数据
        buffer.put(modifiedData);
        
        log.debug("使用原始数据写入，仅修改包名");
    }
    
    /**
     * 验证资源包完整性
     * 
     * @return true=有效, false=无效
     */
    public boolean validate() {
        try {
            // 1. 检查packageId
            if (id != 0x7f && id != 0x01) {
                log.error("无效的packageId: 0x{}", Integer.toHexString(id));
                return false;
            }
            
            // 2. 检查包名
            if (name == null || name.isEmpty()) {
                log.error("包名为空");
                return false;
            }
            
            // 3. 检查字符串池
            if (typeStrings != null && !typeStrings.validate()) {
                log.error("类型字符串池验证失败");
                return false;
            }
            
            if (keyStrings != null && !keyStrings.validate()) {
                log.error("资源名字符串池验证失败");
                return false;
            }
            
            log.debug("资源包验证通过");
            return true;
            
        } catch (Exception e) {
            log.error("资源包验证失败", e);
            return false;
        }
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public ResStringPool getTypeStrings() { return typeStrings; }
    public ResStringPool getKeyStrings() { return keyStrings; }
    public int getTypeStringsOffset() { return typeStringsOffset; }
    public int getKeyStringsOffset() { return keyStringsOffset; }
    public int getOriginalSize() { return originalSize; }
    
    @Override
    public String toString() {
        return String.format("ResTablePackage{id=0x%02X, name='%s', types=%d, keys=%d}", 
                           id, name, 
                           typeStrings != null ? typeStrings.getStringCount() : 0,
                           keyStrings != null ? keyStrings.getStringCount() : 0);
    }
}

