package com.resources.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

/**
 * 回滚执行器
 * 
 * 功能：
 * - 完整回滚（恢复整个APK）
 * - 部分回滚（恢复指定文件）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class RollbackExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(RollbackExecutor.class);
    
    /**
     * 完整回滚
     * 
     * @param snapshotPath 快照路径
     * @param targetPath 目标路径
     * @throws IOException 回滚失败
     */
    public void rollback(String snapshotPath, String targetPath) throws IOException {
        Objects.requireNonNull(snapshotPath, "snapshotPath不能为null");
        Objects.requireNonNull(targetPath, "targetPath不能为null");
        
        log.info("执行完整回滚: {} -> {}", snapshotPath, targetPath);
        
        Path snapshotFilePath = Paths.get(snapshotPath);
        Path targetFilePath = Paths.get(targetPath);
        
        if (!Files.exists(snapshotFilePath)) {
            throw new IOException("快照文件不存在: " + snapshotPath);
        }
        
        try {
            // 复制快照到目标位置
            Files.copy(snapshotFilePath, targetFilePath, 
                      StandardCopyOption.REPLACE_EXISTING);
            
            log.info("回滚完成: {}", targetPath);
            
        } catch (IOException e) {
            log.error("回滚失败: {} -> {}", snapshotPath, targetPath, e);
            throw e;
        }
    }
    
    /**
     * 部分回滚（已移除 - 当前实现仅支持完整回滚）
     * 
     * 设计说明：
     * 部分回滚需要：
     * 1. 解压快照APK
     * 2. 提取指定文件
     * 3. 重新打包
     * 
     * 由于：
     * - 实现复杂度高
     * - 当前事务机制使用完整回滚已足够
     * - 性能影响可接受（快照文件<500MB）
     * 
     * 因此不实现部分回滚功能
     * 
     * 如需部分回滚，请使用 rollback() 进行完整回滚
     */
    // public void rollbackPartial() - 已移除
    
    /**
     * 验证回滚成功
     * 
     * @param originalPath 原始文件路径
     * @param restoredPath 恢复后的文件路径
     * @return true=验证通过
     */
    public boolean verifyRollback(String originalPath, String restoredPath) {
        try {
            Path original = Paths.get(originalPath);
            Path restored = Paths.get(restoredPath);
            
            if (!Files.exists(original) || !Files.exists(restored)) {
                log.error("文件不存在");
                return false;
            }
            
            long originalSize = Files.size(original);
            long restoredSize = Files.size(restored);
            
            if (originalSize != restoredSize) {
                log.error("文件大小不匹配: {} != {}", originalSize, restoredSize);
                return false;
            }
            
            log.debug("回滚验证通过: {} 字节", originalSize);
            return true;
            
        } catch (Exception e) {
            log.error("回滚验证失败", e);
            return false;
        }
    }
}

