package com.resources.mapping;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WhitelistFilter单元测试 - 100%覆盖
 */
public class WhitelistFilterTest {
    
    private WhitelistFilter filter;
    
    @BeforeEach
    void setUp() {
        filter = new WhitelistFilter();
    }
    
    @Test
    @DisplayName("测试系统包识别")
    void testSystemPackageDetection() {
        // 系统包应该被过滤（不替换）
        assertFalse(filter.shouldReplace("android.app.Activity"));
        assertFalse(filter.shouldReplace("androidx.fragment.app.Fragment"));
        assertFalse(filter.shouldReplace("com.google.android.material.Button"));
        assertFalse(filter.shouldReplace("kotlin.collections.List"));
        assertFalse(filter.shouldReplace("java.lang.String"));
        assertFalse(filter.shouldReplace("javax.net.ssl.SSLContext"));
        
        assertTrue(filter.isSystemPackage("android.app.Activity"));
        assertTrue(filter.isSystemPackage("androidx.core.app.ActivityCompat"));
    }
    
    @Test
    @DisplayName("测试第三方库识别")
    void testThirdPartyDetection() {
        // 常见第三方库应该被过滤
        assertFalse(filter.shouldReplace("com.squareup.okhttp.OkHttpClient"));
        assertFalse(filter.shouldReplace("retrofit2.Retrofit"));
        assertFalse(filter.shouldReplace("io.reactivex.Observable"));
        assertFalse(filter.shouldReplace("com.facebook.react.ReactActivity"));
        
        assertTrue(filter.isCommonThirdParty("com.squareup.okhttp.OkHttpClient"));
        assertTrue(filter.isCommonThirdParty("okhttp3.OkHttpClient"));
    }
    
    @Test
    @DisplayName("测试自有包识别")
    void testOwnPackageDetection() {
        // 添加自有包前缀
        filter.addOwnPackage("com.myapp");
        filter.addOwnPackage("com.mycompany.lib");
        
        // 自有包应该被替换
        assertTrue(filter.shouldReplace("com.myapp.MainActivity"));
        assertTrue(filter.shouldReplace("com.myapp.ui.LoginActivity"));
        assertTrue(filter.shouldReplace("com.mycompany.lib.Utils"));
        
        assertTrue(filter.isOwnPackage("com.myapp.MainActivity"));
        
        // 其他包不应该被替换（默认保守策略）
        assertFalse(filter.shouldReplace("com.unknown.SomeClass"));
    }
    
    @Test
    @DisplayName("测试批量添加自有包")
    void testAddOwnPackagesBatch() {
        filter.addOwnPackages(java.util.List.of(
            "com.myapp",
            "com.mylib"
        ));
        
        assertEquals(2, filter.getOwnPackagePrefixes().size());
        assertTrue(filter.shouldReplace("com.myapp.Test"));
        assertTrue(filter.shouldReplace("com.mylib.Test"));
    }
    
    @Test
    @DisplayName("测试排除前缀")
    void testExcludePrefixes() {
        filter.addOwnPackage("com.myapp");
        filter.addExcludePrefix("com.myapp.thirdparty");
        
        // 自有包应该替换
        assertTrue(filter.shouldReplace("com.myapp.MainActivity"));
        
        // 排除的前缀不应该替换
        assertFalse(filter.shouldReplace("com.myapp.thirdparty.SomeLib"));
    }
    
    @Test
    @DisplayName("测试自动添加点号")
    void testAutoAppendDot() {
        // 不带点号的前缀应该自动添加
        filter.addOwnPackage("com.myapp");
        
        // 验证确实添加了点号
        assertTrue(filter.getOwnPackagePrefixes().contains("com.myapp."));
    }
    
    @Test
    @DisplayName("测试清空操作")
    void testClearOperations() {
        filter.addOwnPackage("com.myapp");
        filter.addExcludePrefix("com.exclude");
        
        assertEquals(1, filter.getOwnPackagePrefixes().size());
        assertEquals(1, filter.getExcludePrefixes().size());
        
        filter.clearOwnPackages();
        assertEquals(0, filter.getOwnPackagePrefixes().size());
        
        filter.clearExcludePrefixes();
        assertEquals(0, filter.getExcludePrefixes().size());
    }
    
    @Test
    @DisplayName("测试空字符串和null")
    void testEmptyAndNull() {
        assertFalse(filter.shouldReplace(null));
        assertFalse(filter.shouldReplace(""));
        assertFalse(filter.isSystemPackage(null));
        assertFalse(filter.isSystemPackage(""));
    }
}

