package com.resources.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * ValidationResult单元测试 - 100%覆盖
 */
public class ValidationResultTest {
    
    @Test
    @DisplayName("测试成功的验证结果")
    void testSuccessValidation() {
        List<ValidationResult.ValidationItem> items = new ArrayList<>();
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.AAPT2_STATIC,
            ValidationResult.ValidationStatus.PASSED,
            "AAPT2 validation passed",
            "All checks passed"
        ));
        
        ValidationResult result = new ValidationResult(items);
        
        assertTrue(result.isOverallSuccess());
        assertEquals(1, result.getItems().size());
        assertEquals(0, result.getFailedItems().size());
    }
    
    @Test
    @DisplayName("测试失败的验证结果")
    void testFailureValidation() {
        List<ValidationResult.ValidationItem> items = new ArrayList<>();
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.DEX_CROSS,
            ValidationResult.ValidationStatus.FAILED,
            "DEX cross validation failed",
            "Missing class in DEX"
        ));
        
        ValidationResult result = new ValidationResult(items);
        
        assertFalse(result.isOverallSuccess());
        assertEquals(1, result.getFailedItems().size());
    }
    
    @Test
    @DisplayName("测试带警告的验证")
    void testValidationWithWarnings() {
        List<ValidationResult.ValidationItem> items = new ArrayList<>();
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.INTEGRITY,
            ValidationResult.ValidationStatus.PASSED,
            "Integrity check passed",
            null
        ));
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.RUNTIME,
            ValidationResult.ValidationStatus.WARNING,
            "Runtime validation warning",
            "Some warnings found"
        ));
        
        ValidationResult result = new ValidationResult(items);
        
        // 只有警告不影响整体成功（没有FAILED）
        assertTrue(result.isOverallSuccess());
        assertEquals(1, result.getWarningItems().size());
    }
    
    @Test
    @DisplayName("测试混合结果")
    void testMixedResults() {
        List<ValidationResult.ValidationItem> items = new ArrayList<>();
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.AAPT2_STATIC,
            ValidationResult.ValidationStatus.PASSED,
            "AAPT2 passed",
            null
        ));
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.DEX_CROSS,
            ValidationResult.ValidationStatus.FAILED,
            "DEX failed",
            "Error details"
        ));
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.INTEGRITY,
            ValidationResult.ValidationStatus.WARNING,
            "Integrity warning",
            null
        ));
        
        ValidationResult result = new ValidationResult(items);
        
        assertFalse(result.isOverallSuccess()); // 因为有Failed
        assertEquals(3, result.getItems().size());
        assertEquals(1, result.getFailedItems().size());
        assertEquals(1, result.getWarningItems().size());
    }
    
    @Test
    @DisplayName("测试ValidationItem的toString")
    void testValidationItemToString() {
        ValidationResult.ValidationItem passed = new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.AAPT2_STATIC,
            ValidationResult.ValidationStatus.PASSED,
            "Test passed",
            "Details"
        );
        
        String str = passed.toString();
        assertNotNull(str);
        assertTrue(str.contains("AAPT2_STATIC"));
        assertTrue(str.contains("Test passed"));
    }
    
    @Test
    @DisplayName("测试空结果列表")
    void testEmptyItems() {
        ValidationResult result = new ValidationResult(new ArrayList<>());
        
        assertTrue(result.isOverallSuccess()); // 没有失败项
        assertEquals(0, result.getItems().size());
        assertEquals(0, result.getFailedItems().size());
        assertEquals(0, result.getWarningItems().size());
    }
    
    @Test
    @DisplayName("测试getSummary")
    void testGetSummary() {
        List<ValidationResult.ValidationItem> items = new ArrayList<>();
        items.add(new ValidationResult.ValidationItem(
            ValidationResult.ValidationLevel.DEX_CROSS,
            ValidationResult.ValidationStatus.FAILED,
            "Failed test",
            "Error"
        ));
        
        ValidationResult result = new ValidationResult(items);
        String summary = result.getSummary();
        
        assertNotNull(summary);
        assertTrue(summary.length() > 0);
    }
}
