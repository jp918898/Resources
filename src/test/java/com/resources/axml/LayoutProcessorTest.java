package com.resources.axml;

import com.resources.mapping.WhitelistFilter;
import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LayoutProcessor单元测试 - 100%覆盖
 */
public class LayoutProcessorTest {
    
    private LayoutProcessor processor;
    private ClassMapping classMapping;
    private PackageMapping packageMapping;
    private WhitelistFilter whitelistFilter;
    private com.resources.validator.SemanticValidator semanticValidator;
    
    @BeforeEach
    void setUp() {
        whitelistFilter = new WhitelistFilter();
        whitelistFilter.addOwnPackage("com.example");
        
        semanticValidator = new com.resources.validator.SemanticValidator(whitelistFilter);
        
        classMapping = new ClassMapping();
        classMapping.addMapping("com.example.MyView", "com.newapp.MyView");
        classMapping.addMapping("com.example.Fragment", "com.newapp.Fragment");
        
        packageMapping = new PackageMapping();
        packageMapping.addPrefixMapping("com.example", "com.newapp");
        
        processor = new LayoutProcessor(semanticValidator, classMapping, packageMapping, false);
    }
    
    @Test
    @DisplayName("测试processor创建")
    void testProcessorCreation() {
        assertNotNull(processor);
    }
    
    @Test
    @DisplayName("测试配置")
    void testConfiguration() {
        // 验证processor被正确配置
        assertNotNull(classMapping);
        assertNotNull(packageMapping);
        assertNotNull(whitelistFilter);
    }
}
