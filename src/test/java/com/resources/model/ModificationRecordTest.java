package com.resources.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ModificationRecord单元测试 - 100%覆盖
 */
public class ModificationRecordTest {
    
    @Test
    @DisplayName("测试创建修改记录")
    void testCreateRecord() {
        ModificationRecord record = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("OldClass")
            .newValue("NewClass")
            .build();
        
        assertEquals(ModificationRecord.ModificationType.TAG_NAME_REPLACE, record.getType());
        assertEquals("test.xml", record.getFilePath());
        assertEquals("line 10", record.getLocation());
        assertEquals("OldClass", record.getOriginalValue());
        assertEquals("NewClass", record.getNewValue());
        assertTrue(record.getTimestamp() > 0);
        assertFalse(record.isApplied()); // 默认false
        assertNull(record.getErrorMessage());
        assertFalse(record.hasError());
    }
    
    @Test
    @DisplayName("测试所有修改类型")
    void testAllModificationTypes() {
        ModificationRecord.ModificationType[] types = {
            ModificationRecord.ModificationType.TAG_NAME_REPLACE,
            ModificationRecord.ModificationType.ATTRIBUTE_VALUE_REPLACE,
            ModificationRecord.ModificationType.ARSC_STRING_REPLACE,
            ModificationRecord.ModificationType.ARSC_PACKAGE_NAME,
            ModificationRecord.ModificationType.DATABINDING_TYPE,
            ModificationRecord.ModificationType.DATABINDING_IMPORT,
            ModificationRecord.ModificationType.DATABINDING_EXPRESSION
        };
        
        for (ModificationRecord.ModificationType type : types) {
            ModificationRecord record = new ModificationRecord.Builder()
                .type(type)
                .filePath("test.xml")
                .location("location")
                .originalValue("old")
                .newValue("new")
                .build();
            
            assertEquals(type, record.getType());
        }
    }
    
    @Test
    @DisplayName("测试应用状态")
    void testAppliedStatus() {
        ModificationRecord applied = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .applied(true)
            .build();
        
        assertTrue(applied.isApplied());
        
        ModificationRecord notApplied = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .applied(false)
            .build();
        
        assertFalse(notApplied.isApplied());
    }
    
    @Test
    @DisplayName("测试错误信息")
    void testErrorMessage() {
        ModificationRecord withError = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .errorMessage("Test error")
            .build();
        
        assertEquals("Test error", withError.getErrorMessage());
        assertTrue(withError.hasError());
        
        ModificationRecord noError = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .build();
        
        assertNull(noError.getErrorMessage());
        assertFalse(noError.hasError());
    }
    
    @Test
    @DisplayName("测试equals和hashCode")
    void testEqualsAndHashCode() {
        ModificationRecord record1 = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .build();
        
        ModificationRecord record2 = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .build();
        
        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());
    }
    
    @Test
    @DisplayName("测试toString")
    void testToString() {
        ModificationRecord record = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .applied(true)
            .build();
        
        String str = record.toString();
        
        assertNotNull(str);
        // toString不包含filePath，只包含type, location, originalValue, newValue
        assertTrue(str.contains("TAG_NAME_REPLACE") || str.contains("line 10"));
        assertTrue(str.contains("Old"));
        assertTrue(str.contains("New"));
    }
    
    @Test
    @DisplayName("测试自定义timestamp")
    void testCustomTimestamp() {
        long customTime = 1234567890L;
        
        ModificationRecord record = new ModificationRecord.Builder()
            .type(ModificationRecord.ModificationType.TAG_NAME_REPLACE)
            .filePath("test.xml")
            .location("line 10")
            .originalValue("Old")
            .newValue("New")
            .timestamp(customTime)
            .build();
        
        assertEquals(customTime, record.getTimestamp());
    }
}
