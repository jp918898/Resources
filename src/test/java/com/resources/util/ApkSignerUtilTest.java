package com.resources.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApkSignerUtil单元测试
 */
class ApkSignerUtilTest {
    
    @Test
    void testIsAvailable() {
        // 测试apksigner工具是否存在
        boolean available = ApkSignerUtil.isAvailable();
        
        File apksignerFile = new File("bin/win/apksigner.bat");
        assertEquals(apksignerFile.exists(), available);
    }
    
    @Test
    void testIsTestKeystoreAvailable() {
        // 测试密钥库是否存在
        boolean available = ApkSignerUtil.isTestKeystoreAvailable();
        
        File keystoreFile = new File("config/keystore/testkey.jks");
        assertEquals(keystoreFile.exists() && keystoreFile.canRead(), available);
    }
    
    @Test
    void testSign_nullApk() {
        assertThrows(NullPointerException.class,
                    () -> ApkSignerUtil.sign(null, "keystore.jks", "pass", "pass"));
    }
    
    @Test
    void testSign_nullKeystore() {
        assertThrows(NullPointerException.class,
                    () -> ApkSignerUtil.sign("test.apk", null, "pass", "pass"));
    }
    
    @Test
    void testSign_nullPassword() {
        assertThrows(NullPointerException.class,
                    () -> ApkSignerUtil.sign("test.apk", "keystore.jks", null, "pass"));
        
        assertThrows(NullPointerException.class,
                    () -> ApkSignerUtil.sign("test.apk", "keystore.jks", "pass", null));
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testSign_nonexistentApk() {
        assertThrows(Exception.class,
                    () -> ApkSignerUtil.signWithTestKey("nonexistent.apk"),
                    "不存在的APK应该抛异常");
    }
}

