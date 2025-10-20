package com.resources.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ResourceConfig单元测试 - 验证空配置处理修复
 */
public class ResourceConfigTest {
    
    private static final String TEST_DIR = "temp";
    
    @BeforeAll
    static void setupTestDir() throws IOException {
        Files.createDirectories(Paths.get(TEST_DIR));
    }
    
    @Test
    @DisplayName("测试加载空YAML文件 - 验证null检查修复")
    void testLoadEmptyYaml() throws Exception {
        // 1. 创建空YAML文件
        Path emptyYaml = Paths.get(TEST_DIR, "empty.yaml");
        Files.write(emptyYaml, "".getBytes(StandardCharsets.UTF_8));
        
        System.out.println("测试空YAML配置:");
        System.out.println("  - 文件: " + emptyYaml);
        System.out.println("  - 内容: <空>");
        
        // 2. 加载：不应该抛出NPE
        ResourceConfig config = assertDoesNotThrow(() -> 
            ResourceConfig.loadFromYaml(emptyYaml.toString()),
            "加载空YAML文件不应该抛出NullPointerException");
        
        // 3. 验证默认值
        assertNotNull(config, "应该返回有效的ResourceConfig对象");
        assertTrue(config.getOwnPackagePrefixes().isEmpty(), 
            "自有包前缀应该为空");
        assertTrue(config.getDexPaths().isEmpty(), 
            "DEX路径应该为空");
        assertEquals(0, config.getClassMappings().size(), 
            "类映射应该为空");
        assertEquals(0, config.getPackageMappings().size(), 
            "包映射应该为空");
        
        System.out.println("  - ✓ 返回有效配置对象");
        System.out.println("  - ✓ 所有字段使用默认值");
        
        // 清理
        Files.deleteIfExists(emptyYaml);
        
        System.out.println("✓ 空YAML文件测试通过");
    }
    
