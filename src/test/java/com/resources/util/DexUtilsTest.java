package com.resources.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DexUtils单元测试
 */
class DexUtilsTest {
    
    @Test
    @DisplayName("descriptorToClassName - 标准类")
    void testDescriptorToClassName_StandardClass() {
        String descriptor = "Lcom/example/MainActivity;";
        String expected = "com.example.MainActivity";
        
        String actual = DexUtils.descriptorToClassName(descriptor);
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("descriptorToClassName - 内部类")
    void testDescriptorToClassName_InnerClass() {
        String descriptor = "Lcom/example/Outer$Inner;";
        String expected = "com.example.Outer$Inner";
        
        String actual = DexUtils.descriptorToClassName(descriptor);
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("descriptorToClassName - 数组类型")
    void testDescriptorToClassName_ArrayType() {
        String descriptor = "[Ljava/lang/String;";
        
        // 数组类型保持不变
        String actual = DexUtils.descriptorToClassName(descriptor);
        
        assertEquals(descriptor, actual);
    }
    
    @Test
    @DisplayName("descriptorToClassName - 基本类型")
    void testDescriptorToClassName_PrimitiveType() {
        assertEquals("I", DexUtils.descriptorToClassName("I"));
        assertEquals("V", DexUtils.descriptorToClassName("V"));
        assertEquals("Z", DexUtils.descriptorToClassName("Z"));
    }
    
    @Test
    @DisplayName("descriptorToClassName - null和空字符串")
    void testDescriptorToClassName_NullAndEmpty() {
        assertNull(DexUtils.descriptorToClassName(null));
        assertNull(DexUtils.descriptorToClassName(""));
    }
    
    @Test
    @DisplayName("descriptorToClassName - 无效描述符")
    void testDescriptorToClassName_Invalid() {
        // 无效格式返回null
        assertNull(DexUtils.descriptorToClassName("InvalidDescriptor"));
        assertNull(DexUtils.descriptorToClassName("Lcom/example/NoSemicolon"));
    }
    
    @Test
    @DisplayName("classNameToDescriptor - 标准类")
    void testClassNameToDescriptor_StandardClass() {
        String className = "com.example.MainActivity";
        String expected = "Lcom/example/MainActivity;";
        
        String actual = DexUtils.classNameToDescriptor(className);
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("classNameToDescriptor - 内部类")
    void testClassNameToDescriptor_InnerClass() {
        String className = "com.example.Outer$Inner";
        String expected = "Lcom/example/Outer$Inner;";
        
        String actual = DexUtils.classNameToDescriptor(className);
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("classNameToDescriptor - null和空字符串")
    void testClassNameToDescriptor_NullAndEmpty() {
        assertNull(DexUtils.classNameToDescriptor(null));
        assertNull(DexUtils.classNameToDescriptor(""));
    }
    
    @Test
    @DisplayName("isDexFileAccessible - 不存在的文件")
    void testIsDexFileAccessible_NonExistent() {
        assertFalse(DexUtils.isDexFileAccessible("nonexistent.dex"));
    }
    
    @Test
    @DisplayName("isDexFileAccessible - null和空字符串")
    void testIsDexFileAccessible_NullAndEmpty() {
        assertFalse(DexUtils.isDexFileAccessible(null));
        assertFalse(DexUtils.isDexFileAccessible(""));
    }
    
    @Test
    @DisplayName("loadDexClasses - null参数")
    void testLoadDexClasses_NullParameter() {
        assertThrows(NullPointerException.class, () -> {
            DexUtils.loadDexClasses(null);
        });
    }
    
    @Test
    @DisplayName("loadDexClasses - 空字符串")
    void testLoadDexClasses_EmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            DexUtils.loadDexClasses("");
        });
    }
    
    @Test
    @DisplayName("loadDexClasses - 不存在的文件")
    void testLoadDexClasses_NonExistentFile() {
        assertThrows(FileNotFoundException.class, () -> {
            DexUtils.loadDexClasses("nonexistent.dex");
        });
    }
    
    @Test
    @DisplayName("loadDexClasses - 真实DEX文件（如果存在）")
    @org.junit.jupiter.api.condition.EnabledIf("isDexFileAvailable")
    void testLoadDexClasses_RealDexFile() throws IOException {
        String dexPath = "input/dex/classes.dex";
        
        Set<String> classes = DexUtils.loadDexClasses(dexPath);
        
        assertNotNull(classes);
        assertTrue(classes.size() > 0, "DEX文件应该包含类");
        
        System.out.println("加载DEX成功: " + classes.size() + " 个类");
        
        // 验证类名格式（修正正则表达式，移除短横线）
        for (String className : classes) {
            assertNotNull(className);
            assertFalse(className.isEmpty());
            // Java类名格式验证（支持package-info等特殊类）
            // 合法格式：字母/下划线/美元符开头，后跟字母/数字/下划线/美元符/点号
            // 注意：Java类名不允许短横线，只允许在包名中使用
            assertTrue(className.matches("[a-zA-Z_$][a-zA-Z0-9_$.]*"),
                      "无效的类名格式: " + className);
        }
    }
    
    /**
     * 检查DEX文件是否可用的条件方法
     */
    static boolean isDexFileAvailable() {
        String dexPath = "input/dex/classes.dex";
        boolean available = DexUtils.isDexFileAccessible(dexPath);
        if (!available) {
            System.out.println("跳过测试: DEX文件不存在 " + dexPath);
        }
        return available;
    }
    
    @Test
    @DisplayName("往返转换测试")
    void testRoundTrip() {
        String[] testCases = {
            "com.example.MainActivity",
            "com.example.Outer$Inner",
            "com.example.package.ClassName",
            "a.b.C"
        };
        
        for (String className : testCases) {
            String descriptor = DexUtils.classNameToDescriptor(className);
            String back = DexUtils.descriptorToClassName(descriptor);
            
            assertEquals(className, back, 
                        "往返转换失败: " + className + " -> " + descriptor + " -> " + back);
        }
    }
}

