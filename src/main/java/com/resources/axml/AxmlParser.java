package com.resources.axml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static com.resources.axml.NodeVisitor.TYPE_INT_BOOLEAN;
import static com.resources.axml.NodeVisitor.TYPE_STRING;

/**
 * AxmlParser - Android AXML解析器
 *
 * @author Manifest Builder Team
 */
public class AxmlParser implements ResConst {
    private static final Logger LOGGER = Logger.getLogger(AxmlParser.class.getName());

    public static final int END_FILE = 7;
    public static final int END_NS = 5;
    public static final int END_TAG = 3;
    public static final int START_FILE = 1;
    public static final int START_NS = 4;
    public static final int START_TAG = 2;
    public static final int TEXT = 6;

    private int attributeCount;

    private IntBuffer attrs;

    private int classAttribute;
    private int fileSize = -1;
    private int idAttribute;
    private ByteBuffer in;
    private int lineNumber;
    private int nameIdx;
    private int nsIdx;

    private int prefixIdx;

    private int[] resourceIds;

    private String[] strings;

    private int styleAttribute;

    private int textIdx;
    
    // 保存StringPool的flags（UTF-8/UTF-16编码标志）
    private int stringPoolFlags = 0;
    
    // 健壮性标志
    private boolean hasEncounteredStartElement = false;

