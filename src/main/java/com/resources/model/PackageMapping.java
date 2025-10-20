package com.resources.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 包名映射表 - 维护包名的Old→New映射
 * 支持前缀匹配模式
 * 线程安全实现
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class PackageMapping {
    
    /**
     * 替换模式
     */
    public enum ReplaceMode {
        PREFIX_MATCH,  // 前缀匹配（com.example → com.newapp，会替换com.example.ui → com.newapp.ui）
        EXACT_MATCH    // 精确匹配（只替换完全相等的包名）
    }
    
    /**
     * 包映射项
     */
    public static class MappingEntry {
        private final String oldPackage;
        private final String newPackage;
        private final ReplaceMode mode;
        
        public MappingEntry(String oldPackage, String newPackage, ReplaceMode mode) {
            this.oldPackage = Objects.requireNonNull(oldPackage);
            this.newPackage = Objects.requireNonNull(newPackage);
            this.mode = Objects.requireNonNull(mode);
        }
        
        public String getOldPackage() { return oldPackage; }
        public String getNewPackage() { return newPackage; }
        public ReplaceMode getMode() { return mode; }
        
        @Override
        public String toString() {
            return String.format("%s → %s (%s)", oldPackage, newPackage, mode);
        }
    }
    
    private final Map<String, MappingEntry> mappings;
    
    public PackageMapping() {
        this.mappings = new ConcurrentHashMap<>();
    }
    
    /**
     * 添加包映射
     * 
     * @param oldPackage 原包名
     * @param newPackage 新包名
     * @param mode 替换模式
     */
    public void addMapping(String oldPackage, String newPackage, ReplaceMode mode) {
        Objects.requireNonNull(oldPackage, "oldPackage不能为null");
        Objects.requireNonNull(newPackage, "newPackage不能为null");
        Objects.requireNonNull(mode, "mode不能为null");
        
        MappingEntry newEntry = new MappingEntry(oldPackage, newPackage, mode);
        
        // 使用putIfAbsent确保原子性
        MappingEntry existing = mappings.putIfAbsent(oldPackage, newEntry);
        
        if (existing != null) {
            // 已存在映射 - 检查是否冲突
            if (!existing.newPackage.equals(newPackage) || existing.mode != mode) {
                // 恢复原状
                mappings.put(oldPackage, existing);
                throw new IllegalArgumentException(
                    String.format("映射冲突: %s已有映射%s，无法添加%s → %s (%s)", 
                                 oldPackage, existing, oldPackage, newPackage, mode));
            }
            // 否则是重复添加相同映射，忽略
        }
    }
    
    /**
     * 添加前缀匹配映射
     */
    public void addPrefixMapping(String oldPackage, String newPackage) {
        addMapping(oldPackage, newPackage, ReplaceMode.PREFIX_MATCH);
    }
    
    /**
     * 添加精确匹配映射
     */
    public void addExactMapping(String oldPackage, String newPackage) {
        addMapping(oldPackage, newPackage, ReplaceMode.EXACT_MATCH);
    }
    
    /**
     * 替换包名/类名
     * 
     * @param fullClassName 完整类名（可能包含包名）
     * @return 替换后的类名，如果没有匹配则返回原值
     */
    public String replace(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return fullClassName;
        }
        
        // 按照映射的长度倒序排序（优先匹配最长的前缀）
        List<MappingEntry> sortedEntries = new ArrayList<>(mappings.values());
        sortedEntries.sort((a, b) -> Integer.compare(
            b.oldPackage.length(), a.oldPackage.length()));
        
        for (MappingEntry entry : sortedEntries) {
            if (entry.mode == ReplaceMode.EXACT_MATCH) {
                // 精确匹配
                if (fullClassName.equals(entry.oldPackage)) {
                    return entry.newPackage;
                }
            } else {
                // 前缀匹配
                if (fullClassName.startsWith(entry.oldPackage)) {
                    // 确保是完整的包名分隔（不是com.example匹配com.examples）
                    int prefixLen = entry.oldPackage.length();
                    if (fullClassName.length() == prefixLen || 
                        fullClassName.charAt(prefixLen) == '.') {
                        return entry.newPackage + fullClassName.substring(prefixLen);
                    }
                }
            }
        }
        
        return fullClassName;
    }
    
    /**
     * 检查是否包含映射
     */
    public boolean containsMapping(String oldPackage) {
        return mappings.containsKey(oldPackage);
    }
    
    /**
     * 获取所有映射
     */
    public Collection<MappingEntry> getAllMappings() {
        return new ArrayList<>(mappings.values());
    }
    
    /**
     * 获取映射数量
     */
    public int size() {
        return mappings.size();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return mappings.isEmpty();
    }
    
    /**
     * 从文件加载映射
     * 格式：oldPackage=newPackage[,mode]
     * 
     * @param path 文件路径
     * @throws IOException 读取失败
     */
    public void loadFromFile(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // 解析映射
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                throw new IOException(
                    String.format("文件%s第%d行格式错误: %s", path, i + 1, line));
            }
            
            String oldPackage = parts[0].trim();
            String rightPart = parts[1].trim();
            
            // 解析模式（可选）
            ReplaceMode mode = ReplaceMode.PREFIX_MATCH; // 默认前缀匹配
            String newPackage = rightPart;
            
            if (rightPart.contains(",")) {
                String[] modeParts = rightPart.split(",", 2);
                newPackage = modeParts[0].trim();
                String modeStr = modeParts[1].trim().toUpperCase();
                
                try {
                    mode = ReplaceMode.valueOf(modeStr);
                } catch (IllegalArgumentException e) {
                    throw new IOException(
                        String.format("文件%s第%d行包含无效的模式: %s", 
                                     path, i + 1, modeStr));
                }
            }
            
            addMapping(oldPackage, newPackage, mode);
        }
    }
    
    /**
     * 保存映射到文件
     * 
     * @param path 文件路径
     * @throws IOException 写入失败
     */
    public void saveToFile(String path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(path), StandardCharsets.UTF_8)) {
            
            writer.write("# Package Mapping File\n");
            writer.write("# Format: oldPackage=newPackage[,mode]\n");
            writer.write("# Mode: PREFIX_MATCH (default) or EXACT_MATCH\n");
            writer.write("# Generated at: " + new Date() + "\n");
            writer.write("\n");
            
            // 排序后写入
            List<MappingEntry> entries = new ArrayList<>(mappings.values());
            entries.sort(Comparator.comparing(MappingEntry::getOldPackage));
            
            for (MappingEntry entry : entries) {
                String line = entry.oldPackage + "=" + entry.newPackage;
                if (entry.mode != ReplaceMode.PREFIX_MATCH) {
                    line += "," + entry.mode;
                }
                writer.write(line + "\n");
            }
        }
    }
    
    /**
     * 清空所有映射
     */
    public void clear() {
        mappings.clear();
    }
    
    @Override
    public String toString() {
        return String.format("PackageMapping{size=%d, mappings=%s}", 
                           size(), mappings.values());
    }
}

