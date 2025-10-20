package com.resources.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类名映射表 - 维护Old→New的双向映射
 * 线程安全实现
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ClassMapping {
    
    private final Map<String, String> oldToNew;
    private final Map<String, String> newToOld;
    
    public ClassMapping() {
        this.oldToNew = new ConcurrentHashMap<>();
        this.newToOld = new ConcurrentHashMap<>();
    }
    
    /**
     * 添加映射
     * 
     * @param oldClass 原类名
     * @param newClass 新类名
     * @throws IllegalArgumentException 如果映射冲突
     */
    public void addMapping(String oldClass, String newClass) {
        Objects.requireNonNull(oldClass, "oldClass不能为null");
        Objects.requireNonNull(newClass, "newClass不能为null");
        
        // 使用putIfAbsent确保原子性（检查冲突）
        String existingNewClass = oldToNew.putIfAbsent(oldClass, newClass);
        if (existingNewClass != null && !existingNewClass.equals(newClass)) {
            // 恢复原状（因为冲突）
            oldToNew.put(oldClass, existingNewClass);
            throw new IllegalArgumentException(
                String.format("映射冲突: %s已映射到%s，无法再映射到%s", 
                             oldClass, existingNewClass, newClass));
        }
        
        String existingOldClass = newToOld.putIfAbsent(newClass, oldClass);
        if (existingOldClass != null && !existingOldClass.equals(oldClass)) {
            // 恢复原状（因为冲突）
            newToOld.put(newClass, existingOldClass);
            // 如果oldToNew是新添加的，也要移除
            if (existingNewClass == null) {
                oldToNew.remove(oldClass);
            }
            throw new IllegalArgumentException(
                String.format("映射冲突: %s已被%s映射，无法再被%s映射", 
                             newClass, existingOldClass, oldClass));
        }
    }
    
    /**
     * 获取新类名
     * 
     * @param oldClass 原类名
     * @return 新类名，如果不存在则返回null
     */
    public String getNewClass(String oldClass) {
        return oldToNew.get(oldClass);
    }
    
    /**
     * 获取原类名
     * 
     * @param newClass 新类名
     * @return 原类名，如果不存在则返回null
     */
    public String getOldClass(String newClass) {
        return newToOld.get(newClass);
    }
    
    /**
     * 检查是否包含原类名
     */
    public boolean containsOldClass(String oldClass) {
        return oldToNew.containsKey(oldClass);
    }
    
    /**
     * 检查是否包含新类名
     */
    public boolean containsNewClass(String newClass) {
        return newToOld.containsKey(newClass);
    }
    
    /**
     * 获取所有原类名
     */
    public Set<String> getAllOldClasses() {
        return new HashSet<>(oldToNew.keySet());
    }
    
    /**
     * 获取所有新类名
     */
    public Set<String> getAllNewClasses() {
        return new HashSet<>(newToOld.keySet());
    }
    
    /**
     * 获取映射数量
     */
    public int size() {
        return oldToNew.size();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return oldToNew.isEmpty();
    }
    
    /**
     * 从文件加载映射（每行格式: oldClass=newClass）
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
                    String.format("文件%s第%d行格式错误: %s (期望格式: oldClass=newClass)", 
                                 path, i + 1, line));
            }
            
            String oldClass = parts[0].trim();
            String newClass = parts[1].trim();
            
            if (oldClass.isEmpty() || newClass.isEmpty()) {
                throw new IOException(
                    String.format("文件%s第%d行包含空类名: %s", path, i + 1, line));
            }
            
            addMapping(oldClass, newClass);
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
            
            // 写入头部注释
            writer.write("# Class Mapping File\n");
            writer.write("# Format: oldClass=newClass\n");
            writer.write("# Generated at: " + new Date() + "\n");
            writer.write("\n");
            
            // 排序后写入（便于阅读和diff）
            List<String> oldClasses = new ArrayList<>(oldToNew.keySet());
            Collections.sort(oldClasses);
            
            for (String oldClass : oldClasses) {
                String newClass = oldToNew.get(oldClass);
                writer.write(oldClass + "=" + newClass + "\n");
            }
        }
    }
    
    /**
     * 清空所有映射
     */
    public void clear() {
        oldToNew.clear();
        newToOld.clear();
    }
    
    @Override
    public String toString() {
        return String.format("ClassMapping{size=%d, mappings=%s}", 
                           size(), oldToNew);
    }
}

