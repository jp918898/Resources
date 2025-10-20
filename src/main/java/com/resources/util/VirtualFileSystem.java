package com.resources.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

/**
 * 虚拟文件系统（VFS）
 * 
 * 功能：
 * - 将APK解压到内存中的虚拟文件系统
 * - 像访问硬盘一样访问VFS中的文件
 * - 支持文件的读取、修改、写入
 * - 线程安全
 * 
 * 设计目标：
 * - 其他模块可以直接通过VFS路径访问文件
 * - 避免频繁的ZIP操作
 * - 统一的文件访问接口
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class VirtualFileSystem {
    
    private static final Logger log = LoggerFactory.getLogger(VirtualFileSystem.class);
    
    // 默认文件大小限制
    private static final long DEFAULT_MAX_FILE_SIZE = 100 * 1024 * 1024L; // 100MB
    private static final long DEFAULT_MAX_TOTAL_SIZE = 2 * 1024 * 1024 * 1024L; // 2GB
    
    // VFS存储：路径 -> 文件数据
    private final Map<String, VirtualFile> fileSystem;
    
    // VFS根路径
    private final String vfsRoot;
    
    // 是否已加载（使用AtomicBoolean确保线程安全）
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    
    // 文件大小限制（可配置）
    private long maxFileSize = DEFAULT_MAX_FILE_SIZE;
    private long maxTotalSize = DEFAULT_MAX_TOTAL_SIZE;
    
    public VirtualFileSystem() {
        this("vfs:/");
    }
    
    public VirtualFileSystem(String vfsRoot) {
        this.vfsRoot = Objects.requireNonNull(vfsRoot, "vfsRoot不能为null");
        this.fileSystem = new ConcurrentHashMap<>();
        // loaded已经在字段声明时初始化为false
        
        log.info("VFS初始化: root={}, maxFileSize={}MB, maxTotalSize={}MB", 
                vfsRoot, maxFileSize / 1024 / 1024, maxTotalSize / 1024 / 1024);
    }
    
    /**
     * 设置单个文件大小限制
     * 
     * @param maxFileSize 最大文件大小（字节）
     */
    public void setMaxFileSize(long maxFileSize) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("maxFileSize必须大于0");
        }
        this.maxFileSize = maxFileSize;
        log.info("设置最大文件大小: {}MB", maxFileSize / 1024 / 1024);
    }
    
    /**
     * 设置VFS总大小限制
     * 
     * @param maxTotalSize 最大总大小（字节）
     */
    public void setMaxTotalSize(long maxTotalSize) {
        if (maxTotalSize <= 0) {
            throw new IllegalArgumentException("maxTotalSize必须大于0");
        }
        this.maxTotalSize = maxTotalSize;
        log.info("设置最大总大小: {}MB", maxTotalSize / 1024 / 1024);
    }
    
    /**
     * 获取最大文件大小限制
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * 获取最大总大小限制
     */
    public long getMaxTotalSize() {
        return maxTotalSize;
    }
    
    /**
     * 虚拟文件
     */
    public static class VirtualFile {
        private final String path;
        private byte[] data;
        private final long originalSize;
        private long lastModified;
        private boolean modified;
        
        // ZIP元数据（保留原始压缩信息）
        private int compressionMethod = ZipEntry.DEFLATED;  // 默认DEFLATED
        private long crc = -1;  // CRC32校验和
        private byte[] extra = null;  // 额外字段
        private String comment = null;  // 注释
        
        public VirtualFile(String path, byte[] data) {
            this.path = Objects.requireNonNull(path);
            this.data = Objects.requireNonNull(data).clone();
            this.originalSize = data.length;
            this.lastModified = System.currentTimeMillis();
            this.modified = false;
        }
        
        /**
         * 从ZipEntry创建VirtualFile（保留所有元数据）
         */
        public VirtualFile(String path, byte[] data, ZipEntry originalEntry) {
            this(path, data);
            if (originalEntry != null) {
                this.compressionMethod = originalEntry.getMethod();
                this.crc = originalEntry.getCrc();
                this.extra = originalEntry.getExtra();
                this.comment = originalEntry.getComment();
                this.lastModified = originalEntry.getTime();
            }
        }
        
        public String getPath() { return path; }
        public byte[] getData() { return data.clone(); }
        public long getSize() { return data.length; }
        public long getOriginalSize() { return originalSize; }
        public long getLastModified() { return lastModified; }
        public boolean isModified() { return modified; }
        
        // ZIP元数据访问器
        public int getCompressionMethod() { return compressionMethod; }
        public long getCrc() { return crc; }
        public byte[] getExtra() { return extra != null ? extra.clone() : null; }
        public String getComment() { return comment; }
        
        public void setData(byte[] newData) {
            this.data = Objects.requireNonNull(newData).clone();
            this.lastModified = System.currentTimeMillis();
            this.modified = true;
            // 数据修改后CRC失效
            this.crc = -1;
        }
        
        /**
         * 创建ZipEntry（恢复所有元数据）
         */
        public ZipEntry toZipEntry(String entryName) {
            ZipEntry entry = new ZipEntry(entryName);
            entry.setMethod(compressionMethod);
            entry.setTime(lastModified);
            
            if (crc >= 0 && !modified) {
                // 只有未修改的文件才使用原始CRC
                entry.setCrc(crc);
            }
            
            if (extra != null) {
                entry.setExtra(extra);
            }
            
            if (comment != null) {
                entry.setComment(comment);
            }
            
            // 如果是STORED方法，必须设置大小和CRC
            if (compressionMethod == ZipEntry.STORED) {
                entry.setSize(data.length);
                if (crc < 0 || modified) {
                    // 重新计算CRC
                    java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
                    crc32.update(data);
                    entry.setCrc(crc32.getValue());
                }
            }
            
            return entry;
        }
        
        @Override
        public String toString() {
            return String.format("VirtualFile{path='%s', size=%d, modified=%b, method=%s}", 
                               path, data.length, modified, 
                               compressionMethod == ZipEntry.STORED ? "STORED" : "DEFLATED");
        }
    }
    
    /**
     * 从APK加载到VFS
     * 
     * @param apkPath APK文件路径
     * @return 加载的文件数量
     * @throws IOException 加载失败
     */
    public int loadFromApk(String apkPath) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.info("从APK加载到VFS: {}", apkPath);
        
        fileSystem.clear();
        int count = 0;
        long currentTotalSize = 0;
        
        try (ZipFile zipFile = new ZipFile(apkPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                if (!entry.isDirectory()) {
                    String originalName = entry.getName();
                    String entryPath;
                    
                    // 🔒 ZIP Slip防护：验证ZIP Entry路径
                    try {
                        entryPath = normalizeVfsPath(originalName);
                        validateVfsPath(entryPath);
                    } catch (IllegalArgumentException e) {
                        log.warn("跳过非法ZIP Entry: {} ({})", originalName, e.getMessage());
                        continue;
                    }
                    
                    long entrySize = entry.getSize();
                    
                    // 检查单个文件大小限制
                    if (entrySize > maxFileSize) {
                        log.warn("跳过超大文件: {} (大小: {} MB, 限制: {} MB)", 
                                entryPath, entrySize / 1024 / 1024, maxFileSize / 1024 / 1024);
                        continue;
                    }
                    
                    // 检查总大小限制
                    if (currentTotalSize + entrySize > maxTotalSize) {
                        throw new IOException(
                            String.format("VFS总大小超限: 当前=%d MB, 新增=%d MB, 限制=%d MB",
                                        currentTotalSize / 1024 / 1024,
                                        entrySize / 1024 / 1024,
                                        maxTotalSize / 1024 / 1024));
                    }
                    
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        byte[] data = is.readAllBytes();
                        
                        // 二次验证实际读取的大小（ZIP可能报告不准确）
                        if (data.length > maxFileSize) {
                            log.warn("跳过实际大小超限的文件: {} ({} MB)", 
                                    entryPath, data.length / 1024 / 1024);
                            continue;
                        }
                        
                        // 使用新构造函数保留ZIP元数据
                        VirtualFile vFile = new VirtualFile(entryPath, data, entry);
                        fileSystem.put(entryPath, vFile);
                        count++;
                        currentTotalSize += data.length;
                        
                        log.trace("加载到VFS: {} ({} 字节, method={})", 
                                entryPath, data.length,
                                entry.getMethod() == ZipEntry.STORED ? "STORED" : "DEFLATED");
                    }
                }
            }
        }
        
        loaded.set(true);
        log.info("VFS加载完成: {} 个文件, 总大小: {} MB", 
                count, currentTotalSize / 1024 / 1024);
        
        return count;
    }
    
    /**
     * 从VFS导出到APK
     * 
     * @param apkPath 输出APK路径
     * @return 写入的文件数量
     * @throws IOException 导出失败
     */
    public int saveToApk(String apkPath) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        if (!loaded.get()) {
            throw new IllegalStateException("VFS未加载");
        }
        
        log.info("从VFS导出到APK: {}", apkPath);
        
        // 确保输出目录存在
        Path outputPath = Paths.get(apkPath);
        Files.createDirectories(outputPath.getParent());
        
        int count = 0;
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(apkPath))) {
            
            // 按路径排序（保持ZIP文件的可重现性）
            List<String> sortedPaths = new ArrayList<>(fileSystem.keySet());
            Collections.sort(sortedPaths);
            
            for (String vfsPath : sortedPaths) {
                VirtualFile vFile = fileSystem.get(vfsPath);
                
                // 转换VFS路径到ZIP entry路径
                String zipEntryPath = vfsPathToZipEntry(vfsPath);
                
                // 使用VirtualFile.toZipEntry恢复所有元数据
                ZipEntry entry = vFile.toZipEntry(zipEntryPath);
                
                zos.putNextEntry(entry);
                zos.write(vFile.getData());
                zos.closeEntry();
                
                count++;
                
                log.trace("写入ZIP: {} ({} 字节, method={}, modified={})", 
                        zipEntryPath, vFile.getSize(),
                        vFile.getCompressionMethod() == ZipEntry.STORED ? "STORED" : "DEFLATED",
                        vFile.isModified());
            }
        }
        
        log.info("VFS导出完成: {} 个文件", count);
        
        return count;
    }
    
    /**
     * 读取VFS文件
     * 
     * @param vfsPath VFS路径（如"vfs:/resources.arsc"或"resources.arsc"）
     * @return 文件数据
     * @throws FileNotFoundException 文件不存在
     * @throws IllegalArgumentException 路径非法
     */
    public byte[] readFile(String vfsPath) throws FileNotFoundException {
        vfsPath = normalizeVfsPath(vfsPath);
        validateVfsPath(vfsPath);
        
        VirtualFile vFile = fileSystem.get(vfsPath);
        if (vFile == null) {
            throw new FileNotFoundException("VFS文件不存在: " + vfsPath);
        }
        
        log.trace("VFS读取: {} ({} 字节)", vfsPath, vFile.getSize());
        return vFile.getData();
    }
    
    /**
     * 写入VFS文件（修改或创建）
     * 
     * @param vfsPath VFS路径
     * @param data 文件数据
     * @throws IllegalArgumentException 路径非法
     */
    public void writeFile(String vfsPath, byte[] data) {
        Objects.requireNonNull(data, "data不能为null");
        vfsPath = normalizeVfsPath(vfsPath);
        validateVfsPath(vfsPath);
        
        VirtualFile vFile = fileSystem.get(vfsPath);
        if (vFile != null) {
            // 修改现有文件
            vFile.setData(data);
            log.debug("VFS修改: {} ({} -> {} 字节)", 
                     vfsPath, vFile.getOriginalSize(), data.length);
        } else {
            // 创建新文件
            vFile = new VirtualFile(vfsPath, data);
            fileSystem.put(vfsPath, vFile);
            log.debug("VFS创建: {} ({} 字节)", vfsPath, data.length);
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param vfsPath VFS路径
     * @return true=存在
     * @throws IllegalArgumentException 路径非法
     */
    public boolean exists(String vfsPath) {
        vfsPath = normalizeVfsPath(vfsPath);
        validateVfsPath(vfsPath);
        return fileSystem.containsKey(vfsPath);
    }
    
    /**
     * 删除VFS文件
     * 
     * @param vfsPath VFS路径
     * @return true=删除成功
     * @throws IllegalArgumentException 路径非法
     */
    public boolean deleteFile(String vfsPath) {
        vfsPath = normalizeVfsPath(vfsPath);
        validateVfsPath(vfsPath);
        
        VirtualFile removed = fileSystem.remove(vfsPath);
        if (removed != null) {
            log.debug("VFS删除: {}", vfsPath);
            return true;
        }
        
        return false;
    }
    
    /**
     * 列出目录下的所有文件
     * 
     * @param dirPath 目录路径（如"res/layout"）
     * @return VFS路径列表
     * @throws IllegalArgumentException 路径非法
     */
    public List<String> listFiles(String dirPath) {
        dirPath = normalizeVfsPath(dirPath);
        validateVfsPath(dirPath);
        
        // 确保以/结尾
        if (!dirPath.isEmpty() && !dirPath.endsWith("/")) {
            dirPath += "/";
        }
        
        List<String> files = new ArrayList<>();
        
        for (String vfsPath : fileSystem.keySet()) {
            if (vfsPath.startsWith(dirPath)) {
                files.add(vfsPath);
            }
        }
        
        Collections.sort(files);
        log.debug("VFS列表: {} -> {} 个文件", dirPath, files.size());
        
        return files;
    }
    
    /**
     * 列出匹配模式的所有文件
     * 
     * @param pattern 模式（如"res/layout/**\/*.xml"）
     * @return VFS路径列表
     */
    public List<String> listFilesByPattern(String pattern) {
        Objects.requireNonNull(pattern, "pattern不能为null");
        
        // 转换glob模式到正则表达式
        // 关键修复：使用占位符避免替换顺序导致的相互干扰
        
        String regex = pattern;
        
        // 1. 转义特殊字符（.需要转义为\.）
        regex = regex.replace(".", "\\.");
        
        // 2. 使用唯一占位符标记**和*（避免后续替换相互干扰）
        regex = regex.replace("**/", "<<<DOUBLESTAR_SLASH>>>");
        regex = regex.replace("/**", "<<<SLASH_DOUBLESTAR>>>");
        regex = regex.replace("**", "<<<DOUBLESTAR>>>");
        regex = regex.replace("*", "<<<STAR>>>");
        
        // 3. 替换占位符为实际正则表达式
        // **/ -> 匹配任意层级目录（包括0层）
        regex = regex.replace("<<<DOUBLESTAR_SLASH>>>", "(.*/)?");
        // /** -> 匹配任意路径后缀
        regex = regex.replace("<<<SLASH_DOUBLESTAR>>>", "(/.*)?");
        // ** -> 匹配任意字符（包括/）
        regex = regex.replace("<<<DOUBLESTAR>>>", ".*");
        // * -> 匹配单个路径部分（不包括/）
        regex = regex.replace("<<<STAR>>>", "[^/]*");
        
        List<String> files = new ArrayList<>();
        
        for (String vfsPath : fileSystem.keySet()) {
            if (vfsPath.matches(regex)) {
                files.add(vfsPath);
            }
        }
        
        Collections.sort(files);
        log.debug("VFS模式匹配: pattern='{}', regex='{}' -> {} 个文件", 
                 pattern, regex, files.size());
        
        return files;
    }
    
    /**
     * 获取所有VFS路径
     * 
     * @return 所有路径列表
     */
    public List<String> getAllPaths() {
        List<String> paths = new ArrayList<>(fileSystem.keySet());
        Collections.sort(paths);
        return paths;
    }
    
    /**
     * 获取已修改的文件列表
     * 
     * @return 已修改的文件路径
     */
    public List<String> getModifiedFiles() {
        List<String> modified = new ArrayList<>();
        
        for (Map.Entry<String, VirtualFile> entry : fileSystem.entrySet()) {
            if (entry.getValue().isModified()) {
                modified.add(entry.getKey());
            }
        }
        
        Collections.sort(modified);
        log.debug("VFS已修改文件: {} 个", modified.size());
        
        return modified;
    }
    
    /**
     * 获取文件数量
     * 
     * @return 文件总数
     */
    public int getFileCount() {
        return fileSystem.size();
    }
    
    /**
     * 获取VFS总大小
     * 
     * @return 总字节数
     */
    public long getTotalSize() {
        long total = 0;
        for (VirtualFile vFile : fileSystem.values()) {
            total += vFile.getSize();
        }
        return total;
    }
    
    /**
     * 清空VFS
     */
    public void clear() {
        fileSystem.clear();
        loaded.set(false);
        log.info("VFS已清空");
    }
    
    /**
     * VFS是否已加载
     */
    public boolean isLoaded() {
        return loaded.get();
    }
    
    /**
     * 规范化VFS路径
     * 
     * 支持的格式：
     * - "vfs:/resources.arsc"
     * - "resources.arsc"
     * - "res/layout/activity_main.xml"
     * - "res/./layout/test.xml" -> "res/layout/test.xml"
     * - "res//layout//test.xml" -> "res/layout/test.xml"
     * 
     * 统一转换为："resources.arsc", "res/layout/activity_main.xml"
     * 
     * 安全处理：
     * - 移除 . 和 .. 路径段（防止目录遍历）
     * - 移除冗余斜杠
     * - 统一分隔符为 /
     */
    private String normalizeVfsPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        // 步骤1: 移除vfs:前缀
        if (path.startsWith("vfs:")) {
            path = path.substring(4);
        }
        
        // 步骤2: 统一分隔符为/
        path = path.replace('\\', '/');
        
        // 步骤3: 移除开头的/
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // 步骤4: 处理 . 和 .. （手工实现，避免Paths.get()的跨平台问题）
        String[] segments = path.split("/");
        List<String> stack = new ArrayList<>();
        
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                // 跳过空段（双斜杠）和当前目录
                continue;
            } else if (segment.equals("..")) {
                // 返回上级目录
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
                // 如果stack为空，忽略..（防止越界）
            } else {
                stack.add(segment);
            }
        }
        
        // 步骤5: 重新组装路径
        if (stack.isEmpty()) {
            return "";
        }
        return String.join("/", stack);
    }
    
    /**
     * 验证VFS路径（安全检查）
     * 
     * 验证规则：
     * 1. 不允许包含 NULL 字符（路径截断攻击）
     * 2. 不允许包含控制字符（\u0000-\u001F, \u007F）
     * 3. 不允许包含 Windows 非法字符（<>:"|?*）
     * 4. 路径总长度限制（4096字符）
     * 5. 单个路径组件限制（255字符）
     * 6. 不允许以 / 开头（规范化后的路径必须是相对路径）
     * 
     * @param normalizedPath 已规范化的路径
     * @throws IllegalArgumentException 如果路径非法
     */
    private void validateVfsPath(String normalizedPath) throws IllegalArgumentException {
        // 验证1: 空路径检查
        if (normalizedPath == null) {
            throw new IllegalArgumentException("VFS路径不能为null");
        }
        
        // 空路径是合法的（表示根目录）
        if (normalizedPath.isEmpty()) {
            return;
        }
        
        // 验证2: 长度限制
        if (normalizedPath.length() > 4096) {
            throw new IllegalArgumentException(
                "VFS路径过长: " + normalizedPath.length() + " 字符（限制4096）");
        }
        
        // 验证3: 不允许以/开头（规范化后的路径必须是相对路径）
        if (normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException(
                "VFS路径不应以'/'开头: " + normalizedPath);
        }
        
        // 验证4: 特殊字符检查
        for (int i = 0; i < normalizedPath.length(); i++) {
            char c = normalizedPath.charAt(i);
            
            // NULL字符（路径截断攻击）
            if (c == '\0') {
                throw new IllegalArgumentException("VFS路径不允许包含NULL字符");
            }
            
            // 控制字符
            if (c < 0x20 || c == 0x7F) {
                throw new IllegalArgumentException(
                    String.format("VFS路径包含控制字符: \\u%04X", (int)c));
            }
            
            // Windows非法字符（保持跨平台一致性）
            if (c == '<' || c == '>' || c == ':' || c == '"' || 
                c == '|' || c == '?' || c == '*') {
                throw new IllegalArgumentException(
                    "VFS路径包含非法字符: '" + c + "'");
            }
        }
        
        // 验证5: 路径组件检查
        String[] components = normalizedPath.split("/");
        for (String comp : components) {
            if (comp.length() > 255) {
                throw new IllegalArgumentException(
                    "VFS路径组件过长: " + comp.length() + " 字符（限制255）");
            }
        }
    }
    
    /**
     * VFS路径转ZIP entry路径
     */
    private String vfsPathToZipEntry(String vfsPath) {
        return normalizeVfsPath(vfsPath);
    }
    
    /**
     * 转换为VFS URI
     * 
     * @param path 路径
     * @return VFS URI（如"vfs:/resources.arsc"）
     * @throws IllegalArgumentException 路径非法
     */
    public String toVfsUri(String path) {
        path = normalizeVfsPath(path);
        validateVfsPath(path);
        return vfsRoot + path;
    }
    
    /**
     * 生成VFS统计信息
     */
    public String getStatistics() {
        int totalFiles = fileSystem.size();
        int modifiedFiles = (int) fileSystem.values().stream()
            .filter(VirtualFile::isModified)
            .count();
        long totalSize = getTotalSize();
        
        return String.format("VFS统计: 文件=%d, 已修改=%d, 总大小=%d字节 (%.2f MB)", 
                           totalFiles, modifiedFiles, totalSize, totalSize / 1024.0 / 1024.0);
    }
    
    /**
     * 打印VFS目录结构
     * 
     * @param maxDepth 最大深度
     * @return 目录树字符串
     */
    public String printTree(int maxDepth) {
        StringBuilder sb = new StringBuilder();
        sb.append(vfsRoot).append("\n");
        
        // 按路径排序
        List<String> paths = new ArrayList<>(fileSystem.keySet());
        Collections.sort(paths);
        
        Map<String, List<String>> dirTree = new TreeMap<>();
        
        for (String path : paths) {
            String[] parts = path.split("/");
            int depth = parts.length;
            
            if (depth > maxDepth) {
                continue;
            }
            
            // 构建目录树
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < depth - 1; i++) {
                if (i > 0) currentPath.append("/");
                currentPath.append(parts[i]);
                
                String dir = currentPath.toString();
                dirTree.computeIfAbsent(dir, k -> new ArrayList<>());
            }
            
            if (depth > 0) {
                String parentDir = depth > 1 ? 
                    String.join("/", Arrays.copyOf(parts, depth - 1)) : "";
                List<String> children = dirTree.computeIfAbsent(parentDir, k -> new ArrayList<>());
                children.add(parts[depth - 1]);
            }
        }
        
        // 打印树
        printTreeNode(sb, "", dirTree, 0, maxDepth);
        
        return sb.toString();
    }
    
    private void printTreeNode(StringBuilder sb, String prefix, 
                              Map<String, List<String>> dirTree, 
                              int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            return;
        }
        
        List<String> children = dirTree.get(prefix);
        if (children != null) {
            Collections.sort(children);
            for (int i = 0; i < children.size(); i++) {
                String child = children.get(i);
                boolean isLast = (i == children.size() - 1);
                
                sb.append(isLast ? "└── " : "├── ").append(child).append("\n");
                
                String childPath = prefix.isEmpty() ? child : prefix + "/" + child;
                
                printTreeNode(sb, childPath, dirTree, currentDepth + 1, maxDepth);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("VFS{root='%s', files=%d, loaded=%b}", 
                           vfsRoot, fileSystem.size(), loaded);
    }
}

