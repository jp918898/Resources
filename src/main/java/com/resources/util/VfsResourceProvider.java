package com.resources.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * VFS资源提供者
 * 
 * 提供统一的资源访问接口，其他模块可以像访问硬盘一样访问VFS
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class VfsResourceProvider {
    
    private static final Logger log = LoggerFactory.getLogger(VfsResourceProvider.class);
    
    private final VirtualFileSystem vfs;
    
    public VfsResourceProvider(VirtualFileSystem vfs) {
        this.vfs = Objects.requireNonNull(vfs, "vfs不能为null");
    }
    
    /**
     * 读取resources.arsc
     * 
     * @return ARSC字节数据
     * @throws IOException 读取失败
     */
    public byte[] getResourcesArsc() throws IOException {
        return vfs.readFile("resources.arsc");
    }
    
    /**
     * 写入resources.arsc
     * 
     * @param data ARSC字节数据
     */
    public void setResourcesArsc(byte[] data) {
        vfs.writeFile("resources.arsc", data);
        log.info("VFS更新: resources.arsc ({} 字节)", data.length);
    }
    
    /**
     * 读取AndroidManifest.xml
     * 
     * @return XML字节数据
     * @throws IOException 读取失败
     */
    public byte[] getAndroidManifest() throws IOException {
        return vfs.readFile("AndroidManifest.xml");
    }
    
    /**
     * 写入AndroidManifest.xml
     * 
     * @param data XML字节数据
     */
    public void setAndroidManifest(byte[] data) {
        vfs.writeFile("AndroidManifest.xml", data);
        log.info("VFS更新: AndroidManifest.xml ({} 字节)", data.length);
    }
    
    /**
     * 获取所有layout文件
     * 
     * @return 路径到数据的映射
     */
    public Map<String, byte[]> getAllLayouts() {
        return getFilesByPattern("res/layout/**/*.xml");
    }
    
    /**
     * 获取所有menu文件
     * 
     * @return 路径到数据的映射
     */
    public Map<String, byte[]> getAllMenus() {
        return getFilesByPattern("res/menu/**/*.xml");
    }
    
    /**
     * 获取所有navigation文件
     * 
     * @return 路径到数据的映射
     */
    public Map<String, byte[]> getAllNavigations() {
        return getFilesByPattern("res/navigation/**/*.xml");
    }
    
    /**
     * 获取所有xml配置文件
     * 
     * @return 路径到数据的映射
     */
    public Map<String, byte[]> getAllXmlConfigs() {
        return getFilesByPattern("res/xml/**/*.xml");
    }
    
    /**
     * 根据模式获取文件
     * 
     * @param pattern 模式（如"res/layout/**\/*.xml"）
     * @return 路径到数据的映射
     */
    public Map<String, byte[]> getFilesByPattern(String pattern) {
        List<String> paths = vfs.listFilesByPattern(pattern);
        Map<String, byte[]> files = new LinkedHashMap<>();
        
        for (String path : paths) {
            try {
                byte[] data = vfs.readFile(path);
                files.put(path, data);
            } catch (FileNotFoundException e) {
                log.warn("VFS文件不存在: {}", path);
            }
        }
        
        log.debug("VFS按模式获取: {} -> {} 个文件", pattern, files.size());
        return files;
    }
    
    /**
     * 批量更新文件
     * 
     * @param files 路径到数据的映射
     */
    public void updateFiles(Map<String, byte[]> files) {
        Objects.requireNonNull(files, "files不能为null");
        
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            vfs.writeFile(entry.getKey(), entry.getValue());
        }
        
        log.info("VFS批量更新: {} 个文件", files.size());
    }
    
    /**
     * 获取DEX文件列表
     * 
     * @return DEX文件路径列表
     */
    public List<String> getDexFiles() {
        return vfs.listFilesByPattern("classes*.dex");
    }
    
    /**
     * 读取DEX文件
     * 
     * @param dexName DEX文件名（如"classes.dex"）
     * @return DEX字节数据
     * @throws IOException 读取失败
     */
    public byte[] getDexFile(String dexName) throws IOException {
        return vfs.readFile(dexName);
    }
    
    /**
     * 获取VFS统计信息
     */
    public String getStatistics() {
        return vfs.getStatistics();
    }
    
    /**
     * 打印VFS目录结构
     * 
     * @param maxDepth 最大深度
     * @return 目录树
     */
    public String printVfsTree(int maxDepth) {
        return vfs.printTree(maxDepth);
    }
}

