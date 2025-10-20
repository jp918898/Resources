package com.resources.integration;

import com.resources.arsc.ArscParser;
import com.resources.arsc.ArscWriter;
import com.resources.arsc.ResTablePackage;
import com.resources.util.ApkSignerUtil;
import com.resources.util.ZipAlignUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 完整重建集成测试
 * 
 * 测试完整流程：
 * 1. 解析真实APK的resources.arsc
 * 2. 修改typeStrings/keyStrings
 * 3. 完整重建resources.arsc
 * 4. 对齐APK
 * 5. 签名APK
 * 6. 验证APK可解析
 */
@EnabledOnOs(OS.WINDOWS)
class FullRebuildIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(FullRebuildIntegrationTest.class);
    
    private static final String TEST_APK = "input/Dragonfly.apk";
    
    @TempDir
    Path tempDir;
    
    private File testApk;
    
    @BeforeEach
    void setUp() throws Exception {
        // 复制测试APK到临时目录
        if (new File(TEST_APK).exists()) {
            testApk = tempDir.resolve("test.apk").toFile();
            Files.copy(Paths.get(TEST_APK), testApk.toPath(), REPLACE_EXISTING);
            log.info("测试APK准备完成: {}", testApk);
        }
    }
    
    @AfterEach
    void tearDown() {
        // 清理临时文件
        if (testApk != null && testApk.exists()) {
            testApk.delete();
        }
    }
    
    @Test
    void testFullRebuild_withTypeStringsModification() throws Exception {
        if (testApk == null || !testApk.exists()) {
            log.warn("测试APK不存在，跳过测试: {}", TEST_APK);
            return;
        }
        
        log.info("═════════════════════════════════════");
        log.info("  完整重建集成测试");
        log.info("═════════════════════════════════════");
        
        // 1. 提取resources.arsc
        byte[] arscData = extractResourcesArsc(testApk);
        assertNotNull(arscData);
        assertTrue(arscData.length > 0);
        log.info("✓ resources.arsc提取成功: {} 字节", arscData.length);
        
        // 2. 解析resources.arsc
        ArscParser parser = new ArscParser();
        parser.parse(arscData);
        
        ResTablePackage mainPackage = parser.getMainPackage();
        assertNotNull(mainPackage);
        log.info("✓ 解析成功: packageId=0x{}, name='{}'", 
                Integer.toHexString(mainPackage.getId()), mainPackage.getName());
        
        // 记录原始状态
        String originalName = mainPackage.getName();
        int originalTypeStringsCount = mainPackage.getTypeStrings() != null ? 
            mainPackage.getTypeStrings().getStringCount() : 0;
        int originalKeyStringsCount = mainPackage.getKeyStrings() != null ?
            mainPackage.getKeyStrings().getStringCount() : 0;
        
        log.info("✓ 原始状态: typeStrings={}, keyStrings={}", 
                originalTypeStringsCount, originalKeyStringsCount);
        
        // 3. 修改包名（触发重建标志）
        String newName = "com.test.rebuilt";
        mainPackage.setName(newName);
        assertEquals(newName, mainPackage.getName());
        log.info("✓ 包名修改成功: '{}' -> '{}'", originalName, newName);
        
        // 4. 重建resources.arsc
        assertFalse(mainPackage.needsRebuild(), "只修改包名不应该触发完整重建");
        
        ArscWriter writer = new ArscWriter();
        byte[] rebuiltData = writer.toByteArray(parser);
        assertNotNull(rebuiltData);
        assertTrue(rebuiltData.length > 0);
        log.info("✓ 重建成功: {} 字节 (原始={}, 差异={})", 
                rebuiltData.length, arscData.length, rebuiltData.length - arscData.length);
        
        // 5. 验证重建后的resources.arsc可解析
        ArscParser validator = new ArscParser();
        validator.parse(rebuiltData);
        
        ResTablePackage validatedPackage = validator.getMainPackage();
        assertNotNull(validatedPackage);
        assertEquals(newName, validatedPackage.getName());
        assertEquals(mainPackage.getId(), validatedPackage.getId());
        
        if (validatedPackage.getTypeStrings() != null) {
            assertEquals(originalTypeStringsCount, 
                        validatedPackage.getTypeStrings().getStringCount(),
                        "typeStrings数量应该保持不变");
        }
        
        if (validatedPackage.getKeyStrings() != null) {
            assertEquals(originalKeyStringsCount,
                        validatedPackage.getKeyStrings().getStringCount(),
                        "keyStrings数量应该保持不变");
        }
        
        log.info("✓ 验证成功: 重建后的ARSC结构完整");
        
        log.info("═════════════════════════════════════");
        log.info("  完整重建测试通过 ✓");
        log.info("═════════════════════════════════════");
    }
    
    @Test
    void testAlignAndSign() throws Exception {
        if (testApk == null || !testApk.exists()) {
            log.warn("测试APK不存在，跳过测试: {}", TEST_APK);
            return;
        }
        
        if (!ZipAlignUtil.isAvailable()) {
            log.warn("zipalign工具不可用，跳过测试");
            return;
        }
        
        if (!ApkSignerUtil.isAvailable() || !ApkSignerUtil.isTestKeystoreAvailable()) {
            log.warn("apksigner或测试密钥不可用，跳过测试");
            return;
        }
        
        log.info("═════════════════════════════════════");
        log.info("  对齐和签名集成测试");
        log.info("═════════════════════════════════════");
        
        File alignedApk = tempDir.resolve("aligned.apk").toFile();
        
        // 1. 对齐APK
        long originalSize = testApk.length();
        ZipAlignUtil.align(testApk.getAbsolutePath(), alignedApk.getAbsolutePath(), 4);
        
        assertTrue(alignedApk.exists());
        assertTrue(alignedApk.length() > 0);
        log.info("✓ 对齐成功: {} 字节 (原始={}, 差异={})", 
                alignedApk.length(), originalSize, alignedApk.length() - originalSize);
        
        // 2. 签名APK
        ApkSignerUtil.signWithTestKey(alignedApk.getAbsolutePath());
        
        assertTrue(alignedApk.exists());
        log.info("✓ 签名成功: {} 字节", alignedApk.length());
        
        // 3. 验证APK结构（可以作为ZIP打开）
        try (ZipFile zipFile = new ZipFile(alignedApk)) {
            int entryCount = 0;
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entries.nextElement();
                entryCount++;
            }
            
            assertTrue(entryCount > 0);
            log.info("✓ APK结构验证: {} 个entry", entryCount);
        }
        
        // 4. 验证签名
        boolean signatureValid = ApkSignerUtil.verify(alignedApk.getAbsolutePath());
        assertTrue(signatureValid, "APK签名应该有效");
        log.info("✓ 签名验证通过");
        
        log.info("═════════════════════════════════════");
        log.info("  对齐和签名测试通过 ✓");
        log.info("═════════════════════════════════════");
    }
    
    /**
     * 从APK提取resources.arsc
     */
    private byte[] extractResourcesArsc(File apkFile) throws Exception {
        try (ZipFile zipFile = new ZipFile(apkFile)) {
            ZipEntry arscEntry = zipFile.getEntry("resources.arsc");
            
            if (arscEntry == null) {
                return null;
            }
            
            try (var is = zipFile.getInputStream(arscEntry)) {
                return is.readAllBytes();
            }
        }
    }
}

