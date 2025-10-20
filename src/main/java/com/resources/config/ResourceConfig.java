package com.resources.config;

import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 资源配置类
 * 
 * 包含：
 * - 包名映射
 * - 类名映射
 * - 自有包前缀
 * - 处理目标
 * - 高级选项
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ResourceConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceConfig.class);
    
    private final PackageMapping packageMappings;
    private final ClassMapping classMappings;
    private final Set<String> ownPackagePrefixes;
    private final Set<String> targetDirs;
    private final List<String> dexPaths;
    
    // 选项
    private final boolean processToolsContext;
    private final boolean enableRuntimeValidation;
    private final boolean keepBackup;
    private final boolean parallelProcessing;
    
    private ResourceConfig(Builder builder) {
        this.packageMappings = builder.packageMappings;
        this.classMappings = builder.classMappings;
        this.ownPackagePrefixes = new HashSet<>(builder.ownPackagePrefixes);
        this.targetDirs = new HashSet<>(builder.targetDirs);
        this.dexPaths = new ArrayList<>(builder.dexPaths);
        this.processToolsContext = builder.processToolsContext;
        this.enableRuntimeValidation = builder.enableRuntimeValidation;
        this.keepBackup = builder.keepBackup;
        this.parallelProcessing = builder.parallelProcessing;
    }
    
    // Getters
    public PackageMapping getPackageMappings() { return packageMappings; }
    public ClassMapping getClassMappings() { return classMappings; }
    public Set<String> getOwnPackagePrefixes() { 
        return Collections.unmodifiableSet(ownPackagePrefixes); 
    }
    public Set<String> getTargetDirs() { 
        return Collections.unmodifiableSet(targetDirs); 
    }
    public List<String> getDexPaths() { 
        return Collections.unmodifiableList(dexPaths); 
    }
    public boolean isProcessToolsContext() { return processToolsContext; }
    public boolean isEnableRuntimeValidation() { return enableRuntimeValidation; }
    public boolean isKeepBackup() { return keepBackup; }
    public boolean isParallelProcessing() { return parallelProcessing; }
    
    /**
     * 从YAML文件加载配置
     * 
     * @param path YAML文件路径
     * @return ResourceConfig对象
     * @throws IOException 加载失败
     */
    public static ResourceConfig loadFromYaml(String path) throws IOException {
        Objects.requireNonNull(path, "path不能为null");
        
        log.info("加载配置文件: {}", path);
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)), 
                                       StandardCharsets.UTF_8);
            
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(content);
            
            // 处理空配置文件（空文件或仅注释）
            if (data == null) {
                log.info("配置文件为空，使用默认配置");
                data = new HashMap<>();
            }
            
            Builder builder = new Builder();
            
            // 1. 解析版本
            String version = (String) data.get("version");
            log.debug("配置版本: {}", version);
            
            // 2. 解析自有包前缀
            @SuppressWarnings("unchecked")
            List<String> ownPrefixes = (List<String>) data.get("own_package_prefixes");
            if (ownPrefixes != null) {
                builder.ownPackagePrefixes(ownPrefixes);
                log.debug("自有包前缀: {}", ownPrefixes);
            }
            
            // 3. 解析包名映射
            @SuppressWarnings("unchecked")
            Map<String, String> pkgMappings = (Map<String, String>) data.get("package_mappings");
            if (pkgMappings != null) {
                for (Map.Entry<String, String> entry : pkgMappings.entrySet()) {
                    builder.addPackageMapping(entry.getKey(), entry.getValue());
                }
                log.debug("包名映射: {} 条", pkgMappings.size());
            }
            
            // 4. 解析类名映射
            @SuppressWarnings("unchecked")
            Map<String, String> clsMappings = (Map<String, String>) data.get("class_mappings");
            if (clsMappings != null) {
                for (Map.Entry<String, String> entry : clsMappings.entrySet()) {
                    builder.addClassMapping(entry.getKey(), entry.getValue());
                }
                log.debug("类名映射: {} 条", clsMappings.size());
            }
            
            // 5. 解析处理目标
            @SuppressWarnings("unchecked")
            List<String> targets = (List<String>) data.get("targets");
            if (targets != null) {
                builder.targetDirs(targets);
                log.debug("处理目标: {}", targets);
            }
            
            // 6. 解析DEX路径
            @SuppressWarnings("unchecked")
            List<String> dexPaths = (List<String>) data.get("dex_paths");
            if (dexPaths != null) {
                builder.dexPaths(dexPaths);
                log.debug("DEX路径: {}", dexPaths);
            }
            
            // 7. 解析选项
            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) data.get("options");
            if (options != null) {
                Boolean processTools = (Boolean) options.get("process_tools_context");
                if (processTools != null) {
                    builder.processToolsContext(processTools);
                }
                
                Boolean enableRuntime = (Boolean) options.get("enable_runtime_validation");
                if (enableRuntime != null) {
                    builder.enableRuntimeValidation(enableRuntime);
                }
                
                Boolean keepBackup = (Boolean) options.get("keep_backup");
                if (keepBackup != null) {
                    builder.keepBackup(keepBackup);
                }
                
                Boolean parallel = (Boolean) options.get("parallel_processing");
                if (parallel != null) {
                    builder.parallelProcessing(parallel);
                }
                
                log.debug("选项: {}", options);
            }
            
            ResourceConfig config = builder.build();
            log.info("配置加载完成: {}", path);
            
            return config;
            
        } catch (Exception e) {
            log.error("配置加载失败: {}", path, e);
            throw new IOException("配置加载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存到YAML文件
     * 
     * @param path 文件路径
     * @throws IOException 保存失败
     */
    public void saveToYaml(String path) throws IOException {
        Objects.requireNonNull(path, "path不能为null");
        
        log.info("保存配置到文件: {}", path);
        
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            
            // 版本
            data.put("version", "1.0");
            
            // 自有包前缀
            data.put("own_package_prefixes", new ArrayList<>(ownPackagePrefixes));
            
            // 包名映射
            Map<String, String> pkgMap = new LinkedHashMap<>();
            for (PackageMapping.MappingEntry entry : packageMappings.getAllMappings()) {
                pkgMap.put(entry.getOldPackage(), entry.getNewPackage());
            }
            data.put("package_mappings", pkgMap);
            
            // 类名映射
            Map<String, String> clsMap = new LinkedHashMap<>();
            for (String oldClass : classMappings.getAllOldClasses()) {
                clsMap.put(oldClass, classMappings.getNewClass(oldClass));
            }
            data.put("class_mappings", clsMap);
            
            // 处理目标
            data.put("targets", new ArrayList<>(targetDirs));
            
            // DEX路径
            data.put("dex_paths", new ArrayList<>(dexPaths));
            
            // 选项
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("process_tools_context", processToolsContext);
            options.put("enable_runtime_validation", enableRuntimeValidation);
            options.put("keep_backup", keepBackup);
            options.put("parallel_processing", parallelProcessing);
            data.put("options", options);
            
            // 写入YAML
            Yaml yaml = new Yaml();
            String yamlContent = yaml.dump(data);
            
            Files.write(Paths.get(path), yamlContent.getBytes(StandardCharsets.UTF_8));
            
            log.info("配置保存完成: {}", path);
            
        } catch (Exception e) {
            log.error("配置保存失败: {}", path, e);
            throw new IOException("配置保存失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 转换为Builder（用于修改现有配置）
     * 
     * @return Builder实例（包含当前配置的所有值）
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        
        // 复制所有映射
        for (PackageMapping.MappingEntry entry : packageMappings.getAllMappings()) {
            builder.packageMappings.addMapping(
                entry.getOldPackage(), entry.getNewPackage(), entry.getMode());
        }
        
        for (String oldClass : classMappings.getAllOldClasses()) {
            builder.classMappings.addMapping(oldClass, classMappings.getNewClass(oldClass));
        }
        
        // 复制集合
        builder.ownPackagePrefixes.addAll(this.ownPackagePrefixes);
        builder.targetDirs.addAll(this.targetDirs);
        builder.dexPaths.addAll(this.dexPaths);
        
        // 复制选项
        builder.processToolsContext = this.processToolsContext;
        builder.enableRuntimeValidation = this.enableRuntimeValidation;
        builder.keepBackup = this.keepBackup;
        builder.parallelProcessing = this.parallelProcessing;
        
        return builder;
    }
    
    @Override
    public String toString() {
        return String.format("ResourceConfig{packages=%d, classes=%d, ownPrefixes=%d}", 
                           packageMappings.size(), classMappings.size(), 
                           ownPackagePrefixes.size());
    }
    
    // Builder模式
    public static class Builder {
        private PackageMapping packageMappings = new PackageMapping();
        private ClassMapping classMappings = new ClassMapping();
        private Set<String> ownPackagePrefixes = new HashSet<>();
        private Set<String> targetDirs = new HashSet<>();
        private List<String> dexPaths = new ArrayList<>();
        
        private boolean processToolsContext = true;
        private boolean enableRuntimeValidation = false;
        private boolean keepBackup = true;
        private boolean parallelProcessing = false;
        
        public Builder addPackageMapping(String oldPkg, String newPkg) {
            packageMappings.addPrefixMapping(oldPkg, newPkg);
            return this;
        }
        
        public Builder addClassMapping(String oldClass, String newClass) {
            classMappings.addMapping(oldClass, newClass);
            return this;
        }
        
        public Builder ownPackagePrefixes(Collection<String> prefixes) {
            this.ownPackagePrefixes.addAll(prefixes);
            return this;
        }
        
        public Builder addOwnPackagePrefix(String prefix) {
            this.ownPackagePrefixes.add(prefix);
            return this;
        }
        
        public Builder targetDirs(Collection<String> dirs) {
            this.targetDirs.addAll(dirs);
            return this;
        }
        
        public Builder dexPaths(Collection<String> paths) {
            this.dexPaths.addAll(paths);
            return this;
        }
        
        public Builder addDexPath(String path) {
            this.dexPaths.add(path);
            return this;
        }
        
        public Builder processToolsContext(boolean value) {
            this.processToolsContext = value;
            return this;
        }
        
        public Builder enableRuntimeValidation(boolean value) {
            this.enableRuntimeValidation = value;
            return this;
        }
        
        public Builder keepBackup(boolean value) {
            this.keepBackup = value;
            return this;
        }
        
        public Builder parallelProcessing(boolean value) {
            this.parallelProcessing = value;
            return this;
        }
        
        public ResourceConfig build() {
            return new ResourceConfig(this);
        }
    }
}

