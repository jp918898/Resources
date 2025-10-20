package com.resources.axml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AxmlWriter结构测试
 * 调查为什么往返时标签丢失
 */
@DisplayName("AxmlWriter结构测试")
public class AxmlWriterStructureTest {

    @Test
    @DisplayName("测试AxmlReader收集节点的逻辑")
    void testAxmlReaderCollection() throws IOException {
        Path apkPath = Path.of("input/Dragonfly.apk");
        if (!Files.exists(apkPath)) {
            System.out.println("跳过测试");
            return;
        }

        try (ZipFile zipFile = new ZipFile(apkPath.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            byte[] manifestData = zipFile.getInputStream(manifestEntry).readAllBytes();
            
            // 创建自定义Visitor来跟踪收集过程
            TrackingVisitor tracker = new TrackingVisitor();
            
            AxmlReader reader = new AxmlReader(manifestData);
            reader.accept(tracker);
            
            System.out.println("AxmlReader访问统计:");
            System.out.println("  - 顶级child()调用: " + tracker.childCallCount);
            System.out.println("  - ns()调用次数: " + tracker.nsCallCount);
            System.out.println("\n递归统计:");
            System.out.println("  - 总child()调用: " + TrackingNodeVisitor.totalChildCalls);
            System.out.println("  - 总end()调用: " + TrackingNodeVisitor.totalEndCalls);
            System.out.println("  - 最大深度: " + TrackingNodeVisitor.maxDepth);
            
            assertTrue(tracker.childCallCount > 0, "应有顶级child调用");
            assertEquals(271, TrackingNodeVisitor.totalChildCalls + tracker.childCallCount, 
                "总child调用应等于原始标签数");
        }
    }
    
    /**
     * 跟踪访问器，用于统计方法调用
     */
    static class TrackingVisitor extends AxmlVisitor {
        int childCallCount = 0;
        int attrCallCount = 0;
        int textCallCount = 0;
        int nsCallCount = 0;
        int endCallCount = 0;
        
        @Override
        public NodeVisitor child(String ns, String name) {
            childCallCount++;
            return new TrackingNodeVisitor();
        }
        
        @Override
        public void ns(String prefix, String uri, int ln) {
            nsCallCount++;
            super.ns(prefix, uri, ln);
        }
    }
    
    static class TrackingNodeVisitor extends NodeVisitor {
        static int totalChildCalls = 0;
        static int totalEndCalls = 0;
        static int maxDepth = 0;
        int currentDepth;
        
        TrackingNodeVisitor() {
            this(1);
        }
        
        TrackingNodeVisitor(int depth) {
            this.currentDepth = depth;
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        
        @Override
        public NodeVisitor child(String ns, String name) {
            totalChildCalls++;
            // 递归创建子访问器（增加深度）
            return new TrackingNodeVisitor(currentDepth + 1);
        }
        
        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            // 记录属性访问
        }
        
        @Override
        public void text(int ln, String text) {
            // 记录文本访问
        }
        
        @Override
        public void end() {
            totalEndCalls++;
        }
    }
}

