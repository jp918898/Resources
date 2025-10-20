package com.resources.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * AXML格式验证器
 * 
 * 提供轻量级的AXML格式预检，避免AxmlParser抛RuntimeException
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class AxmlValidator {
    
    /**
     * AXML文件magic number (RES_XML_TYPE)
     */
    private static final int RES_XML_TYPE = 0x0003;
    
    /**
     * 验证是否为合法的AXML二进制文件
     * 
     * 验证项：
     * 1. 文件大小 >= 8字节（type + headerSize + fileSize）
     * 2. Magic number = 0x0003 (RES_XML_TYPE)
     * 3. Header size = 8
     * 4. File size的一致性检查
     * 
     * @param data XML字节数据
     * @return true=合法AXML, false=文本XML/损坏文件/其他格式
     */
    public static boolean isValidAxml(byte[] data) {
        if (data == null || data.length < 8) {
            return false;  // AXML最小8字节
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            
            // 验证magic number (0x0003)
            int type = buffer.getShort() & 0xFFFF;
            if (type != RES_XML_TYPE) {
                return false;
            }
            
            // 验证头部大小（标准值为8）
            int headerSize = buffer.getShort() & 0xFFFF;
            if (headerSize != 8) {
                return false;
            }
            
            // 验证文件大小一致性
            int declaredSize = buffer.getInt();
            if (declaredSize < 8 || declaredSize > data.length) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            // ByteBuffer操作异常，非法格式
            return false;
        }
    }
}


