package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AxmlWriter属性值处理测试
 * 验证工业级标准：禁止静默丢失数据
 */
@DisplayName("AxmlWriter属性值处理测试")
public class AxmlWriterAttributeValueTest {

    @TempDir
    Path tempDir;

    private AxmlWriter.NodeImpl rootNode;

    @BeforeEach
    void setUp() {
        rootNode = new AxmlWriter.NodeImpl(null, "root");
    }

    @Test
    @DisplayName("测试String转Integer失败场景")
    void testStringToIntegerConversionFailure() {
        // 构造包含非法整数字符串的属性
        rootNode.attr("", "testAttr", 0x01010001, 0x10, "invalid_number");
        
        // 验证写入时抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
        
        assertTrue(exception.getMessage().contains("无法将属性值转换为整数"));
        assertTrue(exception.getMessage().contains("invalid_number"));
        assertTrue(exception.getMessage().contains("attr='testAttr'"));
    }

    @Test
    @DisplayName("测试未知类型场景")
    void testUnsupportedValueType() {
        // 构造包含Long类型的属性（不支持的类型）
        rootNode.attr("", "testAttr", 0x01010001, 0x10, 12345L);
        
        // 验证写入时抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
        
        assertTrue(exception.getMessage().contains("不支持的属性值类型"));
        assertTrue(exception.getMessage().contains("Long"));
        assertTrue(exception.getMessage().contains("attr='testAttr'"));
    }

    @Test
    @DisplayName("测试Double类型场景")
    void testDoubleValueType() {
        // 构造包含Double类型的属性
        rootNode.attr("", "testAttr", 0x01010001, 0x10, 3.14);
        
        // 验证写入时抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
        
        assertTrue(exception.getMessage().contains("不支持的属性值类型"));
        assertTrue(exception.getMessage().contains("Double"));
    }

    @Test
    @DisplayName("测试null值处理")
    void testNullValueHandling() {
        // null值应该正常处理（写入0）
        rootNode.attr("", "testAttr", 0x01010001, 0x00, null);
        
        // 验证不抛异常
        assertDoesNotThrow(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
    }

    @Test
    @DisplayName("测试有效Integer值")
    void testValidIntegerValue() {
        // 有效Integer应该正常处理
        rootNode.attr("", "testAttr", 0x01010001, 0x10, 42);
        
        // 验证不抛异常
        assertDoesNotThrow(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
    }

    @Test
    @DisplayName("测试有效String值")
    void testValidStringValue() {
        // 有效String应该正常处理
        rootNode.attr("", "testAttr", 0x01010001, 0x10, "123");
        
        // 验证不抛异常
        assertDoesNotThrow(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
    }

    @Test
    @DisplayName("测试Boolean字符串值")
    void testBooleanStringValues() {
        // "true"应该转换为-1
        rootNode.attr("", "testAttr1", 0x01010001, 0x12, "true");
        
        // "false"应该转换为0
        rootNode.attr("", "testAttr2", 0x01010002, 0x12, "false");
        
        // 验证不抛异常
        assertDoesNotThrow(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
    }

    @Test
    @DisplayName("测试空字符串转换")
    void testEmptyStringConversion() {
        // 空字符串无法转换为整数
        rootNode.attr("", "testAttr", 0x01010001, 0x10, "");
        
        // 验证抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
        
        assertTrue(exception.getMessage().contains("无法将属性值转换为整数"));
    }

    @Test
    @DisplayName("测试边界值转换")
    void testBoundaryValueConversion() {
        // 测试Integer.MAX_VALUE
        rootNode.attr("", "maxInt", 0x01010001, 0x10, String.valueOf(Integer.MAX_VALUE));
        
        // 测试Integer.MIN_VALUE
        rootNode.attr("", "minInt", 0x01010002, 0x10, String.valueOf(Integer.MIN_VALUE));
        
        // 验证不抛异常
        assertDoesNotThrow(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
    }

    @Test
    @DisplayName("测试超出Integer范围的字符串")
    void testOutOfRangeStringConversion() {
        // 超出Integer范围的字符串
        rootNode.attr("", "testAttr", 0x01010001, 0x10, "999999999999999999999");
        
        // 验证抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            rootNode.write(buffer);
        });
        
        assertTrue(exception.getMessage().contains("无法将属性值转换为整数"));
    }
}
