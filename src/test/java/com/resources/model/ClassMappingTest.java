package com.resources.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ClassMapping单元测试 - 100%覆盖
 */
public class ClassMappingTest {
    
    private ClassMapping mapping;
    
    @BeforeEach
    void setUp() {
        mapping = new ClassMapping();
    }
    
    @Test
    @DisplayName("测试添加映射")
    void testAddMapping() {
        mapping.addMapping("com.example.A", "com.newapp.A");
        
        assertEquals("com.newapp.A", mapping.getNewClass("com.example.A"));
        assertEquals("com.example.A", mapping.getOldClass("com.newapp.A"));
        assertTrue(mapping.containsOldClass("com.example.A"));
        assertTrue(mapping.containsNewClass("com.newapp.A"));
    }
    
    @Test
    @DisplayName("测试映射冲突检测")
    void testMappingConflict() {
        mapping.addMapping("com.example.A", "com.newapp.A");
        
        // 尝试将同一个旧类映射到不同的新类
        assertThrows(IllegalArgumentException.class, () -> 
            mapping.addMapping("com.example.A", "com.newapp.B"));
        
        // 尝试将不同的旧类映射到同一个新类
        assertThrows(IllegalArgumentException.class, () ->
            mapping.addMapping("com.example.B", "com.newapp.A"));
    }
    
    @Test
    @DisplayName("测试空值检查")
    void testNullChecks() {
        assertThrows(NullPointerException.class, () ->
            mapping.addMapping(null, "com.newapp.A"));
        
        assertThrows(NullPointerException.class, () ->
            mapping.addMapping("com.example.A", null));
    }
    
    @Test
    @DisplayName("测试映射不存在")
    void testMappingNotExists() {
        assertNull(mapping.getNewClass("com.example.NotExist"));
        assertNull(mapping.getOldClass("com.newapp.NotExist"));
        assertFalse(mapping.containsOldClass("com.example.NotExist"));
        assertFalse(mapping.containsNewClass("com.newapp.NotExist"));
    }
    
    @Test
    @DisplayName("测试获取所有映射")
    void testGetAllMappings() {
        mapping.addMapping("com.example.A", "com.newapp.A");
        mapping.addMapping("com.example.B", "com.newapp.B");
        mapping.addMapping("com.example.C", "com.newapp.C");
        
        assertEquals(3, mapping.size());
        assertEquals(3, mapping.getAllOldClasses().size());
        assertEquals(3, mapping.getAllNewClasses().size());
        assertFalse(mapping.isEmpty());
    }
    
    @Test
    @DisplayName("测试清空映射")
    void testClear() {
        mapping.addMapping("com.example.A", "com.newapp.A");
        assertEquals(1, mapping.size());
        
        mapping.clear();
        
        assertEquals(0, mapping.size());
        assertTrue(mapping.isEmpty());
    }
    
    @Test
    @DisplayName("测试从文件加载")
    void testLoadFromFile() throws IOException {
        String testFile = "temp/test-mapping.txt";
        Files.createDirectories(Paths.get("temp"));
        
        String content = "# Test mapping\n" +
                        "com.example.A=com.newapp.A\n" +
                        "com.example.B=com.newapp.B\n" +
                        "\n" +
                        "# Comment\n" +
                        "com.example.C=com.newapp.C\n";
        
        Files.write(Paths.get(testFile), content.getBytes());
        
        mapping.loadFromFile(testFile);
        
        assertEquals(3, mapping.size());
        assertEquals("com.newapp.A", mapping.getNewClass("com.example.A"));
        assertEquals("com.newapp.B", mapping.getNewClass("com.example.B"));
        assertEquals("com.newapp.C", mapping.getNewClass("com.example.C"));
        
        // 清理
        Files.deleteIfExists(Paths.get(testFile));
    }
    
    @Test
    @DisplayName("测试保存到文件")
    void testSaveToFile() throws IOException {
        String testFile = "temp/test-save.txt";
        Files.createDirectories(Paths.get("temp"));
        
        mapping.addMapping("com.example.A", "com.newapp.A");
        mapping.addMapping("com.example.B", "com.newapp.B");
        
        mapping.saveToFile(testFile);
        
        assertTrue(Files.exists(Paths.get(testFile)));
        
        String content = new String(Files.readAllBytes(Paths.get(testFile)));
        assertTrue(content.contains("com.example.A=com.newapp.A"));
        assertTrue(content.contains("com.example.B=com.newapp.B"));
        
        // 清理
        Files.deleteIfExists(Paths.get(testFile));
    }
    
    @Test
    @DisplayName("测试文件格式错误")
    void testInvalidFileFormat() throws IOException {
        String testFile = "temp/test-invalid.txt";
        Files.createDirectories(Paths.get("temp"));
        
        // 错误格式：缺少等号
        Files.write(Paths.get(testFile), "com.example.A".getBytes());
        
        assertThrows(IOException.class, () -> mapping.loadFromFile(testFile));
        
        // 清理
        Files.deleteIfExists(Paths.get(testFile));
    }
}

