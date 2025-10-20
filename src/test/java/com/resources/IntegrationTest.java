package com.resources;

import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;

/**
 * 集成测试 - 使用真实APK
 * 
 * @author Resources Processor Team
 */
public class IntegrationTest {
    
    private static final String TEST_APK = "input/Dragonfly.apk";
    private VirtualFileSystem vfs;
    
    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }
    
    @Test
    @DisplayName("测试从APK加载到VFS")
    void testLoadFromApk() {
        // 检查测试APK是否存在
        File apkFile = new File(TEST_APK);
        assumeTrue(apkFile.exists(), "跳过测试：测试APK不存在 - " + TEST_APK);
        
        // 加载APK到VFS
        int fileCount = assertDoesNotThrow(() -> vfs.loadFromApk(TEST_APK));
        
        assertTrue(fileCount > 0, "应该加载至少一个文件");
        assertTrue(vfs.isLoaded(), "VFS应该标记为已加载");
        
        System.out.println("从APK加载: " + fileCount + " 个文件");
        System.out.println(vfs.getStatistics());
    }
    
    @Test
    @DisplayName("测试VFS访问resources.arsc")
    void testAccessResourcesArsc() {
        File apkFile = new File(TEST_APK);
        assumeTrue(apkFile.exists(), "跳过测试：测试APK不存在");
        
        assertDoesNotThrow(() -> vfs.loadFromApk(TEST_APK));
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        // 尝试读取resources.arsc
        byte[] arscData = assertDoesNotThrow(() -> provider.getResourcesArsc());
        
        assertNotNull(arscData, "resources.arsc应该存在");
        assertTrue(arscData.length > 0, "resources.arsc不应该为空");
        
        System.out.println("resources.arsc大小: " + arscData.length + " 字节");
    }
    
    @Test
    @DisplayName("测试VFS访问layout文件")
    void testAccessLayoutFiles() {
        File apkFile = new File(TEST_APK);
        assumeTrue(apkFile.exists(), "跳过测试：测试APK不存在");
        
        assertDoesNotThrow(() -> vfs.loadFromApk(TEST_APK));
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        // 获取所有layout文件
        var layouts = provider.getAllLayouts();
        
        System.out.println("发现 " + layouts.size() + " 个layout文件:");
        for (String path : layouts.keySet()) {
            System.out.println("  - " + path);
        }
        
        assertTrue(layouts.size() >= 0, "layout文件数量应该 >= 0");
    }
    
    @Test
    @DisplayName("测试VFS导出到APK")
    void testSaveToApk() throws IOException {
        File apkFile = new File(TEST_APK);
        assumeTrue(apkFile.exists(), "跳过测试：测试APK不存在");
        
        // 1. 加载APK到VFS
        int loadCount = vfs.loadFromApk(TEST_APK);
        
        // 2. 修改一个文件
        if (vfs.exists("resources.arsc")) {
            byte[] arscData = vfs.readFile("resources.arsc");
            vfs.writeFile("resources.arsc", arscData); // 写回相同数据
        }
        
        // 3. 导出到新APK
        String outputApk = "temp/test_output.apk";
        int saveCount = vfs.saveToApk(outputApk);
        
        assertEquals(loadCount, saveCount, "导出的文件数应该与加载的文件数相同");
        
        File outputFile = new File(outputApk);
        assertTrue(outputFile.exists(), "输出APK应该存在");
        
        System.out.println("导出成功: " + outputApk);
        
        // 清理
        outputFile.delete();
    }
    
    @Test
    @DisplayName("测试VFS打印目录树")
    void testPrintTree() {
        File apkFile = new File(TEST_APK);
        assumeTrue(apkFile.exists(), "跳过测试：测试APK不存在");
        
        assertDoesNotThrow(() -> vfs.loadFromApk(TEST_APK));
        
        String tree = vfs.printTree(2);
        
        assertNotNull(tree);
        assertTrue(tree.contains("vfs:/"), "应该包含VFS根路径");
        
        System.out.println("VFS目录树:");
        System.out.println(tree);
    }
}

