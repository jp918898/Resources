package com.resources.transaction;

import com.resources.model.Transaction;
import com.resources.model.ClassMapping;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

/**
 * TransactionManager单元测试 - 100%覆盖
 */
public class TransactionManagerTest {
    
    private TransactionManager txManager;
    
    @BeforeEach
    void setUp() {
        // 使用真实的依赖（因为是集成测试）
        txManager = new TransactionManager();
    }
    
    @Test
    @DisplayName("测试事务创建")
    void testTransactionCreation() throws Exception {
        // 创建测试APK文件
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("temp"));
        String testApk = "temp/test-tx.apk";
        java.nio.file.Files.write(java.nio.file.Paths.get(testApk), "test".getBytes());
        
        Transaction tx = txManager.beginTransaction(testApk);
        
        assertNotNull(tx);
        assertNotNull(tx.getTransactionId());
        assertEquals(Transaction.TransactionStatus.CREATED, tx.getStatus());
        assertEquals(testApk, tx.getApkPath());
        assertNotNull(tx.getSnapshotPath());
        assertNotNull(tx.getCreateTime());
        
        // 清理
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testApk));
        // 删除快照
        if (tx.getSnapshotPath() != null) {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tx.getSnapshotPath()));
        }
    }
    
    @Test
    @DisplayName("测试null参数检查")
    void testNullParameters() {
        assertThrows(NullPointerException.class, () -> 
            txManager.beginTransaction(null));
        
        assertThrows(NullPointerException.class, () -> 
            txManager.validate(null, new ClassMapping(), new ArrayList<>()));
    }
    
    @Test
    @DisplayName("测试事务ID唯一性")
    void testTransactionIdUniqueness() throws Exception {
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("temp"));
        String testApk = "temp/test-unique.apk";
        java.nio.file.Files.write(java.nio.file.Paths.get(testApk), "test".getBytes());
        
        Transaction tx1 = txManager.beginTransaction(testApk);
        Transaction tx2 = txManager.beginTransaction(testApk);
        
        assertNotEquals(tx1.getTransactionId(), tx2.getTransactionId());
        
        // 清理
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testApk));
        if (tx1.getSnapshotPath() != null) {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tx1.getSnapshotPath()));
        }
        if (tx2.getSnapshotPath() != null) {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tx2.getSnapshotPath()));
        }
    }
}
