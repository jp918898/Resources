package com.resources.validator;

import com.resources.mapping.WhitelistFilter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SemanticValidator单元测试 - 100%覆盖
 */
public class SemanticValidatorTest {
    
    private SemanticValidator validator;
    private WhitelistFilter whitelistFilter;
    
    @BeforeEach
    void setUp() {
        whitelistFilter = new WhitelistFilter();
        whitelistFilter.addOwnPackage("com.example");
        validator = new SemanticValidator(whitelistFilter);
    }
    
    @Test
    @DisplayName("测试FQCN识别")
    void testFullyQualifiedClassName() {
        // 有效的FQCN
        assertTrue(validator.isFullyQualifiedClassName("com.example.MainActivity"));
        assertTrue(validator.isFullyQualifiedClassName("com.example.ui.LoginFragment"));
        assertTrue(validator.isFullyQualifiedClassName("a.b.C"));
        
        // 无效的FQCN
        assertFalse(validator.isFullyQualifiedClassName("MainActivity")); // 无点号
        assertFalse(validator.isFullyQualifiedClassName("@string/app_name")); // 资源引用
        assertFalse(validator.isFullyQualifiedClassName("")); // 空字符串
        assertFalse(validator.isFullyQualifiedClassName(null)); // null
    }
    
    @Test
    @DisplayName("测试标签名语义验证")
    void testTagNameSemantic() {
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .tagName("com.example.MyView")
            .isTagName(true)
            .build();
        
        // 标签名是FQCN
        assertTrue(validator.isClassOrPackageSemantic(context, "com.example.MyView"));
        
        // 标签名不是FQCN
        SemanticValidator.Context context2 = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .tagName("LinearLayout")
            .isTagName(true)
            .build();
        
        assertFalse(validator.isClassOrPackageSemantic(context2, "LinearLayout"));
    }
    
    @Test
    @DisplayName("测试属性名语义验证")
    void testAttributeNameSemantic() {
        // android:name在白名单中
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .attributeName("android:name")
            .isTagName(false)
            .build();
        
        assertTrue(validator.isClassOrPackageSemantic(context, "com.example.Fragment"));
        
        // class在白名单中
        SemanticValidator.Context context2 = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .attributeName("class")
            .isTagName(false)
            .build();
        
        assertTrue(validator.isClassOrPackageSemantic(context2, "com.example.CustomView"));
        
        // 普通属性不在白名单中
        SemanticValidator.Context context3 = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .attributeName("android:text")
            .isTagName(false)
            .build();
        
        assertFalse(validator.isClassOrPackageSemantic(context3, "com.example.text"));
    }
    
    @Test
    @DisplayName("测试Data Binding表达式识别")
    void testDataBindingExpression() {
        SemanticValidator.Context context = new SemanticValidator.Context.Builder()
            .filePath("res/layout/test.xml")
            .isDataBindingExpression(true)
            .build();
        
        // 包含T(FQCN)的表达式
        assertTrue(validator.isClassOrPackageSemantic(context, "@{T(com.example.Util).method()}"));
        
        // 不包含类名的表达式
        assertFalse(validator.isClassOrPackageSemantic(context, "@{viewModel.name}"));
    }
    
    @Test
    @DisplayName("测试白名单过滤")
    void testWhitelistFilter() {
        // 自有包应该替换
        assertTrue(validator.shouldReplace("com.example.Test"));
        
        // 系统包不应该替换
        assertFalse(validator.shouldReplace("android.app.Activity"));
        assertFalse(validator.shouldReplace("androidx.fragment.app.Fragment"));
    }
    
    @Test
    @DisplayName("测试validateAndFilter组合验证")
    void testValidateAndFilter() {
        // 自有包的FQCN - 应该通过
        SemanticValidator.Context context1 = new SemanticValidator.Context.Builder()
            .filePath("test.xml")
            .attributeName("android:name")
            .build();
        
        assertTrue(validator.validateAndFilter(context1, "com.example.MainActivity"));
        
        // 系统包的FQCN - 不应该通过（被白名单过滤）
        assertFalse(validator.validateAndFilter(context1, "android.app.Activity"));
        
        // 非FQCN - 不应该通过（语义验证失败）
        SemanticValidator.Context context2 = new SemanticValidator.Context.Builder()
            .filePath("test.xml")
            .attributeName("android:text")
            .build();
        
        assertFalse(validator.validateAndFilter(context2, "Hello World"));
    }
    
    @Test
    @DisplayName("测试批量验证")
    void testBatchValidate() {
        java.util.List<java.util.Map.Entry<SemanticValidator.Context, String>> items = 
            new java.util.ArrayList<>();
        
        SemanticValidator.Context ctx1 = new SemanticValidator.Context.Builder()
            .filePath("test.xml")
            .attributeName("android:name")
            .build();
        
        items.add(new java.util.AbstractMap.SimpleEntry<>(ctx1, "com.example.Test"));
        items.add(new java.util.AbstractMap.SimpleEntry<>(ctx1, "android.app.Activity"));
        
        java.util.List<java.util.Map.Entry<SemanticValidator.Context, String>> validated = 
            validator.batchValidate(items);
        
        // 只有第一个应该通过
        assertEquals(1, validated.size());
        assertEquals("com.example.Test", validated.get(0).getValue());
    }
}