    @Test
    @DisplayName("测试加载仅注释的YAML文件")
    void testLoadCommentOnlyYaml() throws Exception {
        // 1. 创建仅注释的YAML
        Path commentYaml = Paths.get(TEST_DIR, "comment.yaml");
        String content = "# This is a comment\n" +
                        "# Another comment\n" +
                        "# More comments\n";
        Files.write(commentYaml, content.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("测试仅注释YAML配置:");
        System.out.println("  - 文件: " + commentYaml);
        System.out.println("  - 内容: 3行注释");
        
        // 2. 加载：不应该抛出NPE
        ResourceConfig config = assertDoesNotThrow(() -> 
            ResourceConfig.loadFromYaml(commentYaml.toString()),
            "加载仅注释YAML不应该抛出NullPointerException");
        
        // 3. 验证
        assertNotNull(config, "应该返回有效的ResourceConfig对象");
        assertTrue(config.getOwnPackagePrefixes().isEmpty());
        
        System.out.println("  - ✓ 返回有效配置对象");
        
        // 清理
        Files.deleteIfExists(commentYaml);
        
        System.out.println("✓ 仅注释YAML文件测试通过");
    }
    
    @Test
    @DisplayName("测试加载空对象YAML文件")
    void testLoadEmptyObjectYaml() throws Exception {
        // 空对象 {} 不会返回null，应该正常工作
        Path emptyObjYaml = Paths.get(TEST_DIR, "empty_obj.yaml");
        Files.write(emptyObjYaml, "{}".getBytes(StandardCharsets.UTF_8));
        
        System.out.println("测试空对象YAML配置:");
        System.out.println("  - 文件: " + emptyObjYaml);
        System.out.println("  - 内容: {}");
        
        ResourceConfig config = assertDoesNotThrow(() -> 
            ResourceConfig.loadFromYaml(emptyObjYaml.toString()));
        
        assertNotNull(config);
        assertTrue(config.getOwnPackagePrefixes().isEmpty());
        
        System.out.println("  - ✓ 正常处理");
        
        // 清理
        Files.deleteIfExists(emptyObjYaml);
        
        System.out.println("✓ 空对象YAML文件测试通过");
    }
    
    @Test
    @DisplayName("测试加载最小有效配置")
    void testLoadMinimalValidConfig() throws Exception {
        Path minimalYaml = Paths.get(TEST_DIR, "minimal.yaml");
        String content = "version: '1.0'\n" +
                        "own_package_prefixes:\n" +
                        "  - com.example\n";
        Files.write(minimalYaml, content.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("测试最小有效配置:");
        System.out.println("  - 文件: " + minimalYaml);
        
        ResourceConfig config = ResourceConfig.loadFromYaml(minimalYaml.toString());
        
        assertNotNull(config);
        assertEquals(1, config.getOwnPackagePrefixes().size());
        assertTrue(config.getOwnPackagePrefixes().contains("com.example"));
        
        System.out.println("  - ✓ 自有包前缀: " + config.getOwnPackagePrefixes());
        
        // 清理
        Files.deleteIfExists(minimalYaml);
        
        System.out.println("✓ 最小有效配置测试通过");
    }
    
    @Test
    @DisplayName("测试加载完整配置")
    void testLoadFullConfig() throws Exception {
        Path fullYaml = Paths.get(TEST_DIR, "full.yaml");
        String content = "version: '1.0'\n" +
                        "own_package_prefixes:\n" +
                        "  - com.example\n" +
                        "  - com.myapp\n" +
                        "package_mappings:\n" +
                        "  com.example: com.newapp\n" +
                        "class_mappings:\n" +
                        "  com.example.MainActivity: com.newapp.MainActivity\n" +
                        "targets:\n" +
                        "  - res/layout\n" +
                        "dex_paths:\n" +
                        "  - input/dex/classes.dex\n" +
                        "options:\n" +
                        "  process_tools_context: true\n" +
                        "  enable_runtime_validation: false\n" +
                        "  keep_backup: true\n" +
                        "  parallel_processing: false\n";
        Files.write(fullYaml, content.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("测试完整配置:");
        System.out.println("  - 文件: " + fullYaml);
        
        ResourceConfig config = ResourceConfig.loadFromYaml(fullYaml.toString());
        
        assertNotNull(config);
        assertEquals(2, config.getOwnPackagePrefixes().size());
        assertEquals(1, config.getPackageMappings().size());
        assertEquals(1, config.getClassMappings().size());
        assertEquals(1, config.getTargetDirs().size());
        assertEquals(1, config.getDexPaths().size());
        assertTrue(config.isProcessToolsContext());
        assertFalse(config.isEnableRuntimeValidation());
        assertTrue(config.isKeepBackup());
        assertFalse(config.isParallelProcessing());
        
        System.out.println("  - ✓ 所有字段正确解析");
        System.out.println("  - 自有包: " + config.getOwnPackagePrefixes());
        System.out.println("  - 包映射: " + config.getPackageMappings().size() + " 条");
        System.out.println("  - 类映射: " + config.getClassMappings().size() + " 条");
        
        // 清理
        Files.deleteIfExists(fullYaml);
        
        System.out.println("✓ 完整配置测试通过");
    }
    
    @Test
    @DisplayName("测试配置保存和重新加载")
    void testSaveAndReload() throws Exception {
        String testPath = TEST_DIR + "/save_reload.yaml";
        
        // 1. 创建配置
        ResourceConfig.Builder builder = new ResourceConfig.Builder();
        builder.addOwnPackagePrefix("com.test");
        builder.addPackageMapping("com.test", "com.newtest");
        builder.addClassMapping("com.test.MyClass", "com.newtest.MyClass");
        builder.addDexPath("test.dex");
        builder.processToolsContext(true);
        builder.keepBackup(false);
        
        ResourceConfig original = builder.build();
        
        System.out.println("测试配置保存和重新加载:");
        
        // 2. 保存
        original.saveToYaml(testPath);
        assertTrue(Files.exists(Paths.get(testPath)), "配置文件应该存在");
        System.out.println("  - ✓ 保存成功");
        
        // 3. 重新加载
        ResourceConfig reloaded = ResourceConfig.loadFromYaml(testPath);
        
        // 4. 验证一致性
        assertEquals(original.getOwnPackagePrefixes(), reloaded.getOwnPackagePrefixes());
        assertEquals(original.getPackageMappings().size(), reloaded.getPackageMappings().size());
        assertEquals(original.getClassMappings().size(), reloaded.getClassMappings().size());
        assertEquals(original.getDexPaths(), reloaded.getDexPaths());
        assertEquals(original.isProcessToolsContext(), reloaded.isProcessToolsContext());
        assertEquals(original.isKeepBackup(), reloaded.isKeepBackup());
        
        System.out.println("  - ✓ 重新加载成功");
        System.out.println("  - ✓ 所有字段一致");
        
        // 清理
        Files.deleteIfExists(Paths.get(testPath));
        
        System.out.println("✓ 配置保存和重新加载测试通过");
    }
    
    @Test
    @DisplayName("测试null参数处理")
    void testNullParameters() {
        // loadFromYaml不应该接受null路径
        assertThrows(NullPointerException.class, () -> 
            ResourceConfig.loadFromYaml(null));
        
        System.out.println("✓ null参数正确抛出异常");
    }
    
    @Test
    @DisplayName("测试不存在的文件")
    void testNonExistentFile() {
        String nonExistent = "temp/does_not_exist_12345.yaml";
        
        // 应该抛出IOException而不是NPE
        assertThrows(IOException.class, () -> 
            ResourceConfig.loadFromYaml(nonExistent));
        
        System.out.println("✓ 不存在的文件正确抛出IOException");
    }
}

