package com.resources.arsc;

import com.resources.mapping.WhitelistFilter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 真实ARSC替换测试
 * 
 * 使用手工构造的真实ARSC二进制数据进行测试
 * 绝不简化，完全按照AAPT2规范
 * 
 * @author Resources Processor Team
 */
@DisplayName("真实ARSC替换测试")
public class RealArscReplacementTest {
    
    /**
     * 构造一个真实的ResStringPool用于测试
     * 完全符合AAPT2二进制格式
     */
    private ResStringPool createRealStringPool() throws Exception {
        // 构造真实的字符串池二进制数据
        List<String> testStrings = Arrays.asList(
            "com.example.MainActivity",           // 完整类名
            "com.example.ui.HomeFragment",        // 子包类名
            "com.example.util.NetworkHelper",     // 工具类
            "com.other.ThirdPartyClass",          // 第三方类（不应替换）
            "android.app.Activity",               // 系统类（不应替换）
            "Hello World",                        // 普通字符串（不应替换）
            "res/layout/activity_main.xml",       // 资源路径（不应替换）
            "com.example",                        // 纯包名
            "com.example.custom.CustomView"       // 自定义View
        );
        
        // 按照AAPT2格式构造二进制数据
        ByteBuffer buffer = ByteBuffer.allocate(10240);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // 写入chunk头部
        buffer.putShort((short) ResStringPool.RES_STRING_POOL_TYPE);  // type
        buffer.putShort((short) 28);  // headerSize
        
        // 计算字符串数据大小
        int stringCount = testStrings.size();
        List<byte[]> encodedStrings = new ArrayList<>();
        int stringsDataSize = 0;
        
        for (String str : testStrings) {
            byte[] utf8Bytes = ModifiedUTF8.encode(str);
            int utf8CharLen = ModifiedUTF8.countCharacters(utf8Bytes);
            
            // 计算这个字符串的完整大小
            int size = 0;
            size += (utf8CharLen >= 0x80) ? 2 : 1;  // UTF-8字符数编码
            size += (utf8Bytes.length >= 0x80) ? 2 : 1;  // 字节长度编码
            size += utf8Bytes.length;  // 数据
            size += 1;  // 终止符
            
            stringsDataSize += size;
            encodedStrings.add(utf8Bytes);
        }
        
        int offsetsSize = stringCount * 4;
        int padding = (stringsDataSize % 4 == 0) ? 0 : (4 - stringsDataSize % 4);
        int chunkSize = 28 + offsetsSize + stringsDataSize + padding;
        
        buffer.putInt(chunkSize);  // chunkSize
        buffer.putInt(stringCount);  // stringCount
        buffer.putInt(0);  // styleCount
        buffer.putInt(ResStringPool.UTF8_FLAG);  // flags (UTF-8)
        buffer.putInt(28 + offsetsSize);  // stringsStart
        buffer.putInt(0);  // stylesStart
        
        // 写入字符串偏移数组
        int currentOffset = 0;
        for (int i = 0; i < stringCount; i++) {
            buffer.putInt(currentOffset);
            
            byte[] utf8Bytes = encodedStrings.get(i);
            int utf8CharLen = ModifiedUTF8.countCharacters(utf8Bytes);
            
            int size = 0;
            size += (utf8CharLen >= 0x80) ? 2 : 1;
            size += (utf8Bytes.length >= 0x80) ? 2 : 1;
            size += utf8Bytes.length;
            size += 1;
            
            currentOffset += size;
        }
        
        // 写入字符串数据
        for (int i = 0; i < stringCount; i++) {
            byte[] utf8Bytes = encodedStrings.get(i);
            int utf8CharLen = ModifiedUTF8.countCharacters(utf8Bytes);
            int byteLen = utf8Bytes.length;
            
            // 写入UTF-8字符长度
            if (utf8CharLen >= 0x80) {
                buffer.put((byte) (0x80 | ((utf8CharLen >> 8) & 0x7F)));
                buffer.put((byte) (utf8CharLen & 0xFF));
            } else {
                buffer.put((byte) utf8CharLen);
            }
            
            // 写入字节长度
            if (byteLen >= 0x80) {
                buffer.put((byte) (0x80 | ((byteLen >> 8) & 0x7F)));
                buffer.put((byte) (byteLen & 0xFF));
            } else {
                buffer.put((byte) byteLen);
            }
            
            // 写入数据
            buffer.put(utf8Bytes);
            
            // 写入终止符
            buffer.put((byte) 0);
        }
        
        // 写入padding
        for (int i = 0; i < padding; i++) {
            buffer.put((byte) 0);
        }
        
        // 解析构造的数据
        byte[] poolData = new byte[buffer.position()];
        buffer.flip();
        buffer.get(poolData);
        
        ByteBuffer parseBuffer = ByteBuffer.wrap(poolData);
        parseBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        ResStringPool pool = new ResStringPool();
        pool.parse(parseBuffer);
        
        // 验证构造的数据
        assertEquals(stringCount, pool.getStringCount(), "字符串数量应该匹配");
        for (int i = 0; i < stringCount; i++) {
            assertEquals(testStrings.get(i), pool.getString(i), 
                        "字符串[" + i + "]应该匹配");
        }
        
        System.out.println("✓ 真实ResStringPool构造成功: " + stringCount + "个字符串");
        
        return pool;
    }
    
