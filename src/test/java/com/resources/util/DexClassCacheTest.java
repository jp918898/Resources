package com.resources.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DexClassCache单元测试
 */
class DexClassCacheTest {
    
    private DexClassCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new DexClassCache(3); // 小缓存便于测试LRU
    }
    
    @Test
    @DisplayName("构造函数 - 无效大小")
    void testConstructor_InvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DexClassCache(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DexClassCache(-1);
        });
    }
    
    @Test
    @DisplayName("缓存基本功能")
    void testCacheBasics() throws IOException {
        String dexPath = "input/dex/classes.dex";
        assumeTrue(DexUtils.isDexFileAccessible(dexPath), "跳过测试: DEX文件不存在");
        
        // 第一次加载（缓存未命中）
        Set<String> classes1 = cache.getDexClasses(dexPath);
        assertNotNull(classes1);
        assertEquals(1, cache.size());
        
        // 第二次加载（缓存命中）
        Set<String> classes2 = cache.getDexClasses(dexPath);
        assertNotNull(classes2);
        assertEquals(classes1.size(), classes2.size());
        assertEquals(1, cache.size()); // 缓存大小不变
        
        // 验证缓存命中率
        assertTrue(cache.getHitRate() > 0, "应该有缓存命中");
        
        System.out.println("✓ 缓存统计: " + cache.getStatistics());
    }
    
    @Test
    @DisplayName("清除缓存")
    void testClearCache() throws IOException {
        String dexPath = "input/dex/classes.dex";
        assumeTrue(DexUtils.isDexFileAccessible(dexPath), "跳过测试: DEX文件不存在");
        
        // 加载并缓存
        cache.getDexClasses(dexPath);
        assertEquals(1, cache.size());
        
        // 清除缓存
        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(0.0, cache.getHitRate());
    }
    
    @Test
    @DisplayName("缓存不可变性")
    void testCacheImmutability() throws IOException {
        String dexPath = "input/dex/classes.dex";
        assumeTrue(DexUtils.isDexFileAccessible(dexPath), "跳过测试: DEX文件不存在");
        
        // 第一次获取
        Set<String> classes1 = cache.getDexClasses(dexPath);
        int originalSize = classes1.size();
        
        // 修改返回的集合
        classes1.clear();
        assertEquals(0, classes1.size());
        
        // 第二次获取（应该不受影响）
        Set<String> classes2 = cache.getDexClasses(dexPath);
        assertEquals(originalSize, classes2.size(), 
                    "缓存应该返回防御性副本，不受外部修改影响");
    }
    
    @Test
    @DisplayName("统计信息格式")
    void testStatisticsFormat() {
        String stats = cache.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("DEX缓存"));
        assertTrue(stats.contains("size="));
        assertTrue(stats.contains("hits="));
        assertTrue(stats.contains("misses="));
    }
}

