package com.resources.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEX类加载缓存
 * 
 * 使用LRU策略缓存DEX文件的类列表，避免重复加载
 * 缓存键：文件路径 + 修改时间
 * 线程安全实现
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class DexClassCache {
    
    private static final Logger log = LoggerFactory.getLogger(DexClassCache.class);
    
    // 默认缓存大小（最多缓存10个DEX文件的类列表）
    private static final int DEFAULT_CACHE_SIZE = 10;
    
    // 缓存键
    private static class CacheKey {
        private final String path;
        private final long lastModified;
        
        public CacheKey(String path, long lastModified) {
            this.path = path;
            this.lastModified = lastModified;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return lastModified == cacheKey.lastModified && 
                   Objects.equals(path, cacheKey.path);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(path, lastModified);
        }
        
        @Override
        public String toString() {
            return String.format("CacheKey{path='%s', modified=%d}", path, lastModified);
        }
    }
    
    // 缓存值
    private static class CacheValue {
        private final Set<String> classes;
        private final long cacheTime;
        private long lastAccessTime;
        
        public CacheValue(Set<String> classes) {
            this.classes = new HashSet<>(classes); // 防御性复制
            this.cacheTime = System.currentTimeMillis();
            this.lastAccessTime = cacheTime;
        }
        
        public Set<String> getClasses() {
            lastAccessTime = System.currentTimeMillis();
            return new HashSet<>(classes); // 防御性复制
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
    
    // LRU缓存实现
    private final Map<CacheKey, CacheValue> cache;
    private final int maxSize;
    
    // 统计信息
    private long hits = 0;
    private long misses = 0;
    
    /**
     * 默认构造函数（缓存大小=10）
     */
    public DexClassCache() {
        this(DEFAULT_CACHE_SIZE);
    }
    
    /**
     * 指定缓存大小的构造函数
     * 
     * @param maxSize 最大缓存条目数
     */
    public DexClassCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize必须大于0");
        }
        
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(maxSize);
        
        log.info("DEX类缓存初始化: maxSize={}", maxSize);
    }
    
    /**
     * 获取DEX文件的类列表（带缓存）
     * 
     * @param dexPath DEX文件路径
     * @return 类名集合
     * @throws IOException 加载失败
     */
    public Set<String> getDexClasses(String dexPath) throws IOException {
        Objects.requireNonNull(dexPath, "dexPath不能为null");
        
        // 1. 获取文件修改时间
        File dexFile = new File(dexPath);
        if (!dexFile.exists()) {
            throw new IOException("DEX文件不存在: " + dexPath);
        }
        
        long lastModified = dexFile.lastModified();
        CacheKey key = new CacheKey(dexPath, lastModified);
        
        // 2. 尝试从缓存获取
        CacheValue cachedValue = cache.get(key);
        if (cachedValue != null) {
            hits++;
            log.debug("DEX缓存命中: {} (命中率: {}/{})", 
                    dexPath, hits, hits + misses);
            return cachedValue.getClasses();
        }
        
        // 3. 缓存未命中，加载DEX
        misses++;
        log.debug("DEX缓存未命中: {}, 开始加载...", dexPath);
        
        Set<String> classes = DexUtils.loadDexClasses(dexPath);
        
        // 4. 存入缓存
        putCache(key, new CacheValue(classes));
        
        log.info("DEX类加载并缓存: {} ({} 个类)", dexPath, classes.size());
        
        return new HashSet<>(classes); // 防御性复制
    }
    
    /**
     * 添加到缓存（LRU驱逐策略）
     */
    private void putCache(CacheKey key, CacheValue value) {
        // 如果缓存已满，驱逐最久未使用的条目
        if (cache.size() >= maxSize) {
            evictLRU();
        }
        
        cache.put(key, value);
    }
    
    /**
     * 驱逐最久未使用的条目（LRU）
     */
    private void evictLRU() {
        if (cache.isEmpty()) {
            return;
        }
        
        // 找到最久未访问的条目
        CacheKey lruKey = null;
        long oldestAccessTime = Long.MAX_VALUE;
        
        for (Map.Entry<CacheKey, CacheValue> entry : cache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestAccessTime) {
                oldestAccessTime = accessTime;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
            log.debug("驱逐LRU缓存条目: {}", lruKey);
        }
    }
    
    /**
     * 清除缓存
     */
    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
        log.info("DEX缓存已清空");
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("DEX缓存: size=%d/%d, hits=%d, misses=%d, hitRate=%.2f%%",
                           cache.size(), maxSize, hits, misses, getHitRate() * 100);
    }
    
    @Override
    public String toString() {
        return getStatistics();
    }
}

