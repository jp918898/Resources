package com.resources.util;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AxmlValidator测试
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
class AxmlValidatorTest {
    
    @Test
    void testValidAxml() throws Exception {
        // 从真实APK提取的合法AXML
        byte[] validAxml = loadTestResource("/test_layout.xml");
        if (validAxml != null && validAxml.length > 0) {
            assertTrue(AxmlValidator.isValidAxml(validAxml), 
                      "真实AXML文件应该通过验证");
        }
    }
    
    @Test
    void testTextXml() {
        // 文本XML（非二进制）
        String textXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "</LinearLayout>";
        byte[] textXmlBytes = textXml.getBytes();
        
        assertFalse(AxmlValidator.isValidAxml(textXmlBytes), 
                   "文本XML应该被识别为非AXML");
    }
    
    @Test
    void testCorruptedAxml() {
        // 损坏的AXML（magic number正确，但文件大小错误）
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0003);  // RES_XML_TYPE
        buffer.putShort((short) 8);        // headerSize
        buffer.putInt(999999);             // 错误的fileSize
        
        byte[] corrupted = buffer.array();
        assertFalse(AxmlValidator.isValidAxml(corrupted), 
                   "损坏的AXML应该被识别为非法");
    }
    
    @Test
    void testWrongMagicNumber() {
        // Magic number错误
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0001);  // 错误的类型
        buffer.putShort((short) 8);
        buffer.putInt(12);
        
        byte[] wrongMagic = buffer.array();
        assertFalse(AxmlValidator.isValidAxml(wrongMagic), 
                   "错误的magic number应该被识别为非AXML");
    }
    
    @Test
    void testWrongHeaderSize() {
        // Header size错误
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0003);  // RES_XML_TYPE
        buffer.putShort((short) 12);      // 错误的headerSize（应该是8）
        buffer.putInt(12);
        
        byte[] wrongHeader = buffer.array();
        assertFalse(AxmlValidator.isValidAxml(wrongHeader), 
                   "错误的header size应该被识别为非法");
    }
    
    @Test
    void testEmptyData() {
        assertFalse(AxmlValidator.isValidAxml(null), 
                   "null数据应该返回false");
        
        assertFalse(AxmlValidator.isValidAxml(new byte[0]), 
                   "空数组应该返回false");
        
        assertFalse(AxmlValidator.isValidAxml(new byte[4]), 
                   "小于8字节的数据应该返回false");
    }
    
    @Test
    void testMinimalValidAxml() {
        // 最小合法AXML（仅头部）
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0003);  // RES_XML_TYPE
        buffer.putShort((short) 8);        // headerSize
        buffer.putInt(8);                  // fileSize
        
        byte[] minimal = buffer.array();
        assertTrue(AxmlValidator.isValidAxml(minimal), 
                  "最小合法AXML应该通过验证");
    }
    
    @Test
    void testPngFile() {
        // PNG文件的magic number: 89 50 4E 47
        byte[] png = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 
            0x0D, 0x0A, 0x1A, 0x0A
        };
        
        assertFalse(AxmlValidator.isValidAxml(png), 
                   "PNG文件应该被识别为非AXML");
    }
    
    /**
     * 加载测试资源
     */
    private byte[] loadTestResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("测试资源未找到: " + resourcePath + " (跳过测试)");
                return null;
            }
            return is.readAllBytes();
        } catch (Exception e) {
            System.err.println("加载测试资源失败: " + e.getMessage());
            return null;
        }
    }
}


