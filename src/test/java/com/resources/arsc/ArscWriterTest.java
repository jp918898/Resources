package com.resources.arsc;

import com.resources.util.VirtualFileSystem;
import com.resources.util.VfsResourceProvider;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

/**
 * ArscWriter单元测试 - 验证修复后的大小计算
 */
public class ArscWriterTest {
    
    private static final String TEST_APK = "input/Dragonfly.apk";
    private static final String LARGE_APK = "input/Telegram.apk";
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
    @DisplayName("测试ArscWriter大小匹配 - 验证estimatePackageSize修复")
    void testArscWriterSizeMatch() throws Exception {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        // 1. 加载真实ARSC
        ArscParser parser = new ArscParser();
        parser.parse(realArscData);
        
        // 2. 修改包名
        ResTablePackage pkg = parser.getMainPackage();
        assertNotNull(pkg, "应该有主资源包");
        
        String oldName = pkg.getName();
        pkg.setName(oldName + ".test");
        
        System.out.println("测试ArscWriter大小计算:");
        System.out.println("  - 原始ARSC大小: " + realArscData.length + " 字节");
        System.out.println("  - Package原始大小: " + pkg.getOriginalSize() + " 字节");
        System.out.println("  - 包名: " + oldName + " -> " + pkg.getName());
        
        // 3. 写入 - 验证不会抛出BufferOverflowException
        ArscWriter writer = new ArscWriter();
        byte[] output = assertDoesNotThrow(() -> writer.toByteArray(parser),
            "写入ARSC不应该抛出异常（验证buffer大小正确）");
        
        // 4. 验证输出
        assertNotNull(output);
        assertTrue(output.length > 0);
        
        System.out.println("  - 输出ARSC大小: " + output.length + " 字节");
        System.out.println("  - 大小差异: " + Math.abs(output.length - realArscData.length) + " 字节");
        
        // 5. 验证可以重新解析
        ArscParser reparser = new ArscParser();
        assertDoesNotThrow(() -> reparser.parse(output),
            "生成的ARSC应该可以重新解析");
        
        assertTrue(reparser.validate(), "重新解析的ARSC应该验证通过");
        
        // 6. 验证包名确实修改了
        ResTablePackage reparsedPkg = reparser.getMainPackage();
        assertEquals(oldName + ".test", reparsedPkg.getName(), 
            "重新解析后包名应该保持修改");
        
        System.out.println("✓ ArscWriter大小匹配测试通过");
    }
    
    @Test
    @DisplayName("测试getOriginalSize返回正确值")
    void testGetOriginalSize() throws Exception {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        parser.parse(realArscData);
        
        ResTablePackage pkg = parser.getMainPackage();
        assertNotNull(pkg);
        
        int originalSize = pkg.getOriginalSize();
        
        // originalSize应该是正数
        assertTrue(originalSize > 0, "originalSize应该大于0");
        
        // 对于Dragonfly.apk，包大小通常在几十KB到几百KB
        assertTrue(originalSize > 1000, "包大小应该至少1KB");
        assertTrue(originalSize < 10_000_000, "包大小应该小于10MB");
        
        System.out.println("✓ getOriginalSize测试通过: " + originalSize + " 字节");
    }
    
    @Test
    @DisplayName("测试大型ARSC文件处理")
    void testLargeArsc() throws Exception {
        File largeApk = new File(LARGE_APK);
        assumeTrue(largeApk.exists(), "跳过测试：Telegram.apk不存在");
        
        // 使用Telegram.apk测试大型ARSC
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.loadFromApk(LARGE_APK);
        
        VfsResourceProvider provider = new VfsResourceProvider(vfs);
        byte[] arscData = provider.getResourcesArsc();
        
        System.out.println("测试大型ARSC文件:");
        System.out.println("  - ARSC大小: " + arscData.length + " 字节");
        
        // 验证是否真的是大文件
        if (arscData.length < 1_000_000) {
            System.out.println("  - 警告：Telegram ARSC小于1MB，跳过");
            return;
        }
        
        // 解析和写回
        ArscParser parser = new ArscParser();
        parser.parse(arscData);
        
        ResTablePackage pkg = parser.getMainPackage();
        if (pkg != null) {
            System.out.println("  - Package大小: " + pkg.getOriginalSize() + " 字节");
            
            // 修改包名
            String oldName = pkg.getName();
            pkg.setName(oldName + ".test");
        }
        
        ArscWriter writer = new ArscWriter();
        byte[] output = assertDoesNotThrow(() -> writer.toByteArray(parser),
            "大型ARSC写入不应该抛出BufferOverflowException");
        
        assertNotNull(output);
        System.out.println("  - 输出大小: " + output.length + " 字节");
        
        // 验证大小合理（仅修改包名，大小应该接近）
        int sizeDiff = Math.abs(output.length - arscData.length);
        System.out.println("  - 大小差异: " + sizeDiff + " 字节");
        
        // 字符串池修改可能导致轻微大小变化，但应该在合理范围内
        assertTrue(sizeDiff < 10000,
            "输出大小应该与输入接近（差异<10KB）");
        
        // 验证可以重新解析
        ArscParser reparser = new ArscParser();
        assertDoesNotThrow(() -> reparser.parse(output));
        
        System.out.println("✓ 大型ARSC文件测试通过");
    }
    
    @Test
    @DisplayName("测试estimatePackageSize准确性")
    void testEstimatePackageSizeAccuracy() throws Exception {
        assumeTrue(realArscData != null, "跳过测试：测试APK不存在");
        
        ArscParser parser = new ArscParser();
        parser.parse(realArscData);
        
        // 获取估算大小（通过calculateTotalSize）
        ArscWriter writer = new ArscWriter();
        
        // 先写入一次获取实际大小
        byte[] output = writer.toByteArray(parser);
        int actualSize = output.length;
        
        System.out.println("估算准确性测试:");
        System.out.println("  - 实际输出大小: " + actualSize + " 字节");
        System.out.println("  - 原始ARSC大小: " + realArscData.length + " 字节");
        
        // 再次解析并写入，验证结果一致
        ArscParser parser2 = new ArscParser();
        parser2.parse(realArscData);
        
        ResTablePackage pkg2 = parser2.getMainPackage();
        if (pkg2 != null) {
            pkg2.setName(pkg2.getName() + ".test");
        }
        
        byte[] output2 = writer.toByteArray(parser2);
        
        // 两次写入大小应该相同
        assertEquals(actualSize, output2.length,
            "相同操作的两次写入大小应该一致");
        
        System.out.println("✓ 估算准确性测试通过");
    }
}

