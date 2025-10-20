package com.resources.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZipAlignUtil单元测试
 */
class ZipAlignUtilTest {
    
    @Test
    void testIsAvailable() {
        // 测试zipalign工具是否存在
        boolean available = ZipAlignUtil.isAvailable();
        
        File zipalignFile = new File("bin/win/zipalign.exe");
        assertEquals(zipalignFile.exists(), available);
    }
    
    @Test
    void testAlign_invalidAlignment() {
        assertThrows(IllegalArgumentException.class, 
                    () -> ZipAlignUtil.align("input.apk", "output.apk", 3),
                    "非法的alignment应该抛异常");
    }
    
    @Test
    void testAlign_nullInput() {
        assertThrows(NullPointerException.class,
                    () -> ZipAlignUtil.align(null, "output.apk", 4));
    }
    
    @Test
    void testAlign_nullOutput() {
        assertThrows(NullPointerException.class,
                    () -> ZipAlignUtil.align("input.apk", null, 4));
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testAlign_nonexistentInput() {
        assertThrows(Exception.class,
                    () -> ZipAlignUtil.align("nonexistent.apk", "output.apk", 4),
                    "不存在的输入文件应该抛异常");
    }
}

