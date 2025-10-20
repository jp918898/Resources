package com.resources.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 快照管理器
 * 
 * 功能：
 * - 创建完整快照
 * - 恢复快照
 * - 删除快照
 * - 清理过期快照
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class SnapshotManager {
    
    private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);
    
    private final String snapshotDir;
    
    public SnapshotManager() {
        this("temp/snapshots");
    }
    
    public SnapshotManager(String snapshotDir) {
        this.snapshotDir = Objects.requireNonNull(snapshotDir, "snapshotDir不能为null");
        
        // 确保快照目录存在
        try {
            Files.createDirectories(Paths.get(snapshotDir));
            log.debug("快照目录: {}", new File(snapshotDir).getAbsolutePath());
        } catch (IOException e) {
            log.error("创建快照目录失败: {}", snapshotDir, e);
        }
    }
    
    /**
     * 创建快照
     * 
     * @param apkPath APK文件路径
     * @param transactionId 事务ID
     * @return 快照路径
     * @throws IOException 创建失败
     */
    public String createSnapshot(String apkPath, String transactionId) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        Objects.requireNonNull(transactionId, "transactionId不能为null");
        
        log.info("创建快照: apk={}, txId={}", apkPath, transactionId);
        
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            throw new FileNotFoundException("APK文件不存在: " + apkPath);
        }
        
        // 检查磁盘空间（需要至少3倍APK大小：快照+临时+原文件）
        long apkSize = apkFile.length();
        long requiredSpace = apkSize * 3;
        File snapshotDirFile = new File(snapshotDir);
        long freeSpace = snapshotDirFile.getFreeSpace();
        
        if (freeSpace < requiredSpace) {
            throw new IOException(String.format(
                "磁盘空间不足: 需要%d MB (APK×3), 可用%d MB",
                requiredSpace / 1024 / 1024, 
                freeSpace / 1024 / 1024));
        }
        
        log.debug("磁盘空间检查通过: 需要{}MB, 可用{}MB", 
                 requiredSpace / 1024 / 1024, freeSpace / 1024 / 1024);
        
        // 快照文件路径
        String snapshotPath = snapshotDir + "/" + transactionId + ".snapshot";
        Path snapshotFilePath = Paths.get(snapshotPath);
        
        try {
            // 复制APK到快照
            Files.copy(apkFile.toPath(), snapshotFilePath, 
                      StandardCopyOption.REPLACE_EXISTING);
            
            long snapshotSize = Files.size(snapshotFilePath);
            
            log.info("快照创建成功: {} ({} 字节)", snapshotPath, snapshotSize);
            
            return snapshotPath;
            
        } catch (IOException e) {
            log.error("快照创建失败: {}", snapshotPath, e);
            throw e;
        }
    }
    
    /**
     * 恢复快照
     * 
     * @param transactionId 事务ID
     * @param targetPath 恢复目标路径
     * @throws IOException 恢复失败
     */
    public void restoreSnapshot(String transactionId, String targetPath) throws IOException {
        Objects.requireNonNull(transactionId, "transactionId不能为null");
        Objects.requireNonNull(targetPath, "targetPath不能为null");
        
        log.info("恢复快照: txId={}, target={}", transactionId, targetPath);
        
        String snapshotPath = snapshotDir + "/" + transactionId + ".snapshot";
        Path snapshotFilePath = Paths.get(snapshotPath);
        
        if (!Files.exists(snapshotFilePath)) {
            throw new FileNotFoundException("快照不存在: " + snapshotPath);
        }
        
        try {
            // 恢复快照到目标路径
            Files.copy(snapshotFilePath, Paths.get(targetPath),
                      StandardCopyOption.REPLACE_EXISTING);
            
            log.info("快照恢复成功: {} -> {}", snapshotPath, targetPath);
            
        } catch (IOException e) {
            log.error("快照恢复失败: {}", snapshotPath, e);
            throw e;
        }
    }
    
    /**
     * 删除快照
     * 
     * @param transactionId 事务ID
     * @return true=删除成功，false=快照不存在
     */
    public boolean deleteSnapshot(String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId不能为null");
        
        String snapshotPath = snapshotDir + "/" + transactionId + ".snapshot";
        Path snapshotFilePath = Paths.get(snapshotPath);
        
        try {
            if (Files.exists(snapshotFilePath)) {
                Files.delete(snapshotFilePath);
                log.info("快照已删除: {}", snapshotPath);
                return true;
            } else {
                log.debug("快照不存在，无需删除: {}", snapshotPath);
                return false;
            }
            
        } catch (IOException e) {
            log.error("删除快照失败: {}", snapshotPath, e);
            return false;
        }
    }
    
    /**
     * 检查快照是否存在
     * 
     * @param transactionId 事务ID
     * @return true=存在
     */
    public boolean snapshotExists(String transactionId) {
        String snapshotPath = snapshotDir + "/" + transactionId + ".snapshot";
        return Files.exists(Paths.get(snapshotPath));
    }
    
    /**
     * 获取快照大小
     * 
     * @param transactionId 事务ID
     * @return 快照大小（字节），如果不存在返回-1
     */
    public long getSnapshotSize(String transactionId) {
        String snapshotPath = snapshotDir + "/" + transactionId + ".snapshot";
        Path snapshotFilePath = Paths.get(snapshotPath);
        
        try {
            if (Files.exists(snapshotFilePath)) {
                return Files.size(snapshotFilePath);
            }
        } catch (IOException e) {
            log.warn("获取快照大小失败: {}", snapshotPath, e);
        }
        
        return -1;
    }
    
    /**
     * 清理过期快照
     * 
     * @param maxAgeDays 最大保留天数
     * @return 清理的快照数量
     */
    public int cleanupOldSnapshots(int maxAgeDays) {
        log.info("清理过期快照: 保留最近 {} 天", maxAgeDays);
        
        int cleanedCount = 0;
        
        try {
            File dir = new File(snapshotDir);
            if (!dir.exists() || !dir.isDirectory()) {
                return 0;
            }
            
            File[] files = dir.listFiles((d, name) -> name.endsWith(".snapshot"));
            if (files == null || files.length == 0) {
                log.debug("没有快照文件需要清理");
                return 0;
            }
            
            Instant cutoffTime = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);
            
            for (File file : files) {
                Instant fileTime = Files.getLastModifiedTime(file.toPath()).toInstant();
                
                if (fileTime.isBefore(cutoffTime)) {
                    if (file.delete()) {
                        cleanedCount++;
                        log.debug("删除过期快照: {}", file.getName());
                    } else {
                        log.warn("删除快照失败: {}", file.getName());
                    }
                }
            }
            
            log.info("清理完成: 删除 {} 个过期快照", cleanedCount);
            
        } catch (Exception e) {
            log.error("清理快照失败", e);
        }
        
        return cleanedCount;
    }
    
    /**
     * 获取所有快照
     * 
     * @return 快照事务ID列表
     */
    public List<String> getAllSnapshots() {
        List<String> snapshots = new ArrayList<>();
        
        try {
            File dir = new File(snapshotDir);
            if (!dir.exists() || !dir.isDirectory()) {
                return snapshots;
            }
            
            File[] files = dir.listFiles((d, name) -> name.endsWith(".snapshot"));
            if (files != null) {
                for (File file : files) {
                    String txId = file.getName().replace(".snapshot", "");
                    snapshots.add(txId);
                }
            }
            
        } catch (Exception e) {
            log.error("获取快照列表失败", e);
        }
        
        return snapshots;
    }
}

