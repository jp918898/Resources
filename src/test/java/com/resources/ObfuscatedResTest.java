package com.resources;

import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 混淆APK处理测试
 * 
 * 验证系统对res目录和文件名被混淆后的处理能力
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ObfuscatedResTest {
    
    private static final byte[] SAMPLE_AXML = {
        0x03, 0x00, 0x08, 0x00, // type + headerSize
        0x00, 0x00, 0x00, 0x00  // minimal AXML
    };
    
    private static final byte[] SAMPLE_ARSC = {
        0x02, 0x00, 0x0C, 0x00, // RES_TABLE_TYPE
        0x0C, 0x00, 0x00, 0x00, // size
        0x00, 0x00, 0x00, 0x00  // packageCount
    };
    
    @TempDir
    Path tempDir;
    
    /**
     * 场景1：res子目录被混淆（res/layout → res/a）
     */
    @Test
    public void testObfuscatedResSubdirectories() throws Exception {
        // 创建混淆APK：res/a/*.xml (layout混淆为a)
        Path apkPath = tempDir.resolve("obfuscated-subdir.apk");
        createObfuscatedApk(apkPath.toFile(), "res/a/test.xml", "res/b/menu.xml");
        
        // 加载到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        int fileCount = vfs.loadFromApk(apkPath.toString());
        
        assertEquals(3, fileCount); // resources.arsc + 2个XML
        assertTrue(vfs.exists("res/a/test.xml"));
        assertTrue(vfs.exists("res/b/menu.xml"));
        
        // 验证可以通过全局扫描找到文件
        var allXmls = vfs.listFilesByPattern("res/**/*.xml");
        assertEquals(2, allXmls.size());
    }
    
    /**
     * 场景2：res目录本身被重命名（res → x）
     */
    @Test
    public void testObfuscatedResRootDirectory() throws Exception {
        // 创建混淆APK：x/layout/*.xml (res重命名为x)
        Path apkPath = tempDir.resolve("obfuscated-root.apk");
        createObfuscatedApk(apkPath.toFile(), "x/layout/test.xml", "x/menu/test.xml");
        
        // 加载到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        int fileCount = vfs.loadFromApk(apkPath.toString());
        
        assertEquals(3, fileCount);
        
        // res/**/*.xml应该找不到文件
        var standardScan = vfs.listFilesByPattern("res/**/*.xml");
        assertEquals(0, standardScan.size());
        
        // 但全局扫描应该能找到
        var globalScan = vfs.listFilesByPattern("**/*.xml");
        assertEquals(2, globalScan.size()); // 不包括resources.arsc
    }
    
    /**
     * 场景3：完全混淆（res → a, layout → b）
     */
    @Test
    public void testCompletelyObfuscated() throws Exception {
        // 创建极端混淆APK：a/b/c.xml
        Path apkPath = tempDir.resolve("completely-obfuscated.apk");
        createObfuscatedApk(apkPath.toFile(), "a/b/c.xml", "a/d/e.xml", "x/y/z.xml");
        
        // 加载到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        int fileCount = vfs.loadFromApk(apkPath.toString());
        
        assertEquals(4, fileCount); // resources.arsc + 3个XML
        
        // 全局扫描应该能找到所有XML
        var allXmls = vfs.listFilesByPattern("**/*.xml");
        assertEquals(3, allXmls.size());
        
        // 验证路径保留
        assertTrue(vfs.exists("a/b/c.xml"));
        assertTrue(vfs.exists("a/d/e.xml"));
        assertTrue(vfs.exists("x/y/z.xml"));
    }
    
    /**
     * 场景4：res平铺混淆（所有XML直接在res/下）
     */
    @Test
    public void testFlattenedResDirectory() throws Exception {
        // 创建平铺APK：res/*.xml
        Path apkPath = tempDir.resolve("flattened.apk");
        createObfuscatedApk(apkPath.toFile(), "res/abc.xml", "res/def.xml");
        
        // 加载到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        int fileCount = vfs.loadFromApk(apkPath.toString());
        
        assertEquals(3, fileCount);
        
        // 两种模式都应该能找到
        var standardScan = vfs.listFilesByPattern("res/**/*.xml");
        assertEquals(2, standardScan.size());
        
        var flatScan = vfs.listFilesByPattern("res/*.xml");
        assertEquals(2, flatScan.size());
    }
    
    /**
     * 场景5：混淆APK全局扫描fallback验证
     * 
     * 注：此测试不执行完整的processApk流程（需要DEX验证），
     * 仅验证VFS能正确扫描和处理混淆路径的XML
     */
    @Test
    public void testObfuscatedApkGlobalScanFallback() throws Exception {
        // 创建混淆APK
        Path apkPath = tempDir.resolve("fallback-test.apk");
        createObfuscatedApk(apkPath.toFile(), "x/y/test.xml", "a/b/c.xml");
        
        // 加载到VFS
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.loadFromApk(apkPath.toString());
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        
        // 模拟ResourceProcessor.processAxmlFilesVfs的逻辑
        
        // Level 1: 标准res目录应该为空
        var standardScan = provider.getFilesByPattern("res/**/*.xml");
        assertEquals(0, standardScan.size(), "标准res扫描应该为空（res目录被重命名）");
        
        // Level 2: 全局扫描应该找到所有XML
        var globalScan = provider.getFilesByPattern("**/*.xml");
        assertEquals(2, globalScan.size(), "全局扫描应该找到2个XML文件");
        
        // 验证找到的文件路径正确
        assertTrue(globalScan.containsKey("x/y/test.xml"));
        assertTrue(globalScan.containsKey("a/b/c.xml"));
        
        // 验证过滤逻辑（排除AndroidManifest.xml等）
        // 当前测试APK没有这些文件，所以过滤前后应该相同
        assertEquals(2, filterNonResourceXml(globalScan).size());
    }
    
    /**
     * 过滤非资源XML（复制ResourceProcessor的逻辑）
     */
    private Map<String, byte[]> filterNonResourceXml(Map<String, byte[]> allXmls) {
        Map<String, byte[]> filtered = new LinkedHashMap<>();
        
        for (Map.Entry<String, byte[]> entry : allXmls.entrySet()) {
            String path = entry.getKey();
            
            // 排除已知非资源XML
            if (path.equals("AndroidManifest.xml")) continue;
            if (path.startsWith("META-INF/")) continue;
            if (path.startsWith("original/")) continue;
            if (path.startsWith("kotlin/")) continue;
            
            filtered.put(path, entry.getValue());
        }
        
        return filtered;
    }
    
    /**
     * 场景6：数据保真度验证
     */
    @Test
    public void testDataIntegrity() throws Exception {
        // 创建包含特殊路径的APK
        Path apkPath = tempDir.resolve("integrity-test.apk");
        
        String[] paths = {
            "res/layout/test.xml",      // 标准路径
            "res/layout-land/test.xml", // 带连字符
            "res/a/b.xml",              // 单字符混淆
            "res/a_b/c.xml",            // 下划线
            "x/y/z.xml"                 // 完全混淆
        };
        
        createObfuscatedApk(apkPath.toFile(), paths);
        
        // 加载
        VirtualFileSystem vfs = new VirtualFileSystem();
        int count = vfs.loadFromApk(apkPath.toString());
        
        // 验证所有文件都被正确加载
        assertEquals(paths.length + 1, count); // +1 for resources.arsc
        
        for (String path : paths) {
            assertTrue(vfs.exists(path), "路径应该存在: " + path);
        }
        
        // 导出到新APK
        Path outputPath = tempDir.resolve("integrity-output.apk");
        vfs.saveToApk(outputPath.toString());
        
        // 重新加载验证
        VirtualFileSystem vfsCheck = new VirtualFileSystem();
        int checkCount = vfsCheck.loadFromApk(outputPath.toString());
        
        assertEquals(count, checkCount, "导出后文件数量应该相同");
        
        for (String path : paths) {
            assertTrue(vfsCheck.exists(path), "导出后路径应该存在: " + path);
        }
    }
    
    /**
     * 创建混淆APK（用于测试）
     */
    private void createObfuscatedApk(File apkFile, String... xmlPaths) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(apkFile.toPath()))) {
            // 添加resources.arsc
            ZipEntry arscEntry = new ZipEntry("resources.arsc");
            zos.putNextEntry(arscEntry);
            zos.write(SAMPLE_ARSC);
            zos.closeEntry();
            
            // 添加XML文件
            for (String path : xmlPaths) {
                ZipEntry xmlEntry = new ZipEntry(path);
                zos.putNextEntry(xmlEntry);
                zos.write(SAMPLE_AXML);
                zos.closeEntry();
            }
        }
    }
}

