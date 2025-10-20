package com.resources.axml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NamespaceStack 单元测试
 * 验证命名空间栈的完整功能（参考 Apktool NamespaceStack.java）
 * 
 * @author Resources Processor Team
 */
@DisplayName("NamespaceStack 单元测试（P3实现验证）")
public class NamespaceStackTest {

    private NamespaceStack stack;

    @BeforeEach
    void setUp() {
        stack = new NamespaceStack();
    }

    @Test
    @DisplayName("测试1: 初始状态")
    void testInitialState() {
        assertEquals(0, stack.getDepth(), "初始深度应为0");
        assertEquals(0, stack.getCurrentCount(), "初始命名空间数量应为0");
        assertEquals(-1, stack.findPrefix(0), "空栈查找应返回-1");
    }

    @Test
    @DisplayName("测试2: 基本push/pop操作")
    void testBasicPushPop() {
        // 增加深度（模拟进入元素）
        stack.increaseDepth();
        assertEquals(1, stack.getDepth(), "深度应增加到1");
        
        // 压入一个命名空间
        int prefixIdx = 1;
        int uriIdx = 2;
        stack.push(prefixIdx, uriIdx);
        
        assertEquals(1, stack.getCurrentCount(), "当前深度应有1个命名空间");
        assertEquals(prefixIdx, stack.findPrefix(uriIdx), "应能找到对应的prefix");
        
        // 减少深度（模拟离开元素）
        stack.decreaseDepth();
        assertEquals(0, stack.getDepth(), "深度应减少到0");
        assertEquals(-1, stack.findPrefix(uriIdx), "深度减少后应找不到命名空间");
    }

    @Test
    @DisplayName("测试3: 多层嵌套命名空间")
    void testNestedNamespaces() {
        // 第1层：android命名空间
        stack.increaseDepth();
        stack.push(10, 11);  // prefix=10 ("android"), uri=11 ("http://schemas.android.com/apk/res/android")
        assertEquals(1, stack.getDepth(), "第1层深度");
        assertEquals(1, stack.getCurrentCount(), "第1层有1个命名空间");
        
        // 第2层：添加app命名空间
        stack.increaseDepth();
        stack.push(20, 21);  // prefix=20 ("app"), uri=21 ("http://schemas.android.com/apk/res-auto")
        assertEquals(2, stack.getDepth(), "第2层深度");
        assertEquals(1, stack.getCurrentCount(), "第2层有1个命名空间");
        
        // 第3层：再添加tools命名空间
        stack.increaseDepth();
        stack.push(30, 31);  // prefix=30 ("tools"), uri=31 ("http://schemas.android.com/tools")
        assertEquals(3, stack.getDepth(), "第3层深度");
        
        // 验证所有命名空间都可查找
        assertEquals(10, stack.findPrefix(11), "第1层命名空间可查找");
        assertEquals(20, stack.findPrefix(21), "第2层命名空间可查找");
        assertEquals(30, stack.findPrefix(31), "第3层命名空间可查找");
        
        // 离开第3层
        stack.decreaseDepth();
        assertEquals(2, stack.getDepth(), "离开第3层后深度为2");
        assertEquals(-1, stack.findPrefix(31), "第3层命名空间已移除");
        assertEquals(20, stack.findPrefix(21), "第2层命名空间仍存在");
        
        // 离开第2层
        stack.decreaseDepth();
        assertEquals(1, stack.getDepth(), "离开第2层后深度为1");
        assertEquals(-1, stack.findPrefix(21), "第2层命名空间已移除");
        assertEquals(10, stack.findPrefix(11), "第1层命名空间仍存在");
    }

    @Test
    @DisplayName("测试4: 同一URI在不同深度的不同prefix")
    void testSameUriDifferentPrefix() {
        // 这是命名空间栈的关键功能！
        int uriIdx = 100;
        
        // 第1层：uri=100, prefix=1
        stack.increaseDepth();
        stack.push(1, uriIdx);
        assertEquals(1, stack.findPrefix(uriIdx), "第1层使用prefix=1");
        
        // 第2层：同一URI使用不同prefix
        stack.increaseDepth();
        stack.push(2, uriIdx);
        
        // 栈顶优先：应返回最近的prefix
        assertEquals(2, stack.findPrefix(uriIdx), "栈顶优先：应返回第2层的prefix=2");
        
        // 离开第2层
        stack.decreaseDepth();
        assertEquals(1, stack.findPrefix(uriIdx), "离开第2层后应返回第1层的prefix=1");
    }

