package com.resources.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * PackageMapping单元测试 - 100%覆盖
 */
public class PackageMappingTest {
    
    private PackageMapping mapping;
    
    @BeforeEach
    void setUp() {
        mapping = new PackageMapping();
    }
    
    @Test
    @DisplayName("测试前缀匹配映射")
    void testPrefixMapping() {
        mapping.addPrefixMapping("com.example", "com.newapp");
        
        // 精确匹配
        assertEquals("com.newapp", mapping.replace("com.example"));
        
        // 前缀匹配
        assertEquals("com.newapp.ui", mapping.replace("com.example.ui"));
        assertEquals("com.newapp.ui.MainActivity", mapping.replace("com.example.ui.MainActivity"));
        
        // 不匹配（不是完整的包名分隔）
        assertEquals("com.examples", mapping.replace("com.examples"));
    }
    
    @Test
    @DisplayName("测试精确匹配映射")
    void testExactMapping() {
        mapping.addExactMapping("com.example", "com.newapp");
        
        // 精确匹配
        assertEquals("com.newapp", mapping.replace("com.example"));
        
        // 子包不匹配
        assertEquals("com.example.ui", mapping.replace("com.example.ui"));
    }
    
    @Test
    @DisplayName("测试多个映射优先级")
    void testMultipleMappingPriority() {
        // 添加多个映射，长的前缀应该优先匹配
        mapping.addPrefixMapping("com.example", "com.newapp");
        mapping.addPrefixMapping("com.example.ui", "com.custom.ui");
        
        // 更具体的前缀优先
        assertEquals("com.custom.ui", mapping.replace("com.example.ui"));
        assertEquals("com.custom.ui.MainActivity", mapping.replace("com.example.ui.MainActivity"));
        
        // 不太具体的前缀
        assertEquals("com.newapp.data", mapping.replace("com.example.data"));
    }
    
    @Test
    @DisplayName("测试映射冲突检测")
    void testMappingConflict() {
        mapping.addMapping("com.example", "com.newapp", PackageMapping.ReplaceMode.PREFIX_MATCH);
        
        // 尝试将同一个包映射到不同的新包
        assertThrows(IllegalArgumentException.class, () ->
            mapping.addMapping("com.example", "com.other", PackageMapping.ReplaceMode.PREFIX_MATCH));
    }
    
    @Test
    @DisplayName("测试空值检查")
    void testNullChecks() {
        assertThrows(NullPointerException.class, () ->
            mapping.addMapping(null, "com.newapp", PackageMapping.ReplaceMode.PREFIX_MATCH));
        
        assertThrows(NullPointerException.class, () ->
            mapping.addMapping("com.example", null, PackageMapping.ReplaceMode.PREFIX_MATCH));
        
        assertThrows(NullPointerException.class, () ->
            mapping.addMapping("com.example", "com.newapp", null));
    }
    
    @Test
    @DisplayName("测试获取所有映射")
    void testGetAllMappings() {
        mapping.addPrefixMapping("com.example", "com.newapp");
        mapping.addPrefixMapping("com.test", "com.newtest");
        
        assertEquals(2, mapping.size());
        assertEquals(2, mapping.getAllMappings().size());
        assertFalse(mapping.isEmpty());
    }
    
    @Test
    @DisplayName("测试清空映射")
    void testClear() {
        mapping.addPrefixMapping("com.example", "com.newapp");
        assertEquals(1, mapping.size());
        
        mapping.clear();
        
        assertEquals(0, mapping.size());
        assertTrue(mapping.isEmpty());
    }
    
    @Test
    @DisplayName("测试从文件加载")
    void testLoadFromFile() throws IOException {
        String testFile = "temp/test-pkg-mapping.txt";
        Files.createDirectories(Paths.get("temp"));
        
        String content = "# Package mapping\n" +
                        "com.example=com.newapp\n" +
                        "com.test=com.newtest,EXACT_MATCH\n";
        
        Files.write(Paths.get(testFile), content.getBytes());
        
        mapping.loadFromFile(testFile);
        
        assertEquals(2, mapping.size());
        
        // 清理
        Files.deleteIfExists(Paths.get(testFile));
    }
    
    @Test
    @DisplayName("测试保存到文件")
    void testSaveToFile() throws IOException {
        String testFile = "temp/test-pkg-save.txt";
        Files.createDirectories(Paths.get("temp"));
        
        mapping.addPrefixMapping("com.example", "com.newapp");
        mapping.addExactMapping("com.test", "com.newtest");
        
        mapping.saveToFile(testFile);
        
        assertTrue(Files.exists(Paths.get(testFile)));
        
        String content = new String(Files.readAllBytes(Paths.get(testFile)));
        assertTrue(content.contains("com.example=com.newapp"));
        assertTrue(content.contains("com.test=com.newtest"));
        
        // 清理
        Files.deleteIfExists(Paths.get(testFile));
    }
}

