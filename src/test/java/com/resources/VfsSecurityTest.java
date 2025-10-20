package com.resources;

import com.resources.util.VirtualFileSystem;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/**
 * VFS安全性测试
 * 
 * 测试目标：
 * 1. 目录遍历攻击防护
 * 2. ZIP Slip攻击防护
 * 3. 路径注入防护
 * 4. 特殊字符处理
 * 5. 边界条件测试
 * 
 * @author Resources Processor Team
 */
@DisplayName("VFS安全性测试")
public class VfsSecurityTest {
    
    private VirtualFileSystem vfs;
    
    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }
    
    @Nested
    @DisplayName("路径遍历攻击测试")
    class DirectoryTraversalTests {
        
        @Test
        @DisplayName("测试1: 路径规范化安全处理")
        void testPathNormalizationSafety() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 修复后：所有包含..的路径都会被安全规范化
            // "../etc/passwd" -> "etc/passwd" (安全：无法越界)
            // "res/../config.xml" -> "config.xml" (安全：正常的上级引用)
            
            // 测试1: 相对路径规范化
            vfs.writeFile("res/../config.xml", data);
            assertTrue(vfs.exists("config.xml"), 
                "res/../config.xml 应该规范化为 config.xml");
            // 注意：exists()也会规范化路径，所以exists("res/../config.xml")
            // 会被规范化为exists("config.xml")，返回true
            assertTrue(vfs.exists("res/../config.xml"),
                "exists()也会规范化路径");
            
            // 测试2: 越界路径被安全处理
            vfs.writeFile("../etc/passwd", data);
            assertTrue(vfs.exists("etc/passwd"),
                "../etc/passwd 应该规范化为 etc/passwd（防止越界）");
            
            // 测试3: 多级..被安全处理
            vfs.writeFile("../../system/config", data);
            assertTrue(vfs.exists("system/config"),
                "../../system/config 应该规范化为 system/config");
            
            System.out.println("✓ 路径规范化安全性验证通过");
            System.out.println("  - 所有..路径都被安全处理");
            System.out.println("  - 无法越界到VFS之外");
        }
        
        @Test
        @DisplayName("测试2: 复杂路径规范化")
        void testComplexPathNormalization() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 复杂的../组合
            String path = "res/../config/../secret.xml";
            vfs.writeFile(path, data);
            
            // 验证：应该规范化为 secret.xml
            assertTrue(vfs.exists("secret.xml"),
                "res/../config/../secret.xml 应该规范化为 secret.xml");
            
            // 验证：规范化后的路径不包含..
            for (String storedPath : vfs.getAllPaths()) {
                assertFalse(storedPath.contains(".."),
                    "VFS中不应存储包含..的路径: " + storedPath);
            }
            
            System.out.println("✓ 复杂路径规范化验证通过");
        }
        
        @Test
        @DisplayName("测试3: 绝对路径转换")
        void testAbsolutePathConversion() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 绝对路径应该被转换为相对路径
            vfs.writeFile("/etc/passwd", data);
            assertTrue(vfs.exists("etc/passwd"),
                "/etc/passwd 应该规范化为 etc/passwd");
            
            vfs.writeFile("/var/log/system.log", data);
            assertTrue(vfs.exists("var/log/system.log"),
                "/var/log/system.log 应该规范化为 var/log/system.log");
            
            // 验证：VFS中不应有以/开头的路径
            for (String path : vfs.getAllPaths()) {
                assertFalse(path.startsWith("/"),
                    "VFS路径不应以/开头: " + path);
            }
            
            System.out.println("✓ 绝对路径转换验证通过");
        }
    }
    
    @Nested
    @DisplayName("ZIP Slip攻击测试")
    class ZipSlipTests {
        
        @Test
        @DisplayName("测试4: ZIP Slip防护")
        void testZipSlipProtection() throws Exception {
            // 创建包含恶意Entry的ZIP文件
            File tempZip = File.createTempFile("malicious", ".apk");
            tempZip.deleteOnExit();
            
            try (ZipOutputStream zos = new ZipOutputStream(
                    new FileOutputStream(tempZip))) {
                
                // 正常Entry
                ZipEntry normalEntry = new ZipEntry("res/layout/test.xml");
                zos.putNextEntry(normalEntry);
                zos.write("<xml/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                
                // 恶意Entry 1: 包含../（会被规范化）
                ZipEntry maliciousEntry1 = new ZipEntry("res/../../etc/passwd");
                zos.putNextEntry(maliciousEntry1);
                zos.write("malicious".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                
                // 恶意Entry 2: 绝对路径（会被规范化）
                ZipEntry maliciousEntry2 = new ZipEntry("/etc/hosts");
                zos.putNextEntry(maliciousEntry2);
                zos.write("malicious".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                
                // 恶意Entry 3: 包含非法字符（会被跳过）
                ZipEntry maliciousEntry3 = new ZipEntry("test\0file.xml");
                zos.putNextEntry(maliciousEntry3);
                zos.write("malicious".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            
            // 加载恶意ZIP
            int loadedCount = vfs.loadFromApk(tempZip.getAbsolutePath());
            
            // 验证：应该加载3个文件（1个正常 + 2个规范化后的恶意Entry）
            // 第3个恶意Entry包含NULL字符会被跳过
            assertEquals(3, loadedCount, "应该加载3个文件");
            
            // 验证：所有路径都被安全规范化
            for (String path : vfs.getAllPaths()) {
                assertFalse(path.contains(".."), 
                    "不应包含..: " + path);
                assertFalse(path.startsWith("/"),
                    "不应以/开头: " + path);
                assertFalse(path.contains("\0"),
                    "不应包含NULL: " + path);
            }
            
            // 验证：恶意路径被规范化为安全路径
            assertTrue(vfs.exists("res/layout/test.xml"),
                "正常Entry应该存在");
            assertTrue(vfs.exists("etc/passwd"),
                "res/../../etc/passwd 应该规范化为 etc/passwd");
            assertTrue(vfs.exists("etc/hosts"),
                "/etc/hosts 应该规范化为 etc/hosts");
            
            System.out.println("✓ ZIP Slip防护验证通过");
            System.out.println("  - 所有恶意路径都被安全处理");
            System.out.println("  - 包含非法字符的Entry被跳过");
        }
        
        @Test
        @DisplayName("测试5: ZIP路径规范化")
        void testZipPathNormalization() throws Exception {
            File tempZip = File.createTempFile("test", ".apk");
            tempZip.deleteOnExit();
            
            try (ZipOutputStream zos = new ZipOutputStream(
                    new FileOutputStream(tempZip))) {
                
                // 各种需要规范化的路径
                String[] entries = {
                    "res/layout/test.xml",
                    "res/layout/../layout/test2.xml",
                    "./res/values/strings.xml",
                    "res//layout//test3.xml",  // 双斜杠
                    "res/layout/./test4.xml"
                };
                
                for (String entry : entries) {
                    ZipEntry ze = new ZipEntry(entry);
                    zos.putNextEntry(ze);
                    zos.write("test".getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            
            vfs.loadFromApk(tempZip.getAbsolutePath());
            
            // 验证：应该加载5个文件
            assertEquals(5, vfs.getFileCount(), "应该加载5个文件");
            
            // 验证所有路径都被正确规范化
            for (String path : vfs.getAllPaths()) {
                assertFalse(path.contains(".."), "路径不应包含..: " + path);
                assertFalse(path.contains("//"), "路径不应包含双斜杠: " + path);
                assertFalse(path.contains("/./"), "路径不应包含/./: " + path);
                assertFalse(path.startsWith("./"), "路径不应以./开头: " + path);
                assertFalse(path.startsWith("/"), "路径不应以/开头: " + path);
            }
            
            // 验证规范化后的路径
            assertTrue(vfs.exists("res/layout/test.xml"));
            assertTrue(vfs.exists("res/layout/test2.xml"),  // res/layout/../layout/test2.xml -> res/layout/test2.xml
                "res/layout/../layout/test2.xml 应该规范化为 res/layout/test2.xml");
            assertTrue(vfs.exists("res/values/strings.xml"),  // ./res/values/strings.xml -> res/values/strings.xml
                "./res/values/strings.xml 应该规范化为 res/values/strings.xml");
            assertTrue(vfs.exists("res/layout/test3.xml"),  // res//layout//test3.xml -> res/layout/test3.xml
                "res//layout//test3.xml 应该规范化为 res/layout/test3.xml");
            assertTrue(vfs.exists("res/layout/test4.xml"),  // res/layout/./test4.xml -> res/layout/test4.xml
                "res/layout/./test4.xml 应该规范化为 res/layout/test4.xml");
            
            System.out.println("✓ ZIP路径规范化验证通过");
        }
    }
    
    @Nested
    @DisplayName("特殊字符测试")
    class SpecialCharacterTests {
        
        @Test
        @DisplayName("测试6: NULL字符拒绝")
        void testNullCharacterRejection() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // NULL字符应该被拒绝（路径截断攻击）
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile("res/test\0.xml", data),
                "应该拒绝包含NULL字符的路径");
            
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile("test\0file.xml", data),
                "应该拒绝包含NULL字符的路径");
            
            System.out.println("✓ NULL字符拒绝验证通过");
        }
        
        @Test
        @DisplayName("测试7: 控制字符拒绝")
        void testControlCharactersRejection() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 各种控制字符都应该被拒绝
            char[] controlChars = {
                '\u0000', '\u0001', '\u0007', '\u001F',  // ASCII控制字符
                '\u007F'  // DEL
            };
            
            for (char c : controlChars) {
                String path = "res/test" + c + ".xml";
                assertThrows(IllegalArgumentException.class, () -> 
                    vfs.writeFile(path, data),
                    "应该拒绝控制字符: \\u" + String.format("%04X", (int)c));
            }
            
            System.out.println("✓ 控制字符拒绝验证通过");
        }
        
        @Test
        @DisplayName("测试8: 非法文件名字符拒绝")
        void testIllegalFilenameCharactersRejection() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // Windows/Unix非法字符都应该被拒绝
            char[] illegalChars = {'<', '>', ':', '"', '|', '?', '*'};
            
            for (char c : illegalChars) {
                String path = "res/test" + c + ".xml";
                assertThrows(IllegalArgumentException.class, () -> 
                    vfs.writeFile(path, data),
                    "应该拒绝非法字符: " + c);
            }
            
            System.out.println("✓ 非法字符拒绝验证通过");
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {
        
        @Test
        @DisplayName("测试9: 超长路径拒绝")
        void testVeryLongPathRejection() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 创建超长路径（超过4096字符）
            StringBuilder sb = new StringBuilder("res/");
            for (int i = 0; i < 500; i++) {
                sb.append("verylongdirectoryname/");
            }
            sb.append("test.xml");
            
            String longPath = sb.toString();
            assertTrue(longPath.length() > 4096, 
                "测试路径应该超过4096字符");
            
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile(longPath, data),
                "应该拒绝超长路径");
            
            System.out.println("✓ 超长路径拒绝验证通过 (" + longPath.length() + " 字符)");
        }
        
        @Test
        @DisplayName("测试10: 超长文件名组件拒绝")
        void testVeryLongFilenameComponentRejection() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 创建超长文件名（超过255字符）
            String longFilename = "a".repeat(300) + ".xml";
            String path = "res/layout/" + longFilename;
            
            assertTrue(longFilename.length() > 255,
                "测试文件名应该超过255字符");
            
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile(path, data),
                "应该拒绝超长文件名");
            
            System.out.println("✓ 超长文件名拒绝验证通过 (" + longFilename.length() + " 字符)");
        }
        
        @Test
        @DisplayName("测试11: 特殊路径处理")
        void testSpecialPaths() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 空路径应该被规范化为空字符串（合法）
            assertDoesNotThrow(() -> vfs.writeFile("", data));
            assertTrue(vfs.exists(""));
            
            // . 应该被规范化为空（当前目录）
            assertDoesNotThrow(() -> vfs.writeFile(".", data));
            assertTrue(vfs.exists(""));
            
            // .. 单独出现会被规范化为空
            assertDoesNotThrow(() -> vfs.writeFile("..", data));
            assertTrue(vfs.exists(""));
            
            // / 应该被规范化为空
            assertDoesNotThrow(() -> vfs.writeFile("/", data));
            assertTrue(vfs.exists(""));
            
            // 控制字符应该被拒绝
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile("\t", data));
            assertThrows(IllegalArgumentException.class, () -> 
                vfs.writeFile("\n", data));
            
            System.out.println("✓ 特殊路径处理验证通过");
        }
    }
    
    @Nested
    @DisplayName("Windows路径测试")
    class WindowsPathTests {
        
        @Test
        @DisplayName("测试12: Windows路径规范化")
        void testWindowsPathNormalization() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // Windows反斜杠应该被转换为正斜杠
            vfs.writeFile("res\\layout\\test.xml", data);
            assertTrue(vfs.exists("res/layout/test.xml"),
                "res\\layout\\test.xml 应该规范化为 res/layout/test.xml");
            
            // 混合斜杠
            vfs.writeFile("res\\layout/values\\strings.xml", data);
            assertTrue(vfs.exists("res/layout/values/strings.xml"),
                "混合斜杠应该被统一");
            
            // 验证：VFS中不应有反斜杠
            for (String path : vfs.getAllPaths()) {
                assertFalse(path.contains("\\"),
                    "VFS路径不应包含反斜杠: " + path);
            }
            
            System.out.println("✓ Windows路径规范化验证通过");
        }
        
        @Test
        @DisplayName("测试13: 路径分隔符统一")
        void testPathSeparatorUnification() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            
            // 所有分隔符应该被统一为/
            String[] paths = {
                "res/layout/test.xml",
                "res\\layout\\test.xml",
                "res/layout\\test.xml",
                "res\\layout/test.xml"
            };
            
            // 所有这些路径应该指向同一个文件
            for (String path : paths) {
                vfs.writeFile(path, data);
            }
            
            // 应该只有一个文件（同一路径）
            assertEquals(1, vfs.getFileCount(),
                "所有路径变体应该规范化为同一路径");
            
            assertTrue(vfs.exists("res/layout/test.xml"),
                "最终路径应该使用/作为分隔符");
            
            System.out.println("✓ 路径分隔符统一验证通过");
        }
    }
}