    @Test
    @DisplayName("测试5: 同一深度多个命名空间")
    void testMultipleNamespacesAtSameDepth() {
        stack.increaseDepth();
        
        // 同一深度压入3个命名空间
        stack.push(10, 11);  // android
        stack.push(20, 21);  // app
        stack.push(30, 31);  // tools
        
        assertEquals(3, stack.getCurrentCount(), "当前深度应有3个命名空间");
        
        // 验证都能查找到
        assertEquals(10, stack.findPrefix(11), "android命名空间");
        assertEquals(20, stack.findPrefix(21), "app命名空间");
        assertEquals(30, stack.findPrefix(31), "tools命名空间");
        
        // 离开该深度
        stack.decreaseDepth();
        assertEquals(0, stack.getCurrentCount(), "离开后当前深度无命名空间");
        assertEquals(-1, stack.findPrefix(11), "所有命名空间已移除");
        assertEquals(-1, stack.findPrefix(21), "所有命名空间已移除");
        assertEquals(-1, stack.findPrefix(31), "所有命名空间已移除");
    }

    @Test
    @DisplayName("测试6: getPrefix/getUri通过位置访问")
    void testGetByPosition() {
        stack.increaseDepth();
        stack.push(10, 11);
        stack.push(20, 21);
        
        // 第0个命名空间
        assertEquals(10, stack.getPrefix(0), "第0个命名空间的prefix");
        assertEquals(11, stack.getUri(0), "第0个命名空间的uri");
        
        // 第1个命名空间
        assertEquals(20, stack.getPrefix(1), "第1个命名空间的prefix");
        assertEquals(21, stack.getUri(1), "第1个命名空间的uri");
        
        // 越界访问
        assertEquals(-1, stack.getPrefix(2), "越界访问应返回-1");
        assertEquals(-1, stack.getUri(2), "越界访问应返回-1");
        assertEquals(-1, stack.getPrefix(-1), "负索引应返回-1");
    }

    @Test
    @DisplayName("测试7: 累计计数getAccumulatedCount")
    void testAccumulatedCount() {
        // 第1层：2个命名空间
        stack.increaseDepth();
        stack.push(10, 11);
        stack.push(12, 13);
        assertEquals(2, stack.getAccumulatedCount(1), "第1层累计2个");
        
        // 第2层：1个命名空间
        stack.increaseDepth();
        stack.push(20, 21);
        assertEquals(2, stack.getAccumulatedCount(1), "到第1层累计2个");
        assertEquals(3, stack.getAccumulatedCount(2), "到第2层累计3个");
        
        // 第3层：3个命名空间
        stack.increaseDepth();
        stack.push(30, 31);
        stack.push(32, 33);
        stack.push(34, 35);
        assertEquals(2, stack.getAccumulatedCount(1), "到第1层累计2个");
        assertEquals(3, stack.getAccumulatedCount(2), "到第2层累计3个");
        assertEquals(6, stack.getAccumulatedCount(3), "到第3层累计6个");
    }

    @Test
    @DisplayName("测试8: reset重置状态")
    void testReset() {
        // 构建复杂状态
        stack.increaseDepth();
        stack.push(10, 11);
        stack.increaseDepth();
        stack.push(20, 21);
        
        assertEquals(2, stack.getDepth(), "reset前深度为2");
        
        // 重置
        stack.reset();
        
        assertEquals(0, stack.getDepth(), "reset后深度为0");
        assertEquals(0, stack.getCurrentCount(), "reset后命名空间数为0");
        assertEquals(-1, stack.findPrefix(11), "reset后查找失败");
    }

