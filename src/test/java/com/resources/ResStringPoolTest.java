package com.resources;

import com.resources.arsc.ResStringPool;
import com.resources.arsc.ArscParser;
import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * ResStringPool测试 - 使用真实APK数据
 * 
 * @author Resources Processor Team
 */
public class ResStringPoolTest {
    
    private static final String TEST_APK = "input/Dragonfly.apk";
    private static ResStringPool realStringPool;
    private static byte[] realArscData;
    
    @BeforeAll
    static void loadRealData() throws Exception {
        File apkFile = new File(TEST_APK);
        if (!apkFile.exists()) {
            System.out.println("警告：测试APK不存在，部分测试将跳过: " + TEST_APK);
            return;
        }
        
        // 从真实APK加载resources.arsc
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.loadFromApk(TEST_APK);
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        realArscData = provider.getResourcesArsc();
        
        // 解析ARSC获取全局字符串池
        ArscParser parser = new ArscParser();
        parser.parse(realArscData);
        
        realStringPool = parser.getGlobalStringPool();
        
        System.out.println("✓ 真实测试数据加载成功:");
        System.out.println("  - ARSC大小: " + realArscData.length + " 字节");
        if (realStringPool != null) {
            System.out.println("  - 全局字符串池: " + realStringPool.getStringCount() + " 个字符串");
            System.out.println("  - 编码: " + (realStringPool.isUtf8() ? "UTF-8" : "UTF-16"));
        }
    }
    
    @Test
    @DisplayName("测试真实ARSC字符串池解析")
    void testRealStringPoolParsing() {
        assumeTrue(realStringPool != null, "跳过测试：测试APK不存在");
        
        // 1. 验证字符串池不为空
        assertTrue(realStringPool.getStringCount() > 0, "字符串池应该包含字符串");
        
        // 2. 验证可以读取字符串
        String firstString = realStringPool.getString(0);
        assertNotNull(firstString, "应该能读取第一个字符串");
        
        System.out.println("✓ 字符串池解析测试通过");
        System.out.println("  - 总字符串数: " + realStringPool.getStringCount());
        System.out.println("  - 第一个字符串: " + firstString);
        System.out.println("  - 编码格式: " + (realStringPool.isUtf8() ? "UTF-8" : "UTF-16"));
    }
    
    @Test
    @DisplayName("测试字符串替换功能")
    void testRealStringReplacement() {
        assumeTrue(realStringPool != null, "跳过测试：测试APK不存在");
        
        // 1. 找到一个包含包名的字符串（通常在前面）
        int targetIndex = -1;
        String originalString = null;
        
        for (int i = 0; i < Math.min(100, realStringPool.getStringCount()); i++) {
            String str = realStringPool.getString(i);
            if (str != null && str.contains(".") && str.length() > 5 && str.length() < 50) {
                targetIndex = i;
                originalString = str;
                break;
            }
        }
        
        if (targetIndex == -1) {
            System.out.println("警告：未找到合适的测试字符串");
            return;
        }
        
        System.out.println("✓ 找到测试字符串 [" + targetIndex + "]: " + originalString);
        
        // 2. 创建一个新的字符串池副本用于测试
        ResStringPool testPool = new ResStringPool();
        ByteBuffer testBuffer = ByteBuffer.wrap(realArscData);
        testBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // 注意：这里需要定位到字符串池的位置
        // 由于ArscParser已经解析过，我们直接从parser获取
        ArscParser parser = new ArscParser();
        try {
            parser.parse(realArscData);
            testPool = parser.getGlobalStringPool();
        } catch (Exception e) {
            fail("解析失败: " + e.getMessage());
        }
        
        // 3. 执行替换
        String newString = "com.newapp.Test";
        String beforeReplace = testPool.getString(targetIndex);
        
        testPool.setString(targetIndex, newString);
        
        // 4. 验证替换
        String afterReplace = testPool.getString(targetIndex);
        assertEquals(newString, afterReplace, "字符串应该被成功替换");
        assertNotEquals(beforeReplace, afterReplace, "替换前后的字符串应该不同");
        
        System.out.println("✓ 字符串替换测试通过");
        System.out.println("  - 原始: " + beforeReplace);
        System.out.println("  - 新值: " + afterReplace);
    }
    
    @Test
    @DisplayName("测试字符串池验证功能")
    void testRealStringPoolValidation() {
        assumeTrue(realStringPool != null, "跳过测试：测试APK不存在");
        
        // 1. 验证原始字符串池
        assertTrue(realStringPool.validate(), "原始字符串池应该有效");
        
        System.out.println("✓ 字符串池验证测试通过");
        System.out.println("  - 字符串数: " + realStringPool.getStringCount());
        System.out.println("  - 验证状态: 通过");
    }
    
    @Test
    @DisplayName("测试批量字符串替换")
    void testBatchStringReplacement() {
        assumeTrue(realStringPool != null, "跳过测试：测试APK不存在");
        
        // 1. 创建测试用的字符串池副本
        ArscParser parser = new ArscParser();
        try {
            parser.parse(realArscData);
        } catch (Exception e) {
            fail("解析失败: " + e.getMessage());
        }
        
        ResStringPool testPool = parser.getGlobalStringPool();
        
        // 2. 准备批量替换映射
        Map<String, String> replacements = new HashMap<>();
        int replaceCount = 0;
        
        // 找到前10个可替换的字符串
        for (int i = 0; i < Math.min(100, testPool.getStringCount()); i++) {
            String str = testPool.getString(i);
            if (str != null && str.startsWith("com.") && str.length() < 100) {
                replacements.put(str, str.replace("com.", "app."));
                replaceCount++;
                if (replaceCount >= 10) break;
            }
        }
        
        if (replacements.isEmpty()) {
            System.out.println("警告：未找到可替换的字符串");
            return;
        }
        
        System.out.println("✓ 准备替换 " + replacements.size() + " 个字符串");
        
        // 3. 执行批量替换（模拟）
        int actualReplaced = 0;
        for (int i = 0; i < testPool.getStringCount(); i++) {
            String original = testPool.getString(i);
            if (replacements.containsKey(original)) {
                testPool.setString(i, replacements.get(original));
                actualReplaced++;
            }
        }
        
        // 4. 验证
        assertTrue(actualReplaced > 0, "应该至少替换一个字符串");
        assertTrue(testPool.validate(), "替换后字符串池应该仍然有效");
        
        System.out.println("✓ 批量替换测试通过");
        System.out.println("  - 计划替换: " + replacements.size());
        System.out.println("  - 实际替换: " + actualReplaced);
    }
    
    @Test
    @DisplayName("测试边界情况")
    void testBoundaryCases() {
        assumeTrue(realStringPool != null, "跳过测试：测试APK不存在");
        
        // 1. 测试索引边界
        assertThrows(IndexOutOfBoundsException.class, () -> {
            realStringPool.getString(-1);
        }, "负索引应该抛出异常");
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            realStringPool.getString(realStringPool.getStringCount() + 100);
        }, "超出范围的索引应该抛出异常");
        
        // 2. 测试有效索引
        assertDoesNotThrow(() -> {
            realStringPool.getString(0);
            realStringPool.getString(realStringPool.getStringCount() - 1);
        }, "有效索引不应该抛出异常");
        
        System.out.println("✓ 边界情况测试通过");
    }
}
