package com.resources.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * ZIP对齐工具类
 * 
 * 调用bin/win/zipalign.exe对APK进行对齐
 * 对齐要求：
 * - 未压缩资源：4字节对齐
 * - native库(.so)：4096字节对齐
 * 
 * 设计：完全复用Aapt2Validator的ProcessBuilder模式
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ZipAlignUtil {
    
    private static final Logger log = LoggerFactory.getLogger(ZipAlignUtil.class);
    
    private static final String ZIPALIGN_PATH = "bin/win/zipalign.exe";
    private static final int DEFAULT_ALIGNMENT = 4;
    private static final long COMMAND_TIMEOUT_MS = 60000;  // 60秒
    
    /**
     * 对齐APK
     * 
     * @param inputApk 输入APK路径
     * @param outputApk 输出APK路径
     * @param alignment 对齐字节数（4或4096）
     * @throws IOException 对齐失败
     */
    public static void align(String inputApk, String outputApk, int alignment) throws IOException {
        Objects.requireNonNull(inputApk, "inputApk不能为null");
        Objects.requireNonNull(outputApk, "outputApk不能为null");
        
        // 验证alignment参数
        if (alignment != 4 && alignment != 8 && alignment != 4096) {
            throw new IllegalArgumentException(
                "alignment必须是4, 8或4096，实际: " + alignment);
        }
        
        // 验证zipalign工具存在
        File zipalignFile = new File(ZIPALIGN_PATH);
        if (!zipalignFile.exists()) {
            throw new FileNotFoundException("zipalign工具不存在: " + ZIPALIGN_PATH);
        }
        
        // 验证输入文件存在
        File inputFile = new File(inputApk);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("输入APK不存在: " + inputApk);
        }
        
        if (!inputFile.canRead()) {
            throw new IOException("输入APK不可读: " + inputApk);
        }
        
        log.info("对齐APK: {} -> {} (alignment={})", inputApk, outputApk, alignment);
        
        try {
            // 构建命令（完全复用Aapt2Validator模式）
            ProcessBuilder pb = new ProcessBuilder(
                ZIPALIGN_PATH,
                "-f",  // force overwrite
                "-v",  // verbose
                String.valueOf(alignment),
                inputApk,
                outputApk
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出（复用Aapt2Validator的readProcessOutput模式）
            String output = readProcessOutput(process);
            
            // 等待完成（带超时，防止hang）
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("zipalign超时: " + COMMAND_TIMEOUT_MS + "ms");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                log.error("zipalign失败: exitCode={}", exitCode);
                log.error("输出:\n{}", output);
                throw new IOException("zipalign failed with code " + exitCode);
            }
            
            // 检查输出文件是否存在且有效
            File outputFile = new File(outputApk);
            if (!outputFile.exists()) {
                throw new IOException("zipalign未生成输出文件: " + outputApk);
            }
            
            if (outputFile.length() == 0) {
                throw new IOException("zipalign生成的文件为空: " + outputApk);
            }
            
            log.info("APK对齐完成: {} ({} 字节)", outputApk, outputFile.length());
            log.debug("zipalign输出:\n{}", output);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("zipalign被中断", e);
        } catch (IOException e) {
            // 清理可能生成的不完整文件
            try {
                Files.deleteIfExists(Paths.get(outputApk));
            } catch (IOException cleanupError) {
                log.warn("清理输出文件失败: {}", outputApk, cleanupError);
            }
            throw e;
        }
    }
    
    /**
     * 就地对齐APK（覆盖原文件）
     * 
     * @param apkPath APK路径
     * @param alignment 对齐字节数
     * @throws IOException 对齐失败
     */
    public static void alignInPlace(String apkPath, int alignment) throws IOException {
        Objects.requireNonNull(apkPath, "apkPath不能为null");
        
        String tempPath = apkPath + ".aligned.tmp";
        
        try {
            align(apkPath, tempPath, alignment);
            Files.move(Paths.get(tempPath), Paths.get(apkPath), REPLACE_EXISTING);
            log.info("APK就地对齐完成: {}", apkPath);
        } catch (Exception e) {
            // 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(tempPath));
            } catch (IOException cleanupError) {
                log.warn("清理临时文件失败: {}", tempPath, cleanupError);
            }
            throw e;
        }
    }
    
    /**
     * 使用默认对齐参数（4字节）
     * 
     * @param inputApk 输入APK路径
     * @param outputApk 输出APK路径
     * @throws IOException 对齐失败
     */
    public static void alignDefault(String inputApk, String outputApk) throws IOException {
        align(inputApk, outputApk, DEFAULT_ALIGNMENT);
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
     * 验证zipalign工具是否可用
     * 
     * @return true=可用
     */
    public static boolean isAvailable() {
        File zipalignFile = new File(ZIPALIGN_PATH);
        boolean available = zipalignFile.exists() && zipalignFile.canExecute();
        
        if (available) {
            log.debug("zipalign工具可用: {}", zipalignFile.getAbsolutePath());
        } else {
            log.warn("zipalign工具不可用: {}", ZIPALIGN_PATH);
        }
        
        return available;
    }
}

