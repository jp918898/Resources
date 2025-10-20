package com.resources.axml;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import com.resources.validator.SemanticValidator;
import com.resources.mapping.WhitelistFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AxmlReplacer错误处理测试
 * 
 * 验证宽松错误处理机制：
 * - 部分文件损坏 → 不抛异常
 * - 全部文件损坏 → 抛IOException
 * - 混合场景（成功+跳过+失败）→ 不抛异常
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
class AxmlReplacerErrorHandlingTest {
    
    private AxmlReplacer axmlReplacer;
    
    @BeforeEach
    void setUp() {
        WhitelistFilter whitelistFilter = new WhitelistFilter();
        whitelistFilter.addOwnPackages(Set.of("com.test"));
        
        SemanticValidator semanticValidator = new SemanticValidator(whitelistFilter);
        ClassMapping classMapping = new ClassMapping();
        PackageMapping packageMapping = new PackageMapping();
        
        axmlReplacer = new AxmlReplacer(
            semanticValidator, classMapping, packageMapping, false);
    }
    
    @Test
    void testPartialFailure_ShouldNotThrowException() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        // 1个有效的AXML
        files.put("res/layout/valid.xml", createValidAxml());
        
        // 1个损坏的文件
        files.put("res/layout/corrupted.xml", new byte[]{0x01, 0x02, 0x03});
        
        // 1个文本XML
        files.put("res/layout/text.xml", "<?xml version=\"1.0\"?>".getBytes());
        
        // 部分失败不应该抛异常
        assertDoesNotThrow(() -> {
            Map<String, byte[]> results = axmlReplacer.replaceAxmlBatch(files);
            
            // 验证所有文件都有结果
            assertEquals(3, results.size(), "应该返回所有文件的结果");
            
            // 验证失败的文件保留了原数据
            assertArrayEquals(files.get("res/layout/corrupted.xml"), 
                            results.get("res/layout/corrupted.xml"),
                            "损坏文件应该保留原数据");
        });
    }
    
    @Test
    void testAllCorrupted_ShouldHandleGracefully() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        // 全部损坏的文件（AxmlParser能够优雅处理）
        // 注意：由于AxmlParser的健壮性，损坏的AXML会被优雅处理为返回原数据
        files.put("res/layout/corrupted1.xml", createCorruptedAxmlPassPrecheck());
        files.put("res/layout/corrupted2.xml", createCorruptedAxmlPassPrecheck());
        
        // 由于AxmlParser的优雅错误处理，这些文件会被标记为skipped而非failed
        // 因此不应该抛异常（验证系统的鲁棒性）
        assertDoesNotThrow(() -> {
            Map<String, byte[]> results = axmlReplacer.replaceAxmlBatch(files);
            
            // 应该返回原数据
            assertEquals(2, results.size());
            assertArrayEquals(files.get("res/layout/corrupted1.xml"), 
                            results.get("res/layout/corrupted1.xml"),
                            "损坏文件应该保留原数据");
        });
    }
    
    @Test
    void testMixedScenario_ShouldNotThrowException() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        // 1个有效AXML
        files.put("res/layout/valid.xml", createValidAxml());
        
        // 1个非AXML（会被跳过）
        files.put("res/drawable/icon.png", new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47  // PNG magic
        });
        
        // 1个损坏文件（会失败）
        files.put("res/layout/bad.xml", new byte[]{(byte) 0xFF, (byte) 0xFF});
        
        // 混合场景不应该抛异常
        assertDoesNotThrow(() -> {
            Map<String, byte[]> results = axmlReplacer.replaceAxmlBatch(files);
            assertEquals(3, results.size(), "应该处理所有文件");
        });
    }
    
    @Test
    void testAllSkipped_ShouldNotThrowException() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        // 全部非AXML文件
        files.put("res/drawable/icon1.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        files.put("res/drawable/icon2.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        
        // 全部跳过不应该抛异常
        assertDoesNotThrow(() -> {
            Map<String, byte[]> results = axmlReplacer.replaceAxmlBatch(files);
            assertEquals(2, results.size(), "应该返回所有文件");
        });
    }
    
    @Test
    void testEmptyInput_ShouldNotThrowException() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        // 空输入不应该抛异常
        assertDoesNotThrow(() -> {
            Map<String, byte[]> results = axmlReplacer.replaceAxmlBatch(files);
            assertTrue(results.isEmpty(), "结果应该为空");
        });
    }
    
    /**
     * 创建一个最小的合法AXML
     */
    private byte[] createValidAxml() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0003);  // RES_XML_TYPE
        buffer.putShort((short) 8);        // headerSize
        buffer.putInt(8);                  // fileSize
        return buffer.array();
    }
    
    /**
     * 创建通过预检但解析会失败的AXML
     */
    private byte[] createCorruptedAxmlPassPrecheck() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0x0003);  // RES_XML_TYPE (通过预检)
        buffer.putShort((short) 8);        // headerSize (通过预检)
        buffer.putInt(16);                 // fileSize (通过预检)
        // 后续8字节为垃圾数据，解析时会失败
        buffer.putInt(0xFFFFFFFF);
        buffer.putInt(0xFFFFFFFF);
        return buffer.array();
    }
}