    @Test
    @DisplayName("测试9: 大量命名空间（自动扩容）")
    void testLargeNumberOfNamespaces() {
        stack.increaseDepth();
        
        // 压入100个命名空间（超过初始容量32）
        for (int i = 0; i < 100; i++) {
            stack.push(i * 2, i * 2 + 1);
        }
        
        assertEquals(100, stack.getCurrentCount(), "应有100个命名空间");
        
        // 验证都能查找到
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 2, stack.findPrefix(i * 2 + 1), 
                "第" + i + "个命名空间应能查找");
        }
    }

    @Test
    @DisplayName("测试10: 深度嵌套（自动扩容）")
    void testDeepNesting() {
        // 嵌套20层（超过初始容量16）
        for (int depth = 0; depth < 20; depth++) {
            stack.increaseDepth();
            stack.push(depth, depth + 1000);
        }
        
        assertEquals(20, stack.getDepth(), "深度应为20");
        
        // 从深到浅逐层退出
        for (int depth = 20; depth > 0; depth--) {
            assertEquals(depth, stack.getDepth(), "深度验证");
            stack.decreaseDepth();
        }
        
        assertEquals(0, stack.getDepth(), "最终深度应为0");
    }

    @Test
    @DisplayName("测试11: 边界条件 - 空栈decreaseDepth")
    void testDecreaseDepthOnEmptyStack() {
        assertEquals(0, stack.getDepth(), "初始深度为0");
        
        // 在空栈上调用decreaseDepth不应崩溃
        assertDoesNotThrow(() -> stack.decreaseDepth(), "空栈decreaseDepth不应抛出异常");
        assertEquals(0, stack.getDepth(), "深度应保持为0");
    }

    @Test
    @DisplayName("测试12: 实际XML场景模拟")
    void testRealXmlScenario() {
        // 模拟实际XML解析场景：
        // <root xmlns:android="..." xmlns:app="...">
        //   <element1>
        //     <element2 xmlns:tools="...">
        //     </element2>
        //   </element1>
        // </root>
        
        // 解析<root>
        stack.increaseDepth();
        stack.push(1, 10);  // android命名空间
        stack.push(2, 20);  // app命名空间
        assertEquals(1, stack.getDepth(), "root深度=1");
        assertEquals(2, stack.getCurrentCount(), "root有2个命名空间");
        
        // 解析<element1>
        stack.increaseDepth();
        assertEquals(2, stack.getDepth(), "element1深度=2");
        assertEquals(0, stack.getCurrentCount(), "element1无新命名空间");
        // 但父级命名空间仍可访问
        assertEquals(1, stack.findPrefix(10), "android命名空间仍可访问");
        assertEquals(2, stack.findPrefix(20), "app命名空间仍可访问");
        
        // 解析<element2>（添加tools命名空间）
        stack.increaseDepth();
        stack.push(3, 30);  // tools命名空间
        assertEquals(3, stack.getDepth(), "element2深度=3");
        assertEquals(1, stack.getCurrentCount(), "element2有1个新命名空间");
        assertEquals(3, stack.findPrefix(30), "tools命名空间可访问");
        
        // 离开</element2>
        stack.decreaseDepth();
        assertEquals(2, stack.getDepth(), "回到element1深度");
        assertEquals(-1, stack.findPrefix(30), "tools命名空间已移除");
        assertEquals(1, stack.findPrefix(10), "android命名空间仍可访问");
        
        // 离开</element1>
        stack.decreaseDepth();
        assertEquals(1, stack.getDepth(), "回到root深度");
        
        // 离开</root>
        stack.decreaseDepth();
        assertEquals(0, stack.getDepth(), "回到文档根");
        assertEquals(-1, stack.findPrefix(10), "所有命名空间已移除");
        assertEquals(-1, stack.findPrefix(20), "所有命名空间已移除");
    }

    @Test
    @DisplayName("测试13: 命名空间覆盖（子元素重定义）")
    void testNamespaceOverride() {
        int androidUriIdx = 100;
        
        // 第1层：定义android命名空间，prefix=1
        stack.increaseDepth();
        stack.push(1, androidUriIdx);
        assertEquals(1, stack.findPrefix(androidUriIdx), "第1层prefix=1");
        
        // 第2层：重定义同一URI，使用不同prefix=2
        stack.increaseDepth();
        stack.push(2, androidUriIdx);
        
        // 关键：栈顶优先，应返回第2层的prefix
        assertEquals(2, stack.findPrefix(androidUriIdx), "栈顶优先：第2层prefix=2");
        
        // 离开第2层
        stack.decreaseDepth();
        
        // 应恢复到第1层的prefix
        assertEquals(1, stack.findPrefix(androidUriIdx), "恢复第1层prefix=1");
    }

    @Test
    @DisplayName("测试14: 空深度push不应生效")
    void testPushAtZeroDepth() {
        assertEquals(0, stack.getDepth(), "初始深度为0");
        
        // 在深度0时push（无效操作）
        stack.push(10, 11);
        
        // 验证没有添加
        assertEquals(-1, stack.findPrefix(11), "深度0时push无效");
    }

    @Test
    @DisplayName("测试15: toString调试输出")
    void testToString() {
        stack.increaseDepth();
        stack.push(1, 2);
        stack.push(3, 4);
        
        String str = stack.toString();
        assertNotNull(str, "toString不应返回null");
        assertTrue(str.contains("depth=1"), "应包含深度信息");
        assertTrue(str.contains("dataLength=4"), "应包含数据长度（2个命名空间=4个int）");
        assertTrue(str.contains("prefix=1"), "应包含第1个prefix");
        assertTrue(str.contains("uri=2"), "应包含第1个uri");
    }
}

