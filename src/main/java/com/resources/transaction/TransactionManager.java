package com.resources.transaction;

import com.resources.model.ModificationRecord;
import com.resources.model.Transaction;
import com.resources.model.ValidationResult;
import com.resources.validator.DexCrossValidator;
import com.resources.mapping.MappingValidator;
import com.resources.model.ClassMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 事务管理器 - 两阶段提交确保原子性
 * 
 * 流程：
 * 1. 开始事务 -> 创建快照
 * 2. Phase 1 - 预检查：验证所有修改的可行性，不实际写入
 * 3. Phase 2 - 执行：所有验证通过后，统一执行修改
 * 4. 提交/回滚：成功则删除快照，失败则恢复快照
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class TransactionManager {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);
    
    private final SnapshotManager snapshotManager;
    private final RollbackExecutor rollbackExecutor;
    private final DexCrossValidator dexCrossValidator;
    private final MappingValidator mappingValidator;
    
    public TransactionManager() {
        this(new SnapshotManager(),
             new RollbackExecutor(),
             new DexCrossValidator(),
             new MappingValidator());
    }
    
    public TransactionManager(SnapshotManager snapshotManager,
                             RollbackExecutor rollbackExecutor,
                             DexCrossValidator dexCrossValidator,
                             MappingValidator mappingValidator) {
        
        this.snapshotManager = Objects.requireNonNull(snapshotManager);
        this.rollbackExecutor = Objects.requireNonNull(rollbackExecutor);
        this.dexCrossValidator = Objects.requireNonNull(dexCrossValidator);
        this.mappingValidator = Objects.requireNonNull(mappingValidator);
        
        log.info("TransactionManager初始化完成");
    }
    
    /**
     * 开始事务
     * 
     * @param apkPath APK文件路径
     * @return Transaction对象
     * @throws IOException 快照创建失败
     */
    public Transaction beginTransaction(String apkPath) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.info("开始事务: {}", apkPath);
        
        // 1. 生成事务ID
        String transactionId = UUID.randomUUID().toString();
        
        // 2. 创建快照
        String snapshotPath = snapshotManager.createSnapshot(apkPath, transactionId);
        
        // 3. 创建事务对象
        Transaction transaction = new Transaction(transactionId, apkPath, snapshotPath);
        
        log.info("事务已创建: id={}", transactionId);
        
        return transaction;
    }
    
    /**
     * Phase 1 - 预检查
     * 验证所有修改的可行性，不实际写入
     * 
     * @param tx 事务
     * @param mapping 类名映射
     * @param dexPaths DEX文件路径列表
     * @return 验证结果
     */
    public ValidationResult validate(Transaction tx, ClassMapping mapping, 
                                    List<String> dexPaths) {
        
        Objects.requireNonNull(tx, "tx不能为null");
        Objects.requireNonNull(mapping, "mapping不能为null");
        Objects.requireNonNull(dexPaths, "dexPaths不能为null");
        
        log.info("Phase 1 - 预检查: txId={}", tx.getTransactionId());
        
        tx.setStatus(Transaction.TransactionStatus.VALIDATING);
        
        ValidationResult.Builder builder = new ValidationResult.Builder();
        
        // 1. 映射一致性验证
        boolean mappingValid = mappingValidator.validate(mapping, dexPaths);
        if (mappingValid) {
            builder.addPassed(ValidationResult.ValidationLevel.DEX_CROSS,
                            "映射一致性验证通过");
        } else {
            builder.addFailed(ValidationResult.ValidationLevel.DEX_CROSS,
                            "映射一致性验证失败",
                            "请检查类名映射表");
        }
        
        // 2. DEX交叉验证
        ValidationResult dexValidation = dexCrossValidator.validate(mapping, dexPaths);
        for (ValidationResult.ValidationItem item : dexValidation.getItems()) {
            builder.addItem(item.getLevel(), item.getStatus(), 
                          item.getMessage(), item.getDetails());
        }
        
        ValidationResult result = builder.build();
        
        if (result.isOverallSuccess()) {
            tx.setStatus(Transaction.TransactionStatus.VALIDATED);
            log.info("预检查通过");
        } else {
            tx.setStatus(Transaction.TransactionStatus.FAILED);
            log.error("预检查失败");
        }
        
        return result;
    }
    
    /**
     * Phase 2 - 执行并提交
     * 
     * @param tx 事务
     * @param modifications 修改记录列表
     * @throws IOException 执行失败
     */
    public void commit(Transaction tx, List<ModificationRecord> modifications) 
            throws IOException {
        
        Objects.requireNonNull(tx, "tx不能为null");
        Objects.requireNonNull(modifications, "modifications不能为null");
        
        if (!tx.canCommit()) {
            throw new IllegalStateException(
                "事务状态不允许提交: " + tx.getStatus());
        }
        
        log.info("Phase 2 - 执行提交: txId={}, modifications={}", 
                tx.getTransactionId(), modifications.size());
        
        tx.setStatus(Transaction.TransactionStatus.EXECUTING);
        
        try {
            // 注意：实际的修改操作由ResourceProcessor执行
            // 这里只是记录修改的文件
            for (ModificationRecord mod : modifications) {
                if (mod.isApplied()) {
                    tx.addModifiedFile(mod.getFilePath());
                }
            }
            
            // 标记为已提交
            tx.setStatus(Transaction.TransactionStatus.COMMITTED);
            tx.setCommitTime(System.currentTimeMillis());
            
            // 删除快照（成功后不需要保留）
            snapshotManager.deleteSnapshot(tx.getTransactionId());
            
            log.info("事务提交成功: txId={}, duration={}ms", 
                    tx.getTransactionId(), tx.getDurationMs());
            
        } catch (Exception e) {
            log.error("事务提交失败: txId={}", tx.getTransactionId(), e);
            
            // 自动回滚
            rollback(tx);
            
            throw new IOException("事务提交失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 回滚事务
     * 
     * @param tx 事务
     * @throws IOException 回滚失败
     */
    public void rollback(Transaction tx) throws IOException {
        Objects.requireNonNull(tx, "tx不能为null");
        
        if (!tx.canRollback()) {
            log.warn("事务状态不允许回滚: {}", tx.getStatus());
            return;
        }
        
        log.info("回滚事务: txId={}", tx.getTransactionId());
        
        try {
            // 1. 恢复快照
            if (snapshotManager.snapshotExists(tx.getTransactionId())) {
                rollbackExecutor.rollback(tx.getSnapshotPath(), tx.getApkPath());
                
                // 2. 删除快照
                snapshotManager.deleteSnapshot(tx.getTransactionId());
                
                tx.setStatus(Transaction.TransactionStatus.ROLLED_BACK);
                
                log.info("回滚成功: txId={}", tx.getTransactionId());
            } else {
                log.warn("快照不存在，无法回滚: {}", tx.getSnapshotPath());
                tx.setStatus(Transaction.TransactionStatus.FAILED);
            }
            
        } catch (Exception e) {
            log.error("回滚失败: txId={}", tx.getTransactionId(), e);
            tx.setStatus(Transaction.TransactionStatus.FAILED);
            tx.setErrorMessage(e.getMessage());
            throw new IOException("回滚失败: " + e.getMessage(), e);
        }
    }
}

