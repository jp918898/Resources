package com.resources;

import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * VFS系统测试
 * 
 * @author Resources Processor Team
 */
public class VfsTest {
    
    private VirtualFileSystem vfs;
    
    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }
    
    @Test
    @DisplayName("测试VFS基本读写")
    void testVfsBasicReadWrite() {
        // 1. 写入文件
        String testPath = "test.txt";
        byte[] testData = "Hello VFS!".getBytes(StandardCharsets.UTF_8);
        
        vfs.writeFile(testPath, testData);
        
        // 2. 读取文件
        byte[] readData = assertDoesNotThrow(() -> vfs.readFile(testPath));
        
        assertArrayEquals(testData, readData, "读取的数据应该与写入的数据相同");
    }
    
    @Test
    @DisplayName("测试VFS文件修改")
    void testVfsFileModification() {
        String path = "test.txt";
        byte[] original = "Original".getBytes(StandardCharsets.UTF_8);
        byte[] modified = "Modified".getBytes(StandardCharsets.UTF_8);
        
        // 1. 写入原始数据
        vfs.writeFile(path, original);
        
        // 2. 修改数据
        vfs.writeFile(path, modified);
        
        // 3. 验证修改
        byte[] result = assertDoesNotThrow(() -> vfs.readFile(path));
        assertArrayEquals(modified, result);
        
        // 4. 检查已修改文件列表
        List<String> modifiedFiles = vfs.getModifiedFiles();
        assertTrue(modifiedFiles.contains(path), "应该在已修改文件列表中");
    }
    
    @Test
    @DisplayName("测试VFS路径规范化")
    void testVfsPathNormalization() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        
        // 不同的路径格式应该指向同一个文件
        vfs.writeFile("vfs:/test.txt", data);
        
        assertTrue(vfs.exists("test.txt"));
        assertTrue(vfs.exists("vfs:/test.txt"));
        assertTrue(vfs.exists("/test.txt"));
    }
    
    @Test
    @DisplayName("测试VFS目录列表")
    void testVfsListDirectory() {
        // 创建测试文件
        vfs.writeFile("res/layout/activity_main.xml", new byte[10]);
        vfs.writeFile("res/layout/fragment_test.xml", new byte[20]);
        vfs.writeFile("res/menu/main_menu.xml", new byte[30]);
        
        // 列出res/layout目录
        List<String> layoutFiles = vfs.listFiles("res/layout");
        
        assertEquals(2, layoutFiles.size(), "应该有2个layout文件");
        assertTrue(layoutFiles.contains("res/layout/activity_main.xml"));
        assertTrue(layoutFiles.contains("res/layout/fragment_test.xml"));
    }
    
    @Test
    @DisplayName("测试VFS模式匹配")
    void testVfsPatternMatching() {
        // 创建测试文件
        vfs.writeFile("res/layout/activity_main.xml", new byte[10]);
        vfs.writeFile("res/layout-land/activity_main.xml", new byte[15]);
        vfs.writeFile("res/menu/main.xml", new byte[20]);
        
        // 匹配所有XML文件
        List<String> xmlFiles = vfs.listFilesByPattern("res/**/*.xml");
        
        assertEquals(3, xmlFiles.size(), "应该匹配3个XML文件");
        
        // 匹配只layout目录下的XML
        List<String> layoutFiles = vfs.listFilesByPattern("res/layout/*.xml");
        assertEquals(1, layoutFiles.size(), "应该匹配1个layout文件");
    }
    
    @Test
    @DisplayName("测试VFS统计信息")
    void testVfsStatistics() {
        vfs.writeFile("file1.txt", new byte[100]);
        vfs.writeFile("file2.txt", new byte[200]);
        
        assertEquals(2, vfs.getFileCount());
        assertEquals(300, vfs.getTotalSize());
        
        String stats = vfs.getStatistics();
        assertTrue(stats.contains("文件=2"));
        assertTrue(stats.contains("总大小=300"));
    }
    
    @Test
    @DisplayName("测试VfsResourceProvider")
    void testVfsResourceProvider() {
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        // 写入resources.arsc
        byte[] arscData = "ARSC_TEST".getBytes(StandardCharsets.UTF_8);
        provider.setResourcesArsc(arscData);
        
        // 读取resources.arsc
        byte[] readData = assertDoesNotThrow(() -> provider.getResourcesArsc());
        assertArrayEquals(arscData, readData);
    }
    
    @Test
    @DisplayName("测试路径规范化 - 处理. 和 ..")
    void testPathNormalizationDotSegments() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        
        // 应该规范化./
        vfs.writeFile("res/./layout/test.xml", data);
        assertTrue(vfs.exists("res/layout/test.xml"), 
            "res/./layout/test.xml 应该规范化为 res/layout/test.xml");
        
        // 应该规范化//
        vfs.writeFile("res//layout//test.xml", data);
        assertTrue(vfs.exists("res/layout/test.xml"),
            "res//layout//test.xml 应该规范化为 res/layout/test.xml");
        
        // .. 应该被处理（安全：移除上级路径）
        vfs.writeFile("res/layout/../values/test.xml", data);
        assertTrue(vfs.exists("res/values/test.xml"),
            "res/layout/../values/test.xml 应该规范化为 res/values/test.xml");
    }
    
    @Test
    @DisplayName("测试恶意路径拒绝")
    void testMaliciousPathsRejected() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        
        // 注意：../路径不会被拒绝，而是被安全规范化
        // "../etc/passwd" -> "etc/passwd" (安全的相对路径)
        // 这是安全的设计：无法越界到VFS之外
        
        // NULL字符攻击 - 应该被拒绝
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile("test\0.xml", data),
            "应该拒绝包含NULL字符的路径");
        
        // 非法字符 - 应该被拒绝
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile("test<>.xml", data),
            "应该拒绝包含<>字符的路径");
            
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile("test|.xml", data),
            "应该拒绝包含|字符的路径");
        
        // 控制字符 - 应该被拒绝
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile("test\u0001.xml", data),
            "应该拒绝包含控制字符的路径");
    }
    
    @Test
    @DisplayName("测试路径组件长度限制")
    void testPathComponentLengthLimit() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        
        // 超长文件名（>255字符）
        String longName = "a".repeat(300) + ".xml";
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile("res/" + longName, data),
            "应该拒绝超长文件名");
    }
    
    @Test
    @DisplayName("测试路径总长度限制")
    void testPathTotalLengthLimit() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        
        // 创建超长路径（>4096字符）
        // 使用单个超长的路径组件
        String longComponent = "a".repeat(5000);
        String longPath = "res/" + longComponent + "/test.xml";
        
        assertTrue(longPath.length() > 4096,
            "测试路径应该超过4096字符: 实际=" + longPath.length());
        
        assertThrows(IllegalArgumentException.class, () -> 
            vfs.writeFile(longPath, data),
            "应该拒绝超长路径");
    }
}

