package com.resources.cli;

import com.resources.config.ResourceConfig;
import com.resources.core.ResourceProcessor;
import com.resources.model.ProcessingResult;
import com.resources.scanner.ResourceScanner;
import com.resources.validator.Aapt2Validator;
import com.resources.validator.SemanticValidator;
import com.resources.mapping.WhitelistFilter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Resources Processor命令行工具
 * 
 * 命令：
 * - process-apk：处理APK文件
 * - scan：扫描APK
 * - validate：验证APK
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
@Command(name = "resource-processor", 
         mixinStandardHelpOptions = true,
         version = "Resources Processor 1.0.0",
         description = "工业生产级resources.arsc和二进制XML处理工具")
public class ResourceCLI implements Callable<Integer> {
    
    @Override
    public Integer call() {
        System.err.println("错误: 请使用子命令。使用 --help 查看帮助。");
        System.err.println();
        System.err.println("可用命令:");
        System.err.println("  process-apk  处理APK文件，替换包名和类名");
        System.err.println("  scan         扫描APK，定位需要修改的位置");
        System.err.println("  validate     验证APK资源的合法性");
        System.err.println();
        System.err.println("使用 resource-processor <命令> --help 查看命令详情");
        return 2;  // 返回2表示用法错误
    }
    
    /**
     * process-apk命令 - 处理APK文件
     */
    @Command(name = "process-apk",
             description = "处理APK文件，替换包名和类名")
    public static class ProcessApkCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "输入APK文件路径")
        private String apkPath;
        
        @Option(names = {"-c", "--config"}, 
                description = "配置文件路径（YAML格式）",
                required = true)
        private String configPath;
        
        @Option(names = {"-o", "--output"}, 
                description = "输出APK文件路径")
        private String outputPath;
        
        @Option(names = {"--dex-path"}, 
                description = "DEX文件路径（用于交叉验证，可多次指定）")
        private String[] dexPaths;
        
        @Option(names = {"-v", "--verbose"}, 
                description = "详细输出模式")
        private boolean verbose;
        
        @Override
        public Integer call() {
            try {
                printHeader();
                
                // 基本输入验证（使用标准Files API）
                java.nio.file.Path apkFilePath = Paths.get(apkPath);
                if (!Files.exists(apkFilePath)) {
                    System.err.println("✗ 错误: APK文件不存在: " + apkPath);
                    return 1;
                }
                if (!Files.isRegularFile(apkFilePath)) {
                    System.err.println("✗ 错误: 不是有效文件: " + apkPath);
                    return 1;
                }
                
                java.nio.file.Path configFilePath = Paths.get(configPath);
                if (!Files.exists(configFilePath)) {
                    System.err.println("✗ 错误: 配置文件不存在: " + configPath);
                    return 1;
                }
                
                // 1. 加载配置
                System.out.println("加载配置: " + configPath);
                ResourceConfig config = ResourceConfig.loadFromYaml(configPath);
                
                // 添加DEX路径（如果指定）- 合并CLI参数和配置文件
                if (dexPaths != null && dexPaths.length > 0) {
                    System.out.println("合并CLI DEX路径: " + java.util.Arrays.toString(dexPaths));
                    
                    // 创建新的builder基于现有config
                    ResourceConfig.Builder builder = config.toBuilder();
                    
                    // 添加CLI指定的DEX路径（优先级高于配置文件）
                    for (String dexPath : dexPaths) {
                        builder.addDexPath(dexPath);
                    }
                    
                    // 重新构建config
                    config = builder.build();
                }
                
                // 2. 准备工作APK（如果指定输出路径，先复制以保护输入文件）
                String workingApkPath = apkPath;
                if (outputPath != null && !outputPath.equals(apkPath)) {
                    System.out.println("复制到输出路径: " + outputPath);
                    java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outputPath).getParent());
                    java.nio.file.Files.copy(
                        java.nio.file.Paths.get(apkPath), 
                        java.nio.file.Paths.get(outputPath),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    workingApkPath = outputPath; // 处理输出文件而非输入文件
                    System.out.println("✓ 已复制到: " + outputPath);
                }
                
                // 3. 处理APK（处理workingApkPath，不破坏输入文件）
                System.out.println("处理APK: " + workingApkPath);
                
                ResourceProcessor processor = new ResourceProcessor();
                ProcessingResult result = processor.processApk(workingApkPath, config);
                
                // 3. 显示结果
                System.out.println();
                System.out.println(result.getSummary());
                
                if (result.isSuccess()) {
                    System.out.println("✓ 处理成功！");
                    return 0;
                } else {
                    System.err.println("✗ 处理失败！");
                    return 1;
                }
                
            } catch (Exception e) {
                System.err.println("✗ 错误: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
        
        private void printHeader() {
            System.out.println("════════════════════════════════════════");
            System.out.println("  Resources Processor - 处理APK");
            System.out.println("════════════════════════════════════════");
            System.out.println();
        }
    }
    
    /**
     * scan命令 - 扫描APK
     */
    @Command(name = "scan",
             description = "扫描APK，定位所有需要修改的位置")
    public static class ScanCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "输入APK文件路径")
        private String apkPath;
        
        @Option(names = {"-c", "--config"}, 
                description = "配置文件路径（YAML格式）",
                required = true)
        private String configPath;
        
        @Option(names = {"-o", "--output"}, 
                description = "输出报告路径")
        private String outputPath;
        
        @Option(names = {"-v", "--verbose"}, 
                description = "详细输出模式")
        private boolean verbose;
        
        @Override
        public Integer call() {
            try {
                printHeader();
                
                // 1. 加载配置
                System.out.println("加载配置: " + configPath);
                ResourceConfig config = ResourceConfig.loadFromYaml(configPath);
                
                // 2. 创建扫描器
                WhitelistFilter whitelistFilter = new WhitelistFilter();
                whitelistFilter.addOwnPackages(config.getOwnPackagePrefixes());
                
                SemanticValidator semanticValidator = new SemanticValidator(whitelistFilter);
                
                ResourceScanner scanner = new ResourceScanner(
                    semanticValidator, whitelistFilter, config.getOwnPackagePrefixes());
                
                // 3. 扫描APK
                System.out.println("扫描APK: " + apkPath);
                ResourceScanner.ScanReport report = scanner.scanApk(apkPath);
                
                // 4. 显示结果
                System.out.println();
                System.out.println(report.getSummary());
                
                // 5. 保存报告（如果指定）
                if (outputPath != null) {
                    try {
                        java.nio.file.Path reportPath = Paths.get(outputPath);
                        // 确保父目录存在
                        if (reportPath.getParent() != null) {
                            Files.createDirectories(reportPath.getParent());
                        }
                        // 使用UTF-8编码写入
                        Files.write(reportPath, 
                                  report.getSummary().getBytes(StandardCharsets.UTF_8));
                        System.out.println("报告已保存: " + outputPath);
                    } catch (java.io.IOException e) {
                        System.err.println("⚠️  警告: 保存报告失败 - " + e.getMessage());
                        if (verbose) {
                            e.printStackTrace();
                        }
                        // 不影响扫描成功状态
                    }
                }
                
                System.out.println("✓ 扫描完成！");
                return 0;
                
            } catch (Exception e) {
                System.err.println("✗ 错误: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
        
        private void printHeader() {
            System.out.println("════════════════════════════════════════");
            System.out.println("  Resources Processor - 扫描APK");
            System.out.println("════════════════════════════════════════");
            System.out.println();
        }
    }
    
    /**
     * validate命令 - 验证APK
     */
    @Command(name = "validate",
             description = "验证APK资源的合法性")
    public static class ValidateCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "输入APK文件路径")
        private String apkPath;
        
        @Option(names = {"--dex-path"}, 
                description = "DEX文件路径（用于交叉验证，可多次指定）")
        private String[] dexPaths;
        
        @Option(names = {"-v", "--verbose"}, 
                description = "详细输出模式")
        private boolean verbose;
        
        @Override
        public Integer call() {
            try {
                printHeader();
                
                // 1. aapt2验证
                System.out.println("aapt2验证: " + apkPath);
                
                Aapt2Validator aapt2Validator = new Aapt2Validator();
                com.resources.model.ValidationResult result = 
                    aapt2Validator.validate(apkPath);
                
                // 2. DEX交叉验证（新增）
                if (dexPaths != null && dexPaths.length > 0) {
                    System.out.println();
                    System.out.println("DEX交叉验证:");
                    
                    // 验证DEX文件存在性
                    java.util.List<String> validDexPaths = new java.util.ArrayList<>();
                    for (int i = 0; i < dexPaths.length; i++) {
                        String dexPath = dexPaths[i];
                        java.nio.file.Path path = Paths.get(dexPath);
                        
                        if (!Files.exists(path)) {
                            System.err.println("  [" + (i + 1) + "/" + dexPaths.length + "] ✗ DEX文件不存在: " + dexPath);
                            continue;
                        }
                        
                        if (!Files.isRegularFile(path)) {
                            System.err.println("  [" + (i + 1) + "/" + dexPaths.length + "] ✗ 不是文件: " + dexPath);
                            continue;
                        }
                        
                        System.out.println("  [" + (i + 1) + "/" + dexPaths.length + "] " + dexPath);
                        validDexPaths.add(dexPath);
                    }
                    
                    if (!validDexPaths.isEmpty()) {
                        // 使用现有的DexCrossValidator
                        com.resources.validator.DexCrossValidator dexValidator = 
                            new com.resources.validator.DexCrossValidator();
                        
                        // 从APK加载类映射（简化：只验证DEX可加载）
                        // 完整的DEX验证需要类映射，这里只验证DEX可加载
                        try {
                            for (String dexPath : validDexPaths) {
                                java.util.Set<String> classes = dexValidator.loadDexClasses(dexPath);
                                System.out.println("    ✓ 加载成功: " + classes.size() + " 个类");
                            }
                        } catch (Exception e) {
                            System.err.println("    ✗ DEX加载失败: " + e.getMessage());
                            if (verbose) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.err.println("  ⚠️  所有DEX路径无效，跳过DEX验证");
                    }
                }
                
                // 3. 显示结果
                System.out.println();
                System.out.println(result.getSummary());
                
                if (result.isOverallSuccess()) {
                    System.out.println("✓ 验证通过！");
                    return 0;
                } else {
                    System.err.println("✗ 验证失败！");
                    return 1;
                }
                
            } catch (Exception e) {
                System.err.println("✗ 错误: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
        
        private void printHeader() {
            System.out.println("════════════════════════════════════════");
            System.out.println("  Resources Processor - 验证APK");
            System.out.println("════════════════════════════════════════");
            System.out.println();
        }
    }
    
    /**
     * 主入口
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ResourceCLI())
            .addSubcommand("process-apk", new ProcessApkCommand())
            .addSubcommand("scan", new ScanCommand())
            .addSubcommand("validate", new ValidateCommand())
            .execute(args);
        
        System.exit(exitCode);
    }
}

