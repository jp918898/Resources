package com.resources.arsc;

import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

/**
 * ArscParser单元测试 - 使用真实ARSC数据
 */
public class ArscParserTest {
    
    private static final String TEST_APK = "input/Dragonfly.apk";
    private byte[] realArscData;
    
    @BeforeEach
    void setUp() throws Exception {
        // 从真实APK加载resources.arsc
        File apkFile = new File(TEST_APK);
        if (apkFile.exists()) {
            VirtualFileSystem vfs = new VirtualFileSystem();
            vfs.loadFromApk(TEST_APK);
            
            VfsResourceProvider provider = new VfsResourceProvider(vfs);
            realArscData = provider.getResourcesArsc();
        }
    }
    
    @Test
    @DisplayName("测试解析真实ARSC文件")
    void testParseRealArsc() {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        assertDoesNotThrow(() -> parser.parse(realArscData));
        
        // 验证解析结果
        assertNotNull(parser.getGlobalStringPool(), "应该有全局字符串池");
        assertTrue(parser.getPackageCount() > 0, "应该至少有一个包");
        assertTrue(parser.getPackages().size() > 0, "packages列表不应该为空");
        
        System.out.println("ARSC解析成功:");
        System.out.println("  - 包数量: " + parser.getPackageCount());
        System.out.println("  - 全局字符串: " + parser.getGlobalStringPool().getStringCount());
    }
    
    @Test
    @DisplayName("测试获取主资源包")
    void testGetMainPackage() {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        assertDoesNotThrow(() -> parser.parse(realArscData));
        
        ResTablePackage mainPackage = parser.getMainPackage();
        assertNotNull(mainPackage, "应该有主资源包");
        
        // 主包的ID应该是0x7f
        assertTrue(mainPackage.getId() == 0x7f || mainPackage.getId() == 0x01, 
                  "主包ID应该是0x7f或0x01");
        
        assertNotNull(mainPackage.getName(), "包名不应该为null");
        assertFalse(mainPackage.getName().isEmpty(), "包名不应该为空");
        
        System.out.println("主资源包:");
        System.out.println("  - ID: 0x" + Integer.toHexString(mainPackage.getId()));
        System.out.println("  - 名称: " + mainPackage.getName());
    }
    
    @Test
    @DisplayName("测试查找字符串")
    void testFindStrings() {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        assertDoesNotThrow(() -> parser.parse(realArscData));
        
        // 查找包含"app"的字符串
        java.util.List<Integer> indices = parser.findStringIndices("app");
        
        System.out.println("找到 " + indices.size() + " 个包含'app'的字符串");
        
        // 查找以特定前缀开头的字符串
        java.util.Map<Integer, String> prefixMatches = parser.findStringsByPrefix("com.");
        
        System.out.println("找到 " + prefixMatches.size() + " 个以'com.'开头的字符串");
        
        if (!prefixMatches.isEmpty()) {
            System.out.println("示例:");
            int count = 0;
            for (java.util.Map.Entry<Integer, String> entry : prefixMatches.entrySet()) {
                System.out.println("  [" + entry.getKey() + "] " + entry.getValue());
                if (++count >= 5) break; // 只显示前5个
            }
        }
    }
    
    @Test
    @DisplayName("测试ARSC验证")
    void testValidate() {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        assertDoesNotThrow(() -> parser.parse(realArscData));
        
        assertTrue(parser.validate(), "ARSC应该验证通过");
    }
    
    @Test
    @DisplayName("测试无效ARSC数据")
    void testInvalidArscData() {
        ArscParser parser = new ArscParser();
        
        // 无效的数据应该抛出异常
        byte[] invalidData = "INVALID".getBytes();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(invalidData));
    }
    
    @Test
    @DisplayName("测试null参数")
    void testNullParameters() {
        ArscParser parser = new ArscParser();
        
        // 测试null参数
        assertThrows(NullPointerException.class, () -> parser.parse(null));
        
        // 测试空数据 - 应该抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[0]));
        
        // 测试其他null参数
        assertThrows(NullPointerException.class, () -> parser.findStringIndices(null));
        assertThrows(NullPointerException.class, () -> parser.findStringsByPrefix(null));
        assertThrows(NullPointerException.class, () -> parser.getPackageByName(null));
    }
}

