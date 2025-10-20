package com.resources.arsc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResTablePackage完整重建功能测试
 */
class ResTablePackageRebuildTest {
    
    @Test
    void testNeedsRebuild_noModification() {
        // 创建package（未修改）
        ResTablePackage pkg = createMinimalPackage();
        
        assertFalse(pkg.needsRebuild(), "未修改时不应该需要重建");
    }
    
    @Test
    void testNeedsRebuild_afterNameModification() {
        ResTablePackage pkg = createMinimalPackage();
        
        // 只修改包名
        pkg.setName("new.package.name");
        
        assertFalse(pkg.needsRebuild(), "只修改包名不需要重建");
    }
    
    @Test
    void testCalculateRebuildSize_noModification() {
        ResTablePackage pkg = createMinimalPackage();
        
        int rebuildSize = pkg.calculateRebuildSize();
        
        assertTrue(rebuildSize > 0, "重建大小应该大于0");
        assertEquals(pkg.getOriginalSize(), rebuildSize, 
                    "未修改时重建大小应该等于原始大小");
    }
    
    @Test
    void testSetTypeString_throwsException_whenNotInitialized() {
        ResTablePackage pkg = new ResTablePackage();
        
        assertThrows(IllegalStateException.class, 
                    () -> pkg.setTypeString(0, "test"),
                    "typeStrings未初始化时应该抛异常");
    }
    
    @Test
    void testSetKeyString_throwsException_whenNotInitialized() {
        ResTablePackage pkg = new ResTablePackage();
        
        assertThrows(IllegalStateException.class,
                    () -> pkg.setKeyString(0, "test"),
                    "keyStrings未初始化时应该抛异常");
    }
    
    /**
     * 创建最小化的测试package
     */
    private ResTablePackage createMinimalPackage() {
        // 创建一个最小的package chunk用于测试
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        
        int headerSize = 288;
        int chunkSize = headerSize;  // 暂时只包含header
        
        // 1. 写入package header
        buffer.putShort((short) 0x0200);  // RES_TABLE_PACKAGE_TYPE
        buffer.putShort((short) headerSize);
        buffer.putInt(chunkSize);
        buffer.putInt(0x7f);  // packageId
        
        // 2. 写入包名 (char16_t[128])
        String testPackage = "com.test.app";
        for (int i = 0; i < 128; i++) {
            if (i < testPackage.length()) {
                buffer.putChar(testPackage.charAt(i));
            } else {
                buffer.putChar((char) 0);
            }
        }
        
        // 3. 写入偏移字段
        buffer.putInt(0);  // typeStringsOffset
        buffer.putInt(0);  // lastPublicType
        buffer.putInt(0);  // keyStringsOffset
        buffer.putInt(0);  // lastPublicKey
        buffer.putInt(0);  // typeIdOffset
        
        buffer.flip();
        
        // 解析package（这会初始化typeStrings和keyStrings为null）
        ResTablePackage pkg = new ResTablePackage();
        
        try {
            pkg.parse(buffer);
        } catch (Exception e) {
            // 预期会失败，因为没有字符串池
            // 但至少会初始化基本字段
        }
        
        return pkg;
    }
}

