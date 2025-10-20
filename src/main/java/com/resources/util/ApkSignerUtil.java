package com.resources.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * APK签名工具类
 * 
 * 调用bin/win/apksigner.bat对APK进行签名
 * 支持v1和v2签名方案
 * 
 * 设计：完全复用Aapt2Validator的ProcessBuilder模式
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ApkSignerUtil {
    
    private static final Logger log = LoggerFactory.getLogger(ApkSignerUtil.class);
    
    private static final String APKSIGNER_PATH = "bin/win/apksigner.bat";
    private static final String TEST_KEYSTORE = "config/keystore/testkey.jks";
    private static final String TEST_PASSWORD = "testkey";
    private static final long COMMAND_TIMEOUT_MS = 120000;  // 120秒
    
    /**
     * 签名APK
     * 
     * @param apkPath APK路径（就地签名）
     * @param keystorePath 密钥库路径
     * @param storePass 密钥库密码
     * @param keyPass 密钥密码
     * @throws IOException 签名失败
     */
    public static void sign(String apkPath, String keystorePath, 
                           String storePass, String keyPass) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        Objects.requireNonNull(keystorePath, "keystorePath不能为null");
        Objects.requireNonNull(storePass, "storePass不能为null");
        Objects.requireNonNull(keyPass, "keyPass不能为null");
        
        // 验证apksigner工具存在
        File apksignerFile = new File(APKSIGNER_PATH);
        if (!apksignerFile.exists()) {
            throw new FileNotFoundException("apksigner工具不存在: " + APKSIGNER_PATH);
        }
        
        // 验证密钥库存在
        File keystoreFile = new File(keystorePath);
        if (!keystoreFile.exists()) {
            throw new FileNotFoundException("密钥库不存在: " + keystorePath);
        }
        
        if (!keystoreFile.canRead()) {
            throw new IOException("密钥库不可读: " + keystorePath);
        }
        
        // 验证APK存在
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            throw new FileNotFoundException("APK不存在: " + apkPath);
        }
        
        if (!apkFile.canRead() || !apkFile.canWrite()) {
            throw new IOException("APK不可读写: " + apkPath);
        }
        
        log.info("签名APK: {} (keystore={})", apkPath, keystorePath);
        
        try {
            // 构建命令（完全复用Aapt2Validator模式）
            ProcessBuilder pb = new ProcessBuilder(
                APKSIGNER_PATH,
                "sign",
                "--ks", keystorePath,
                "--ks-pass", "pass:" + storePass,
                "--key-pass", "pass:" + keyPass,
                "--v1-signing-enabled", "true",
                "--v2-signing-enabled", "true",
                apkPath
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出（复用Aapt2Validator的readProcessOutput模式）
            String output = readProcessOutput(process);
            
            // 等待完成（带超时，防止hang）
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("apksigner超时: " + COMMAND_TIMEOUT_MS + "ms");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                log.error("apksigner失败: exitCode={}", exitCode);
                log.error("输出:\n{}", output);
                throw new IOException("apksigner failed with code " + exitCode);
            }
            
            // 检查签名是否成功（APK应该存在且可读）
            apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                throw new IOException("签名后APK文件消失: " + apkPath);
            }
            
            if (apkFile.length() == 0) {
                throw new IOException("签名后APK文件为空: " + apkPath);
            }
            
            log.info("APK签名完成: {} ({} 字节)", apkPath, apkFile.length());
            log.debug("apksigner输出:\n{}", output);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("apksigner被中断", e);
        }
    }
    
    /**
     * 使用测试密钥签名APK
     * 
     * 使用config/keystore/testkey.jks签名
     * 密码：testkey/testkey
     * 
     * @param apkPath APK路径
     * @throws IOException 签名失败
     */
    public static void signWithTestKey(String apkPath) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        log.info("使用测试密钥签名APK: {}", apkPath);
        
        sign(apkPath, TEST_KEYSTORE, TEST_PASSWORD, TEST_PASSWORD);
    }
    
    /**
     * 验证APK签名
     * 
     * @param apkPath APK路径
     * @return true=签名有效
     */
    public static boolean verify(String apkPath) {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                APKSIGNER_PATH,
                "verify",
                "-v",  // verbose
                apkPath
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                log.warn("apksigner verify超时");
                return false;
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                log.warn("APK签名验证失败: exitCode={}", exitCode);
                log.debug("输出:\n{}", output);
                return false;
            }
            
            log.info("APK签名验证通过: {}", apkPath);
            log.debug("verify输出:\n{}", output);
            
            return true;
            
        } catch (Exception e) {
            log.warn("签名验证异常: {}", apkPath, e);
            return false;
        }
    }
    
    /**
     * 读取进程输出（复用Aapt2Validator模式）
     */
    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        return output.toString();
    }
    
    /**
     * 验证apksigner工具是否可用
     * 
     * @return true=可用
     */
    public static boolean isAvailable() {
        File apksignerFile = new File(APKSIGNER_PATH);
        boolean available = apksignerFile.exists();
        
        if (available) {
            log.debug("apksigner工具可用: {}", apksignerFile.getAbsolutePath());
        } else {
            log.warn("apksigner工具不可用: {}", APKSIGNER_PATH);
        }
        
        return available;
    }
    
    /**
     * 验证测试密钥是否可用
     * 
     * @return true=可用
     */
    public static boolean isTestKeystoreAvailable() {
        File keystoreFile = new File(TEST_KEYSTORE);
        boolean available = keystoreFile.exists() && keystoreFile.canRead();
        
        if (available) {
            log.debug("测试密钥库可用: {}", keystoreFile.getAbsolutePath());
        } else {
            log.warn("测试密钥库不可用: {}", TEST_KEYSTORE);
        }
        
        return available;
    }
}

