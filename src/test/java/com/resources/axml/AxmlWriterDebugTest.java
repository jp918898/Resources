package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AxmlWriter调试测试
 * 验证嵌套结构是否正确写入
 */
@DisplayName("AxmlWriter调试测试")
public class AxmlWriterDebugTest {

    @Test
    @DisplayName("测试嵌套结构写入")
    void testNestedStructure() throws IOException {
        // 手动构建嵌套结构：
        // <root>
        //   <child1>
        //     <grandchild/>
        //   </child1>
        //   <child2/>
        // </root>
        
        AxmlWriter writer = new AxmlWriter();
        
        // 创建root节点
        NodeVisitor root = writer.child(null, "root");
        assertNotNull(root, "root节点不应为null");
        
        // 创建child1
        NodeVisitor child1 = root.child(null, "child1");
        assertNotNull(child1, "child1节点不应为null");
        
        // 创建grandchild
        NodeVisitor grandchild = child1.child(null, "grandchild");
        assertNotNull(grandchild, "grandchild节点不应为null");
        grandchild.end();
        
        child1.end();
        
        // 创建child2
        NodeVisitor child2 = root.child(null, "child2");
        assertNotNull(child2, "child2节点不应为null");
        child2.end();
        
        root.end();
        writer.end();
        
        // 写入AXML
        byte[] axmlData = writer.toByteArray();
        assertNotNull(axmlData, "AXML数据不应为null");
        assertTrue(axmlData.length > 0, "AXML数据不应为空");
        
        System.out.println("手动构建AXML大小: " + axmlData.length + " 字节");
        
        // 解析重建的AXML
        AxmlParser parser = new AxmlParser(axmlData);
        
        int startTagCount = 0;
        int endTagCount = 0;
        
        int event;
        while ((event = parser.next()) != AxmlParser.END_FILE) {
            if (event == AxmlParser.START_TAG) {
                startTagCount++;
                String tagName = parser.getName();
                System.out.println("解析到标签: <" + tagName + ">");
            } else if (event == AxmlParser.END_TAG) {
                endTagCount++;
            }
        }
        
        System.out.println("解析结果:");
        System.out.println("  - START_TAG: " + startTagCount);
        System.out.println("  - END_TAG: " + endTagCount);
        
        // 验证
        assertEquals(4, startTagCount, "应有4个START_TAG（root, child1, grandchild, child2）");
        assertEquals(4, endTagCount, "应有4个END_TAG");
    }
}