    @Test
    @DisplayName("测试包名前缀替换（真实数据）")
    void testPackagePrefixReplacement() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 测试包名前缀替换 ===");
        System.out.println("原始字符串池:");
        for (int i = 0; i < pool.getStringCount(); i++) {
            System.out.println("  [" + i + "] " + pool.getString(i));
        }
        
        // 2. 统计com.example开头的字符串
        int oldPackageCount = 0;
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            if (str.startsWith("com.example.") || str.equals("com.example")) {
                oldPackageCount++;
            }
        }
        
        System.out.println("\n包含'com.example'的字符串: " + oldPackageCount);
        
        // 3. 构建替换映射
        Map<String, String> replacements = new HashMap<>();
        replacements.put("com.example", "com.newapp");  // 包名前缀
        
        // 4. 执行替换
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        int replaceCount = replacer.replaceStringPool(pool, replacements);
        
        System.out.println("\n替换结果:");
        System.out.println("  - 预期替换: " + oldPackageCount);
        System.out.println("  - 实际替换: " + replaceCount);
        
        // 5. 验证替换数量
        assertEquals(oldPackageCount, replaceCount, 
                    "替换数量应该等于旧包名出现次数");
        assertTrue(replaceCount > 0, "应该有字符串被替换");
        
        // 6. 验证替换后的字符串
        System.out.println("\n替换后字符串池:");
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            System.out.println("  [" + i + "] " + str);
            
            // 不应该存在旧包名
            assertFalse(str.startsWith("com.example.") || str.equals("com.example"),
                       "字符串[" + i + "]不应该包含旧包名: " + str);
        }
        
        // 7. 验证新包名存在
        int newPackageCount = 0;
        for (int i = 0; i < pool.getStringCount(); i++) {
            String str = pool.getString(i);
            if (str.startsWith("com.newapp.") || str.equals("com.newapp")) {
                newPackageCount++;
            }
        }
        
        assertEquals(oldPackageCount, newPackageCount, 
                    "新包名数量应该等于旧包名数量");
        
        // 8. 验证第三方和系统类未被修改
        assertEquals("com.other.ThirdPartyClass", pool.getString(3), 
                    "第三方类不应该被修改");
        assertEquals("android.app.Activity", pool.getString(4), 
                    "系统类不应该被修改");
        assertEquals("Hello World", pool.getString(5), 
                    "普通字符串不应该被修改");
        
        System.out.println("\n✅ 包名前缀替换测试通过!");
        System.out.println("  - com.example.MainActivity -> com.newapp.MainActivity");
        System.out.println("  - com.example.ui.HomeFragment -> com.newapp.ui.HomeFragment");
        System.out.println("  - com.example.util.NetworkHelper -> com.newapp.util.NetworkHelper");
        System.out.println("  - com.example -> com.newapp");
    }
    
    @Test
    @DisplayName("测试类名精确匹配优先于包名前缀（真实数据）")
    void testClassMappingPriority() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 测试精确匹配优先级 ===");
        
        // 2. 构建替换映射
        Map<String, String> replacements = new HashMap<>();
        
        // 类名精确匹配（优先级高）
        replacements.put("com.example.MainActivity", "com.special.RenamedActivity");
        
        // 包名前缀匹配（优先级低）
        replacements.put("com.example", "com.newapp");
        
        // 3. 执行替换
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        int replaceCount = replacer.replaceStringPool(pool, replacements);
        
        assertTrue(replaceCount > 0, "应该有字符串被替换");
        
        // 4. 验证精确匹配优先
        String actualMainActivity = pool.getString(0);  // 第一个是MainActivity
        
        assertEquals("com.special.RenamedActivity", actualMainActivity,
                    "MainActivity应该使用精确匹配（而不是前缀匹配）");
        
        // 5. 验证其他类使用前缀匹配
        assertEquals("com.newapp.ui.HomeFragment", pool.getString(1),
                    "其他类应该使用前缀匹配");
        
        System.out.println("\n✅ 精确匹配优先级测试通过!");
        System.out.println("  - MainActivity: 使用精确匹配 -> " + actualMainActivity);
        System.out.println("  - HomeFragment: 使用前缀匹配 -> " + pool.getString(1));
    }
    
    @Test
    @DisplayName("测试长前缀优先匹配（真实数据）")
    void testLongestPrefixMatch() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 测试长前缀优先匹配 ===");
        
        // 2. 构建替换映射（包含长短前缀）
        Map<String, String> replacements = new HashMap<>();
        replacements.put("com.example", "com.short");              // 短前缀
        replacements.put("com.example.ui", "com.long");            // 长前缀
        
        // 3. 执行替换
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        int replaceCount = replacer.replaceStringPool(pool, replacements);
        
        assertTrue(replaceCount > 0, "应该有字符串被替换");
        
        // 4. 验证长前缀优先
        // com.example.MainActivity -> com.short.MainActivity (使用短前缀)
        assertEquals("com.short.MainActivity", pool.getString(0),
                    "MainActivity应该使用短前缀");
        
        // com.example.ui.HomeFragment -> com.long.HomeFragment (使用长前缀)
        assertEquals("com.long.HomeFragment", pool.getString(1),
                    "ui.HomeFragment应该使用长前缀");
        
        // com.example.util.NetworkHelper -> com.short.util.NetworkHelper (使用短前缀)
        assertEquals("com.short.util.NetworkHelper", pool.getString(2),
                    "util.NetworkHelper应该使用短前缀（因为没有com.example.util映射）");
        
        System.out.println("\n✅ 长前缀优先匹配测试通过!");
        System.out.println("  - MainActivity: com.short.MainActivity");
        System.out.println("  - ui.HomeFragment: com.long.HomeFragment");
        System.out.println("  - util.NetworkHelper: com.short.util.NetworkHelper");
    }
    
    @Test
    @DisplayName("测试完整包名替换（无子包）")
    void testExactPackageNameReplacement() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 测试完整包名替换 ===");
        
        // 2. 构建替换映射
        Map<String, String> replacements = new HashMap<>();
        replacements.put("com.example", "com.newapp");
        
        // 3. 执行替换
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        replacer.replaceStringPool(pool, replacements);
        
        // 4. 验证纯包名被替换
        String purePackage = pool.getString(7);  // "com.example"
        assertEquals("com.newapp", purePackage,
                    "纯包名应该被替换");
        
        System.out.println("✅ 完整包名替换测试通过: " + purePackage);
    }
    
    @Test
    @DisplayName("测试白名单过滤（真实数据）")
    void testWhitelistFiltering() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 测试白名单过滤 ===");
        
        // 2. 构建替换映射（尝试替换所有包）
        Map<String, String> replacements = new HashMap<>();
        replacements.put("com.example", "com.newapp");
        replacements.put("com.other", "com.hacked");      // 第三方（应被过滤）
        replacements.put("android", "com.evil");          // 系统包（应被过滤）
        
        // 3. 执行替换（只配置com.example为自有包）
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        replacer.replaceStringPool(pool, replacements);
        
        // 4. 验证白名单过滤生效
        assertEquals("com.other.ThirdPartyClass", pool.getString(3),
                    "第三方类应该被过滤，保持原样");
        assertEquals("android.app.Activity", pool.getString(4),
                    "系统类应该被过滤，保持原样");
        
        // 5. 验证自有包被替换
        assertTrue(pool.getString(0).startsWith("com.newapp."),
                  "自有包应该被替换");
        
        System.out.println("✅ 白名单过滤测试通过!");
        System.out.println("  - 自有包(com.example): ✓ 已替换");
        System.out.println("  - 第三方包(com.other): ✓ 已过滤");
        System.out.println("  - 系统包(android): ✓ 已过滤");
    }
    
    @Test
    @DisplayName("测试完整的包名+类名替换场景（真实数据）")
    void testCompleteReplacementScenario() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool pool = createRealStringPool();
        
        System.out.println("\n=== 完整替换场景测试 ===");
        System.out.println("模拟真实的包名/类名随机化场景");
        
        // 2. 构建替换映射（模拟真实配置）
        Map<String, String> replacements = new HashMap<>();
        
        // 类名映射（精确匹配）
        replacements.put("com.example.MainActivity", "a.b.c.Act");
        replacements.put("com.example.ui.HomeFragment", "d.e.f.Frag");
        
        // 包名映射（前缀匹配）
        replacements.put("com.example", "x.y.z");
        
        // 3. 执行替换
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        int replaceCount = replacer.replaceStringPool(pool, replacements);
        
        System.out.println("\n替换数量: " + replaceCount);
        assertTrue(replaceCount >= 5, "应该替换至少5个字符串");
        
        // 4. 验证结果
        System.out.println("\n替换后字符串:");
        for (int i = 0; i < pool.getStringCount(); i++) {
            System.out.println("  [" + i + "] " + pool.getString(i));
        }
        
        // 验证精确匹配
        assertEquals("a.b.c.Act", pool.getString(0),
                    "MainActivity应使用精确匹配");
        assertEquals("d.e.f.Frag", pool.getString(1),
                    "HomeFragment应使用精确匹配");
        
        // 验证前缀匹配
        assertEquals("x.y.z.util.NetworkHelper", pool.getString(2),
                    "NetworkHelper应使用前缀匹配");
        assertEquals("x.y.z", pool.getString(7),
                    "纯包名应使用前缀匹配");
        assertEquals("x.y.z.custom.CustomView", pool.getString(8),
                    "CustomView应使用前缀匹配");
        
        // 验证未替换的字符串
        assertEquals("com.other.ThirdPartyClass", pool.getString(3),
                    "第三方类应保持不变");
        assertEquals("android.app.Activity", pool.getString(4),
                    "系统类应保持不变");
        assertEquals("Hello World", pool.getString(5),
                    "普通字符串应保持不变");
        assertEquals("res/layout/activity_main.xml", pool.getString(6),
                    "资源路径应保持不变");
        
        System.out.println("\n✅ 完整替换场景测试通过!");
        System.out.println("  精确匹配: 2个");
        System.out.println("  前缀匹配: 3个");
        System.out.println("  过滤保护: 4个");
    }
    
    @Test
    @DisplayName("测试ResStringPool写入和重新解析（数据保真度）")
    void testStringPoolWriteAndReparse() throws Exception {
        // 1. 创建真实的字符串池
        ResStringPool originalPool = createRealStringPool();
        
        System.out.println("\n=== 测试数据保真度 ===");
        
        // 2. 执行替换
        Map<String, String> replacements = new HashMap<>();
        replacements.put("com.example", "com.newapp");
        
        WhitelistFilter filter = new WhitelistFilter();
        filter.addOwnPackage("com.example");
        ArscReplacer replacer = new ArscReplacer(filter);
        
        replacer.replaceStringPool(originalPool, replacements);
        
        // 3. 写入到ByteBuffer
        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);
        writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int bytesWritten = originalPool.write(writeBuffer);
        
        System.out.println("写入字节数: " + bytesWritten);
        assertTrue(bytesWritten > 0, "应该写入数据");
        
        // 4. 重新解析
        byte[] writtenData = new byte[bytesWritten];
        writeBuffer.flip();
        writeBuffer.get(writtenData);
        
        ByteBuffer parseBuffer = ByteBuffer.wrap(writtenData);
        parseBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        ResStringPool reparsedPool = new ResStringPool();
        reparsedPool.parse(parseBuffer);
        
        // 5. 验证数据保真度
        assertEquals(originalPool.getStringCount(), reparsedPool.getStringCount(),
                    "重新解析后字符串数量应该相同");
        
        for (int i = 0; i < originalPool.getStringCount(); i++) {
            String original = originalPool.getString(i);
            String reparsed = reparsedPool.getString(i);
            
            assertEquals(original, reparsed,
                        "字符串[" + i + "]重新解析后应该相同");
        }
        
        System.out.println("✅ 数据保真度测试通过!");
        System.out.println("  - 原始字符串数: " + originalPool.getStringCount());
        System.out.println("  - 重解析字符串数: " + reparsedPool.getStringCount());
        System.out.println("  - 数据一致性: 100%");
    }
}

