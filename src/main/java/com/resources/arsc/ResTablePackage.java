package com.resources.arsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
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
    
    // typeSpec和type chunks（用于完整重建）
    private List<ResTableTypeSpec> typeSpecs = new ArrayList<>();
    private List<ResTableType> types = new ArrayList<>();
    
    // 修改标志
    private boolean nameModified = false;
    private boolean typeStringsModified = false;
    private boolean keyStringsModified = false;
    
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
            
            // 7. 解析typeSpec和type chunks
            log.debug("开始解析typeSpec和type chunks");
            
            while (buffer.position() < startPosition + chunkSize) {
                if (buffer.remaining() < 8) {
                    log.debug("剩余字节不足8，停止解析");
                    break;
                }
                
                int chunkPos = buffer.position();
                int chunkType = buffer.getShort() & 0xFFFF;
                buffer.position(chunkPos);  // 回退
                
                if (chunkType == 0x0202) {  // RES_TABLE_TYPE_SPEC_TYPE
                    try {
                        ResTableTypeSpec spec = new ResTableTypeSpec();
                        spec.parse(buffer);
                        typeSpecs.add(spec);
                        log.debug("解析typeSpec: {}", spec);
                    } catch (Exception e) {
                        log.warn("解析typeSpec失败，停止解析: {}", e.getMessage());
                        break;
                    }
                } else if (chunkType == 0x0201) {  // RES_TABLE_TYPE_TYPE
                    try {
                        ResTableType typeChunk = new ResTableType();
                        typeChunk.parse(buffer);
                        types.add(typeChunk);
                        log.debug("解析type: {}", typeChunk);
                    } catch (Exception e) {
                        log.warn("解析type失败，停止解析: {}", e.getMessage());
                        break;
                    }
                } else {
                    log.debug("遇到未知chunk类型0x{}, 停止解析", 
                             Integer.toHexString(chunkType));
                    break;
                }
            }
            
            log.info("typeSpec/type解析完成: typeSpecs={}, types={}", 
                    typeSpecs.size(), types.size());
            
            // 8. 保存原始数据（用于写回）
            this.originalSize = chunkSize;
            buffer.position(startPosition);
            this.originalData = new byte[chunkSize];
            buffer.get(this.originalData);
            
            log.info("资源包解析完成: id=0x{}, name='{}', typeStrings={}, keyStrings={}, typeSpecs={}, types={}", 
                    Integer.toHexString(id), name, 
                    typeStrings != null ? typeStrings.getStringCount() : 0,
                    keyStrings != null ? keyStrings.getStringCount() : 0,
                    typeSpecs.size(),
                    types.size());
            
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
        
        // 标记包名已修改
        if (!oldName.equals(newName)) {
            this.nameModified = true;
        }
        
        log.info("包名修改: '{}' -> '{}' (packageId保持0x{})", 
                oldName, newName, Integer.toHexString(id));
    }
    
    /**
     * 修改类型字符串
     * 
     * @param index 字符串索引
     * @param value 新值
     */
    public void setTypeString(int index, String value) {
        Objects.requireNonNull(value, "value不能为null");
        
        if (typeStrings == null) {
            throw new IllegalStateException("typeStrings未初始化");
        }
        
        typeStrings.setString(index, value);
        typeStringsModified = true;
        
        log.info("类型字符串修改[{}]: -> '{}'", index, value);
    }
    
    /**
     * 修改资源名字符串
     * 
     * @param index 字符串索引
     * @param value 新值
     */
    public void setKeyString(int index, String value) {
        Objects.requireNonNull(value, "value不能为null");
        
        if (keyStrings == null) {
            throw new IllegalStateException("keyStrings未初始化");
        }
        
        keyStrings.setString(index, value);
        keyStringsModified = true;
        
        log.info("资源名字符串修改[{}]: -> '{}'", index, value);
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
        
        // 策略判断：
        // 1. 如果修改了typeStrings或keyStrings，需要完整重建
        // 2. 否则使用原始数据并只更新包名部分
        
        if (typeStringsModified || keyStringsModified) {
            return writeWithFullRebuild(buffer);
        } else {
            if (originalData != null && typeStrings != null && keyStrings != null) {
                writeWithOriginalData(buffer);
            } else {
                throw new IllegalStateException(
                    "ResTablePackage缺少原始数据，无法写入。" +
                    "这通常意味着解析失败或数据损坏。");
            }
        }
        
        int bytesWritten = buffer.position() - startPosition;
        log.info("资源包写入完成: {} 字节 (重建模式={})", 
                bytesWritten, typeStringsModified || keyStringsModified);
        
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
        
        log.debug("使用原始数据写入，仅修改包名 (nameModified={})", nameModified);
    }
    
    /**
     * 完整重建package chunk
     * 
     * 当typeStrings或keyStrings被修改时调用
     * 完整重建所有chunk：header + typeStrings + keyStrings + typeSpecs + types
     * 
     * @param buffer ByteBuffer
     * @return 写入的字节数
     */
    private int writeWithFullRebuild(ByteBuffer buffer) {
        int startPos = buffer.position();
        
        log.info("开始完整重建package chunk");
        
        // 1. 计算新chunk大小
        int headerSize = HEADER_SIZE;
        int typeStringsSize = calculateStringPoolSize(typeStrings);
        int keyStringsSize = calculateStringPoolSize(keyStrings);
        int typeSpecsSize = typeSpecs.stream().mapToInt(ResTableTypeSpec::getChunkSize).sum();
        int typesSize = types.stream().mapToInt(ResTableType::getChunkSize).sum();
        int newChunkSize = headerSize + typeStringsSize + keyStringsSize + typeSpecsSize + typesSize;
        
        log.debug("计算chunk大小: header={}, typeStrings={}, keyStrings={}, typeSpecs={}, types={}, total={}",
                 headerSize, typeStringsSize, keyStringsSize, typeSpecsSize, typesSize, newChunkSize);
        
        // 2. 写入package header (288字节)
        buffer.putShort((short) RES_TABLE_PACKAGE_TYPE);
        buffer.putShort((short) HEADER_SIZE);
        buffer.putInt(newChunkSize);
        buffer.putInt(id);
        
        // 3. 写入包名 (char16_t[128] = 256字节)
        for (int i = 0; i < PACKAGE_NAME_MAX_LENGTH; i++) {
            if (i < name.length()) {
                buffer.putChar(name.charAt(i));
            } else {
                buffer.putChar((char) 0);
            }
        }
        
        // 4. 写入偏移和保留字段（16字节）
        int typeStringsOffset = headerSize;
        int keyStringsOffset = typeStringsOffset + typeStringsSize;
        
        buffer.putInt(typeStringsOffset);
        buffer.putInt(0);  // lastPublicType
        buffer.putInt(keyStringsOffset);
        buffer.putInt(0);  // lastPublicKey
        buffer.putInt(0);  // typeIdOffset
        
        // 5. 写入typeStrings
        int typeStringsWritten = typeStrings.write(buffer);
        log.debug("typeStrings写入: {} 字节 (预计={})", typeStringsWritten, typeStringsSize);
        
        // 6. 写入keyStrings
        int keyStringsWritten = keyStrings.write(buffer);
        log.debug("keyStrings写入: {} 字节 (预计={})", keyStringsWritten, keyStringsSize);
        
        // 7. 写入所有typeSpec
        int typeSpecsWritten = 0;
        for (ResTableTypeSpec spec : typeSpecs) {
            typeSpecsWritten += spec.write(buffer);
        }
        log.debug("typeSpecs写入: {} 字节 (预计={})", typeSpecsWritten, typeSpecsSize);
        
        // 8. 写入所有type
        int typesWritten = 0;
        for (ResTableType typeChunk : types) {
            typesWritten += typeChunk.write(buffer);
        }
        log.debug("types写入: {} 字节 (预计={})", typesWritten, typesSize);
        
        int bytesWritten = buffer.position() - startPos;
        
        // ✅ 工业级标准：Size不匹配立即失败，符合Fail-Fast原则
        if (bytesWritten != newChunkSize) {
            String msg = String.format(
                "Package重建size严重不匹配: 预计=%d, 实际=%d, 差异=%d. " +
                "这表明StringPool或TypeSpec/Type的size计算不准确，必须立即修复",
                newChunkSize, bytesWritten, bytesWritten - newChunkSize);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        
        log.info("完整重建完成: {} 字节", bytesWritten);
        
        return bytesWritten;
    }
    
    /**
     * 计算字符串池大小
     * 
     * @param pool 字符串池
     * @return 字节大小
     */
    private int calculateStringPoolSize(ResStringPool pool) {
        if (pool == null) {
            return 0;
        }
        
        int headerSize = 28;
        int offsetsSize = pool.getStringCount() * 4;
        int stylesOffsetsSize = pool.getStyleCount() * 4;
        
        int stringsDataSize = 0;
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            stringsDataSize += calculateStringSize(str, pool.isUtf8());
        }
        
        // 4字节对齐
        int padding = (stringsDataSize % 4 == 0) ? 0 : (4 - stringsDataSize % 4);
        
        return headerSize + offsetsSize + stylesOffsetsSize + stringsDataSize + padding;
    }
    
    /**
     * 计算单个字符串编码后的大小
     * 
     * @param str 字符串
     * @param utf8 是否UTF-8编码
     * @return 字节大小
     */
    private int calculateStringSize(String str, boolean utf8) {
        if (utf8) {
            try {
                byte[] bytes = ModifiedUTF8.encode(str);
                int byteLen = bytes.length;
                int utf8CharLen = ModifiedUTF8.countCharacters(bytes);
                
                int size = 0;
                size += (utf8CharLen >= 0x80) ? 2 : 1;  // UTF-8字符数编码
                size += (byteLen >= 0x80) ? 2 : 1;      // 字节长度编码
                size += byteLen;                         // 数据
                size += 1;                               // 终止符
                return size;
            } catch (IOException e) {
                throw new IllegalStateException("MUTF-8编码失败: " + str, e);
            }
        } else {
            // UTF-16
            int charLen = str.length();
            
            int size = 0;
            size += (charLen >= 0x8000) ? 4 : 2;  // charLen编码
            size += charLen * 2;                   // 数据（UTF-16）
            size += 2;                             // 终止符
            return size;
        }
    }
    
    /**
     * 判断是否需要完整重建
     * 
     * @return true=需要重建
     */
    public boolean needsRebuild() {
        return typeStringsModified || keyStringsModified;
    }
    
    /**
     * 计算重建后的package大小
     * 
     * @return 字节大小
     */
    public int calculateRebuildSize() {
        if (!needsRebuild()) {
            return originalSize;
        }
        
        int headerSize = HEADER_SIZE;
        int typeStringsSize = calculateStringPoolSize(typeStrings);
        int keyStringsSize = calculateStringPoolSize(keyStrings);
        int typeSpecsSize = typeSpecs.stream().mapToInt(ResTableTypeSpec::getChunkSize).sum();
        int typesSize = types.stream().mapToInt(ResTableType::getChunkSize).sum();
        
        int totalSize = headerSize + typeStringsSize + keyStringsSize + typeSpecsSize + typesSize;
        
        log.debug("计算重建大小: {}", totalSize);
        
        return totalSize;
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

