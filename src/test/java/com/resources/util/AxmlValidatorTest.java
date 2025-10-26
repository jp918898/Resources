package com.resources.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AXML验证器测试
 */
@DisplayName("AXML格式验证器测试")
public class AxmlValidatorTest {

    @Test
    @DisplayName("验证真实AXML文件")
    void testValidAxml() throws IOException {
        // Read real AXML file from APK
        Path apkPath = Path.of("input/Dragonfly.apk");
        if (!Files.exists(apkPath)) {
            System.out.println("Skip test: Dragonfly.apk not found");
            return;
        }

        try (ZipFile zipFile = new ZipFile(apkPath.toFile())) {
            ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
            assertNotNull(entry, "AndroidManifest.xml should exist in APK");
            
            byte[] data = zipFile.getInputStream(entry).readAllBytes();
            
            // Test validator
            boolean isValid = AxmlValidator.isValidAxml(data);
            assertTrue(isValid, "Real AXML file should pass validation");
        }
    }

    @Test
    @DisplayName("验证最小有效AXML")
    void testMinimalValidAxml() {
        // Minimal valid AXML: magic(2) + headerSize(2) + fileSize(4) = 8 bytes
        byte[] data = new byte[]{
            0x03, 0x00,  // type = 0x0003 (RES_XML_TYPE)
            0x08, 0x00,  // headerSize = 8
            0x08, 0x00, 0x00, 0x00  // fileSize = 8 (LITTLE_ENDIAN)
        };
        
        assertTrue(AxmlValidator.isValidAxml(data), "Minimal valid AXML should pass");
    }

    @Test
    @DisplayName("拒绝空数据")
    void testEmptyData() {
        assertFalse(AxmlValidator.isValidAxml(null), "Null data should be rejected");
        assertFalse(AxmlValidator.isValidAxml(new byte[0]), "Empty data should be rejected");
        assertFalse(AxmlValidator.isValidAxml(new byte[7]), "Data < 8 bytes should be rejected");
    }

    @Test
    @DisplayName("拒绝错误的magic number")
    void testWrongMagicNumber() {
        byte[] data = new byte[]{
            0x02, 0x00,  // type = 0x0002 (wrong, should be 0x0003)
            0x08, 0x00,
            0x08, 0x00, 0x00, 0x00
        };
        
        assertFalse(AxmlValidator.isValidAxml(data), "Wrong magic number should be rejected");
    }

    @Test
    @DisplayName("拒绝错误的header size")
    void testWrongHeaderSize() {
        byte[] data = new byte[]{
            0x03, 0x00,
            0x10, 0x00,  // headerSize = 16 (wrong, should be 8)
            0x10, 0x00, 0x00, 0x00
        };
        
        assertFalse(AxmlValidator.isValidAxml(data), "Wrong header size should be rejected");
    }

    @Test
    @DisplayName("拒绝文本XML")
    void testTextXml() {
        byte[] data = "<?xml version=\"1.0\"?>".getBytes();
        assertFalse(AxmlValidator.isValidAxml(data), "Text XML should be rejected");
    }

    @Test
    @DisplayName("拒绝PNG文件")
    void testPngFile() {
        byte[] data = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47,  // PNG signature
            0x0D, 0x0A, 0x1A, 0x0A
        };
        
        assertFalse(AxmlValidator.isValidAxml(data), "PNG file should be rejected");
    }

    @Test
    @DisplayName("拒绝损坏的AXML")
    void testCorruptedAxml() {
        byte[] data = new byte[]{
            0x03, 0x00,
            0x08, 0x00,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F  // fileSize = 2147483647 (too large)
        };
        
        assertFalse(AxmlValidator.isValidAxml(data), "Corrupted AXML should be rejected");
    }
}