    public AxmlParser(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public AxmlParser(ByteBuffer in) {
        super();
        this.in = in.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int getAttrCount() {
        return attributeCount;
    }

    public int getAttributeCount() {
        return attributeCount;
    }

    public String getAttrName(int i) {
        int idx = attrs.get(i * 5 + 1);
        return strings[idx];
    }

    public String getAttrNs(int i) {
        int idx = attrs.get(i * 5 + 0);
        return idx >= 0 ? strings[idx] : null;
    }

    String getAttrRawString(int i) {
        int idx = attrs.get(i * 5 + 2);
        if (idx >= 0) {
            return strings[idx];
        }
        return null;
    }

    public int getAttrResId(int i) {
        if (resourceIds != null) {
            int idx = attrs.get(i * 5 + 1);
            if (idx >= 0 && idx < resourceIds.length) {
                return resourceIds[idx];
            }
        }
        return -1;
    }

    public int getAttrType(int i) {
        return attrs.get(i * 5 + 3) >> 24;
    }

    /**
     * 获取属性值
     * 支持9种主要属性类型
     * 
     * @param i 属性索引
     * @return 属性值对象
     */
    public Object getAttrValue(int i) {
        int v = attrs.get(i * 5 + 4);

        // 处理特殊属性（id, style, class）
        if (i == idAttribute) {
            return ValueWrapper.wrapId(v, getAttrRawString(i));
        } else if (i == styleAttribute) {
            return ValueWrapper.wrapStyle(v, getAttrRawString(i));
        } else if (i == classAttribute) {
            return ValueWrapper.wrapClass(v, getAttrRawString(i));
        }

        int type = getAttrType(i);
        String rawString = getAttrRawString(i);

        switch (type) {
        // 1. TYPE_NULL (0x00) - 空值
        case com.resources.util.TypedValue.TYPE_NULL:
            if (v == com.resources.util.TypedValue.DATA_NULL_EMPTY) {
                return null;  // @empty
            }
            return null;  // @null or undefined
            
        // 2. TYPE_REFERENCE (0x01) - 资源引用
        case com.resources.util.TypedValue.TYPE_REFERENCE:
            return new ResourceReference(v, rawString);
            
        // 3. TYPE_ATTRIBUTE (0x02) - 属性引用 (如 android:attr/textColor)
        case com.resources.util.TypedValue.TYPE_ATTRIBUTE:
            return new ResourceReference(v, rawString);
            
        // 4. TYPE_STRING (0x03) - 字符串
        case TYPE_STRING:
            if (v >= 0 && v < strings.length) {
                return strings[v];
            }
            LOGGER.warning(String.format("Invalid string index %d (pool size=%d)", v, strings.length));
            return rawString != null ? rawString : "";
            
        // 5. TYPE_FLOAT (0x04) - 浮点数
        case com.resources.util.TypedValue.TYPE_FLOAT:
            return Float.intBitsToFloat(v);
            
        // 6. TYPE_DIMENSION (0x05) - 尺寸值 (如 16dp, 24sp)
        case com.resources.util.TypedValue.TYPE_DIMENSION:
            // 返回复杂数据，调用者可通过TypedValue.complexToFloat()和getComplexUnit()解析
            return v;  // 复杂数据，保持原始值
            
        // 7. TYPE_FRACTION (0x06) - 百分比 (如 50%, 75%p)
        case com.resources.util.TypedValue.TYPE_FRACTION:
            // 返回复杂数据，调用者可通过TypedValue.complexToFraction()解析
            return v;  // 复杂数据，保持原始值
            
        // 8. TYPE_DYNAMIC_REFERENCE (0x07) - 动态资源引用
        case com.resources.util.TypedValue.TYPE_DYNAMIC_REFERENCE:
            return new ResourceReference(v, rawString);
            
        // 9. TYPE_DYNAMIC_ATTRIBUTE (0x08) - 动态属性引用
        case com.resources.util.TypedValue.TYPE_DYNAMIC_ATTRIBUTE:
            return new ResourceReference(v, rawString);
            
        // 10. TYPE_INT_BOOLEAN (0x12) - 布尔值
        case TYPE_INT_BOOLEAN:
            return v != 0;
            
        // 11. 其他整型值 (0x10-0x1F)
        case com.resources.util.TypedValue.TYPE_INT_DEC:
        case com.resources.util.TypedValue.TYPE_INT_HEX:
        case com.resources.util.TypedValue.TYPE_INT_COLOR_ARGB8:
        case com.resources.util.TypedValue.TYPE_INT_COLOR_RGB8:
        case com.resources.util.TypedValue.TYPE_INT_COLOR_ARGB4:
        case com.resources.util.TypedValue.TYPE_INT_COLOR_RGB4:
            return v;
            
        default:
            // 未知类型，返回原始值并记录警告
            if (type >= com.resources.util.TypedValue.TYPE_FIRST_INT 
                && type <= com.resources.util.TypedValue.TYPE_LAST_INT) {
                return v;  // 整型范围内的其他类型
            }
            LOGGER.warning(String.format("Unknown attribute type: 0x%02x, returning raw value", type));
            return v;
        }
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getName() {
        return strings[nameIdx];
    }

    public String getNamespacePrefix() {
        return strings[prefixIdx];
    }

    public String getNamespaceUri() {
        return nsIdx >= 0 ? strings[nsIdx] : null;
    }

    public String getText() {
        return strings[textIdx];
    }

    public int next() throws IOException {
        if (fileSize < 0) {
            int type = in.getInt() & 0xFFFF;
            if (type != RES_XML_TYPE) {
                throw new RuntimeException("Invalid AXML file type: expected " + RES_XML_TYPE + ", got " + type);
            }
            fileSize = in.getInt();
            return START_FILE;
        }
        int event = -1;
        for (int p = in.position(); p < fileSize; p = in.position()) {
            // 防御#2070: 异常的文件结尾（参考 Apktool BinaryXmlResourceParser.java:742-746）
            if (in.remaining() < 8) {  // 至少需要8字节（type + size）
                LOGGER.warning(String.format("AXML hit unexpected end of file at position 0x%08x", in.position()));
                return END_FILE;
            }
            
            int type = in.getInt() & 0xFFFF;
            int size = in.getInt();
            
            // 验证chunk大小
            if (size < 8 || p + size > fileSize) {
                LOGGER.warning(String.format("Invalid chunk size %d at position 0x%08x", size, p));
                return END_FILE;
            }
            
            switch (type) {
            case RES_XML_START_ELEMENT_TYPE: {
                hasEncounteredStartElement = true;  // 标记已遇到START_ELEMENT
                {
                    lineNumber = in.getInt();
                    in.getInt();/* skip, 0xFFFFFFFF */
                    nsIdx = in.getInt();
                    nameIdx = in.getInt();
                    int flag = in.getInt();// 0x00140014 ?
                    if (flag != 0x00140014) {
                        throw new RuntimeException();
                    }
                }

                attributeCount = in.getShort() & 0xFFFF;
                idAttribute = (in.getShort() & 0xFFFF) - 1;
                classAttribute = (in.getShort() & 0xFFFF) - 1;
                styleAttribute = (in.getShort() & 0xFFFF) - 1;

                attrs = in.asIntBuffer();

                event = START_TAG;
            }
                break;
            case RES_XML_END_ELEMENT_TYPE: {
                in.position(p + size);
                event = END_TAG;
            }
                break;
            case RES_XML_START_NAMESPACE_TYPE:
                lineNumber = in.getInt();
                in.getInt();/* 0xFFFFFFFF */
                prefixIdx = in.getInt();
                nsIdx = in.getInt();
                event = START_NS;
                break;
            case RES_XML_END_NAMESPACE_TYPE:
                // 防御#3838: START_ELEMENT前的无效END_NAMESPACE（参考 Apktool:796-802）
                if (!hasEncounteredStartElement) {
                    LOGGER.warning(String.format(
                        "Skipping END_NAMESPACE event at position 0x%08x before START_ELEMENT", p));
                    in.position(p + size);
                    continue;  // 跳过此事件，继续解析
                }
                in.position(p + size);
                event = END_NS;
                break;
            case RES_STRING_POOL_TYPE:
                // 🔧 创建StringItems实例以接收flags
                StringItems stringItems = new StringItems();
                strings = StringItems.read(in, stringItems);
                stringPoolFlags = stringItems.getOriginalFlags();  // 保存flags
                in.position(p + size);
                continue;
            case RES_XML_RESOURCE_MAP_TYPE:
                int count = size / 4 - 2;
                if (count < 0 || count > 10000) {  // 合理性检查
                    LOGGER.warning("Invalid resource map count: " + count);
                    in.position(p + size);
                    continue;
                }
                resourceIds = new int[count];
                // 防御#3236: 防止读取超出chunk边界（参考 Apktool:112-127）
                int maxPosition = p + size;
                for (int i = 0; i < count; i++) {
                    if (in.position() + 4 > maxPosition) {
                        LOGGER.warning(String.format(
                            "Resource map entry %d at position 0x%08x exceeds chunk boundary at 0x%08x", 
                            i, in.position(), maxPosition));
                        break;  // 提前终止，避免崩溃
                    }
                    resourceIds[i] = in.getInt();
                }
                in.position(p + size);
                continue;
            case RES_XML_CDATA_TYPE:
                lineNumber = in.getInt();
                in.getInt();/* 0xFFFFFFFF */
                textIdx = in.getInt();

                in.getInt();/* 00000008 00000000 */
                in.getInt();

                event = TEXT;
                break;
            default:
                // 遇到未知chunk类型时优雅处理，而不是崩溃
                LOGGER.warning(String.format(
                    "Unknown chunk type 0x%04x at position 0x%08x, size=%d. Skipping.", 
                    type, p, size));
                in.position(p + size);
                continue;
            }
            in.position(p + size);
            return event;
        }
        return END_FILE;
    }
    
    /**
     * 获取StringPool的原始flags
     * @return flags值（0x100表示UTF-8，0表示UTF-16）
     */
    public int getStringPoolFlags() {
        return stringPoolFlags;
    }
}
