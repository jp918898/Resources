package com.resources.axml;

import com.resources.arsc.ModifiedUTF8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class StringItems extends ArrayList<StringItem> {
	private static final Logger LOGGER = Logger.getLogger(StringItems.class.getName());
	
	private static final int UTF8_FLAG = 0x00000100;
	
	// 字符集解码器
	private static final CharsetDecoder UTF16LE_DECODER = StandardCharsets.UTF_16LE.newDecoder();
	private static final CharsetDecoder UTF8_DECODER = StandardCharsets.UTF_8.newDecoder();
	private static final CharsetDecoder CESU8_DECODER = Charset.forName("CESU8").newDecoder();
	
	/**
	 * 样式字符串池
	 * 用于存储AXML格式中的样式相关信息
	 */
	private List<StringItem> styleItems = new ArrayList<>();
	
	/**
	 * 样式数据（Styles Data）
	 * 存储样式span信息：[tagNameIndex, startPos, endPos, ..., -1]
	 * 每个字符串的样式由一系列三元组组成，以-1结尾
	 * 参考 Apktool ResStringPool.java mStyles字段
	 */
	private int[] styleData = null;
	
	/**
	 * 获取样式字符串池
	 * @return 样式字符串池
	 */
	public List<StringItem> getStyleItems() {
		return styleItems;
	}
	
	/**
	 * 添加样式项目
	 * @param styleItem 样式项目
	 */
	public void addStyleItem(StringItem styleItem) {
		styleItems.add(styleItem);
	}
	
	/**
	 * 获取样式项目数量
	 * @return 样式项目数量
	 */
	public int getStyleCount() {
		return styleItems.size();
	}
	
	/**
	 * 获取样式数据
	 * @return 样式数据数组
	 */
	public int[] getStyleData() {
		return styleData;
	}
	
	/**
	 * 设置样式数据
	 * @param styleData 样式数据数组（三元组：tagNameIndex, startPos, endPos, ..., -1）
	 */
	public void setStyleData(int[] styleData) {
		this.styleData = styleData;
	}
	
	/**
	 * 获取带HTML样式的字符串
	 * 
	 * @param index 字符串索引
	 * @param strings 字符串数组
	 * @param styleOffsets 样式偏移数组
	 * @param styles 样式数据数组
	 * @return 带HTML样式标签的字符串
	 */
	public static String getHTML(int index, String[] strings, int[] styleOffsets, int[] styles) {
		if (strings == null || index < 0 || index >= strings.length) {
			return null;
		}
		
		String text = strings[index];
		if (text == null) {
			return null;
		}
		
		// 获取样式数据
		int[] style = getStyle(index, styleOffsets, styles);
		if (style == null) {
			// 无样式，转义XML特殊字符后返回
			return escapeXmlChars(text);
		}
		
		// 如果样式起始位置超出字符串长度，跳过样式处理
		if (style.length > 1 && style[1] > text.length()) {
			return escapeXmlChars(text);
		}
		
		// 转换样式为HTML标签（简化实现，完整实现需要StyledString类）
		// 这里返回带<b>, <i>等标签的字符串
		StringBuilder result = new StringBuilder();
		int lastPos = 0;
		
		// 遍历三元组：[tagIndex, start, end, tagIndex, start, end, ...]
		for (int i = 0; i < style.length; i += 3) {
			if (i + 2 >= style.length) break;
			
			int tagIndex = style[i];
			int start = style[i + 1];
			int end = style[i + 2];
			
			// 添加样式前的文本
			if (start > lastPos) {
				result.append(escapeXmlChars(text.substring(lastPos, start)));
			}
			
			// 获取标签名
			String tagName = (tagIndex >= 0 && tagIndex < strings.length) 
				? strings[tagIndex] : "span";
			
			// 添加带标签的文本
			result.append("<").append(tagName).append(">");
			if (end <= text.length()) {
				result.append(escapeXmlChars(text.substring(start, end)));
			}
			result.append("</").append(tagName).append(">");
			
			lastPos = end;
		}
		
		// 添加剩余文本
		if (lastPos < text.length()) {
			result.append(escapeXmlChars(text.substring(lastPos)));
		}
		
		return result.toString();
	}
	
	/**
	 * 获取字符串的样式信息
	 * 
	 * @param index 字符串索引
	 * @param styleOffsets 样式偏移数组
	 * @param styles 样式数据数组
	 * @return 样式三元组数组，null表示无样式
	 */
	private static int[] getStyle(int index, int[] styleOffsets, int[] styles) {
		if (styleOffsets == null || styles == null || index >= styleOffsets.length) {
			return null;
		}
		
		int offset = styleOffsets[index] / 4;
		int count = 0;
		
		// 统计直到遇到-1结束符
		for (int i = offset; i < styles.length; i++) {
			if (styles[i] == -1) {
				break;
			}
			count += 1;
		}
		
		if (count == 0 || (count % 3) != 0) {
			return null;
		}
		
		// 读取三元组：[tagIndex, start, end, tagIndex, start, end, ...]
		int[] style = new int[count];
		for (int i = offset, j = 0; i < styles.length;) {
			if (styles[i] == -1) {
				break;
			}
			style[j++] = styles[i++];
		}
		return style;
	}
	
	/**
	 * 转义XML特殊字符
	 * 
	 * @param str 原始字符串
	 * @return 转义后的字符串
	 */
	private static String escapeXmlChars(String str) {
		if (str == null) {
			return null;
		}
		return str.replace("&", "&amp;")
				  .replace("<", "&lt;")
				  .replace("]]>", "]]&gt;");
	}

	/**
	 * 读取UTF-16字符串长度
	 * @param array 字节数组
	 * @param offset 偏移量
	 * @return int[2]: [头部字节数, 数据字节数]
	 */
	private static int[] getUtf16(byte[] array, int offset) {
		int val = ((array[offset + 1] & 0xFF) << 8) | (array[offset] & 0xFF);
		
		if ((val & 0x8000) != 0) {
			// 长字符串（>= 32768字符）
			int high = (array[offset + 3] & 0xFF) << 8;
			int low = array[offset + 2] & 0xFF;
			int len_value = ((val & 0x7FFF) << 16) + high + low;
			return new int[] { 4, len_value * 2 };
		}
		
		// 短字符串（< 32768字符）
		return new int[] { 2, val * 2 };
	}
	
    /**
     * 读取字符串池（向后兼容版本）
     * @param in ByteBuffer
     * @return 字符串数组
     * @throws IOException 读取失败
     */
    public static String[] read(ByteBuffer in) throws IOException {
        return read(in, null);
    }
    
    /**
     * 读取字符串池（支持保存原始flags）
     * @param in ByteBuffer
     * @param receiver 如果非null，将保存原始flags到此实例
     * @return 字符串数组
     * @throws IOException 读取失败
     */
    public static String[] read(ByteBuffer in, StringItems receiver) throws IOException {
        int trunkOffset = in.position() - 8;
        int stringCount = in.getInt();
        @SuppressWarnings("unused")
        int styleOffsetCount = in.getInt();
        int flags = in.getInt();
        
        // 🔧 保存原始flags（新增）
        if (receiver != null) {
            receiver.originalFlags = flags;
            receiver.hasOriginalFlags = true;
        }
        
        int stringDataOffset = in.getInt();
        @SuppressWarnings("unused")
        int stylesOffset = in.getInt();
        int offsets[] = new int[stringCount];
        String strings[] = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            offsets[i] = in.getInt();
        }

        int base = trunkOffset + stringDataOffset;
        boolean isUtf8 = (flags & UTF8_FLAG) != 0;
        
        for (int i = 0; i < offsets.length; i++) {
            in.position(base + offsets[i]);
            String s = null;

            if (isUtf8) {
                // UTF-8模式：读取长度并解码
                // 第1步：跳过UTF-16字符长度字段（Android AXML格式要求）
                u8length(in);
                
                // 第2步：读取UTF-8字节长度
                int byteLen = u8length(in);
                
                // 第3步：定位到实际字符串数据
                int start = in.position();
                int blength = byteLen;  // 使用字节长度，而非字符长度
                
                // 查找字符串终止符（使用数组直接访问）
                byte[] array = in.array();
                while (start + blength < array.length && array[start + blength] != 0) {
                    blength++;
                }
                
                // 边界检查
                if (start + blength > array.length) {
                    LOGGER.warning(String.format(
                        "字符串索引 %d 超出边界: start=%d, length=%d, arrayLength=%d", 
                        i, start, blength, array.length));
                    s = "";  // 降级：返回空字符串
                } else {
                    // 尝试标准UTF-8解码
                    try {
                        ByteBuffer wrappedBuffer = ByteBuffer.wrap(array, start, blength);
                        s = UTF8_DECODER.decode(wrappedBuffer).toString();
                    } catch (CharacterCodingException e) {
                        // 降级到CESU-8解码
                        // Android某些版本使用CESU-8编码代理对
                        try {
                            ByteBuffer wrappedBufferRetry = ByteBuffer.wrap(array, start, blength);
                            s = CESU8_DECODER.decode(wrappedBufferRetry).toString();
                            LOGGER.fine("使用CESU-8解码器成功解码字符串索引 " + i);
                        } catch (CharacterCodingException e2) {
                            LOGGER.warning("字符串索引 " + i + " UTF-8和CESU-8解码均失败，使用降级方案");
                            // 最后降级：使用标准String构造函数
                            s = new String(array, start, blength, StandardCharsets.UTF_8);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.warning("字符串索引 " + i + " 索引越界，使用降级方案");
                        s = "";
                    }
                }
            } else {
                // UTF-16LE模式
                int[] val = getUtf16(in.array(), in.position());
                int offset = in.position() + val[0];
                int length = val[1];
                
                // 边界检查
                if (offset + length > in.array().length) {
                    LOGGER.warning(String.format(
                        "字符串索引 %d UTF-16LE超出边界: offset=%d, length=%d", 
                        i, offset, length));
                    s = "";
                } else {
                    try {
                        ByteBuffer wrappedBuffer = ByteBuffer.wrap(in.array(), offset, length);
                        s = UTF16LE_DECODER.decode(wrappedBuffer).toString();
                    } catch (CharacterCodingException e) {
                        LOGGER.warning("字符串索引 " + i + " UTF-16LE解码失败: " + e.getMessage());
                        // 降级方案
                        s = new String(in.array(), offset, length, StandardCharsets.UTF_16LE);
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.warning("字符串索引 " + i + " 索引越界");
                        s = "";
                    }
                }
            }
            
            strings[i] = s;
        }
        return strings;
    }

	static int u16length(ByteBuffer in) {
		int length = in.getShort() & 0xFFFF;
		if (length > 0x7FFF) {
			length = ((length & 0x7FFF) << 8) | (in.getShort() & 0xFFFF);
		}
		return length;
	}

	static int u8length(ByteBuffer in) {
		int len = in.get() & 0xFF;
		if ((len & 0x80) != 0) {
			len = ((len & 0x7F) << 8) | (in.get() & 0xFF);
		}
		return len;
	}

	byte[] stringData;
	
	private boolean useUTF8 = true;
	
	/**
	 * 原始StringPool的flags
	 * 从AXML文件读取时保存，用于在写入时保持编码一致性
	 * 0 = 原始为UTF-16, 0x100 = 原始为UTF-8
	 */
	private int originalFlags = 0;
	
	/**
	 * 是否设置过原始flags
	 * 用于区分"未设置"(默认UTF-8)和"显式设置为UTF-16"(originalFlags=0)
	 */
	private boolean hasOriginalFlags = false;

	public int getSize() {
		// AXML字符串池头部大小 + 字符串偏移数组 + 样式偏移数组 + 字符串数据 + 样式数据
		int headerSize = 5 * 4;  // 字符串池头部固定大小（5个int字段）
		int stringOffsetsSize = this.size() * 4;  // 字符串偏移数组
		int styleOffsetsSize = styleItems.size() * 4;  // 样式偏移数组
		int stringDataSize = stringData != null ? stringData.length : 0;  // 字符串数据
		int stylesDataSize = styleData != null ? styleData.length * 4 : 0;  // 样式数据
		
		return headerSize + stringOffsetsSize + styleOffsetsSize + stringDataSize + stylesDataSize;
	}

	public void prepare() throws IOException {
		// 🔧 根据原始flags初始化useUTF8（保持原编码）
		if (hasOriginalFlags) {
			// 如果设置过原始flags，优先使用原始编码
			useUTF8 = (originalFlags & UTF8_FLAG) != 0;
		}
		// 否则使用默认值（useUTF8 = true，已在字段声明时初始化）
		
		// 检查是否有超长字符串（强制UTF-16）
		for (StringItem s : this) {
			if (s.data.length() > 0x7FFF) {
				useUTF8 = false;
				break;
			}
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i = 0;
		int offset = 0;
		baos.reset();
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (StringItem item : this) {
			item.index = i++;
			String stringData = item.data;
			Integer of = map.get(stringData);
			if (of != null) {
				item.dataOffset = of;
			} else {
				item.dataOffset = offset;
				map.put(stringData, offset);
				if (useUTF8) {
					try {
						byte[] data = ModifiedUTF8.encode(stringData);
						int u8lenght = data.length;
						int utf8CharLen = ModifiedUTF8.countCharacters(data);

						int charLenBytes = 0;
						if (utf8CharLen > 0x7F) {
							charLenBytes = 2;
							baos.write((utf8CharLen >> 8) | 0x80);
							baos.write(utf8CharLen & 0xFF);
						} else {
							charLenBytes = 1;
							baos.write(utf8CharLen);
						}

						int byteLenBytes = 0;
						if (u8lenght > 0x7F) {
							byteLenBytes = 2;
							baos.write((u8lenght >> 8) | 0x80);
							baos.write(u8lenght & 0xFF);
						} else {
							byteLenBytes = 1;
							baos.write(u8lenght);
						}
						
						baos.write(data);
						baos.write(0);
						
						// 正确计算offset增量
						offset += charLenBytes + byteLenBytes + u8lenght + 1;
					} catch (IOException e) {
						// 降级：使用标准UTF-8
						int length = stringData.length();
						byte[] data = stringData.getBytes("UTF-8");
						int u8lenght = data.length;

						int charLenBytes = 0;
						if (length > 0x7F) {
							charLenBytes = 2;
							baos.write((length >> 8) | 0x80);
							baos.write(length & 0xFF);
						} else {
							charLenBytes = 1;
							baos.write(length);
						}

						int byteLenBytes = 0;
						if (u8lenght > 0x7F) {
							byteLenBytes = 2;
							baos.write((u8lenght >> 8) | 0x80);
							baos.write(u8lenght & 0xFF);
						} else {
							byteLenBytes = 1;
							baos.write(u8lenght);
						}
						
						baos.write(data);
						baos.write(0);
						
						// 正确计算offset增量
						offset += charLenBytes + byteLenBytes + u8lenght + 1;
					}
				} else {
					// UTF-16LE编码
					int length = stringData.length();
					byte[] data = stringData.getBytes("UTF-16LE");
					
					int lenBytes = 0;
					if (length > 0x7FFF) {
						lenBytes = 4;  // 长度头部占4字节
						int x = (length >> 16) | 0x8000;
						baos.write(x & 0xFF);
						baos.write((x >> 8) & 0xFF);
						baos.write(length & 0xFF);
						baos.write((length >> 8) & 0xFF);
					} else {
						lenBytes = 2;  // 长度头部占2字节
						baos.write(length & 0xFF);
						baos.write((length >> 8) & 0xFF);
					}
					
					baos.write(data);
					baos.write(0);
					baos.write(0);
					
					// 正确计算offset增量
					offset += lenBytes + data.length + 2;  // lenBytes + data + 终止符(2字节)
				}
			}
		}
		
		// 🆕 处理样式字符串
		for (StringItem styleItem : styleItems) {
			String styleData = styleItem.data;
			Integer of = map.get(styleData);
			if (of != null) {
				styleItem.dataOffset = of;
			} else {
				styleItem.dataOffset = offset;
				map.put(styleData, offset);
				if (useUTF8) {
					try {
						byte[] data = ModifiedUTF8.encode(styleData);
						int u8length = data.length;
						int utf8CharLen = ModifiedUTF8.countCharacters(data);

						int charLenBytes = 0;
						if (utf8CharLen > 0x7F) {
							charLenBytes = 2;
							baos.write((utf8CharLen >> 8) | 0x80);
							baos.write(utf8CharLen & 0xFF);
						} else {
							charLenBytes = 1;
							baos.write(utf8CharLen);
						}

						int byteLenBytes = 0;
						if (u8length > 0x7F) {
							byteLenBytes = 2;
							baos.write((u8length >> 8) | 0x80);
							baos.write(u8length & 0xFF);
						} else {
							byteLenBytes = 1;
							baos.write(u8length);
						}
						
						baos.write(data);
						baos.write(0);
						
						// 正确计算offset增量
						offset += charLenBytes + byteLenBytes + u8length + 1;
					} catch (IOException e) {
						// 降级：使用标准UTF-8
						int length = styleData.length();
						byte[] data = styleData.getBytes("UTF-8");
						int u8length = data.length;

						int charLenBytes = 0;
						if (length > 0x7F) {
							charLenBytes = 2;
							baos.write((length >> 8) | 0x80);
							baos.write(length & 0xFF);
						} else {
							charLenBytes = 1;
							baos.write(length);
						}

						int byteLenBytes = 0;
						if (u8length > 0x7F) {
							byteLenBytes = 2;
							baos.write((u8length >> 8) | 0x80);
							baos.write(u8length & 0xFF);
						} else {
							byteLenBytes = 1;
							baos.write(u8length);
						}
						
						baos.write(data);
						baos.write(0);
						
						// 正确计算offset增量
						offset += charLenBytes + byteLenBytes + u8length + 1;
					}
				} else {
					// UTF-16LE编码
					int length = styleData.length();
					byte[] data = styleData.getBytes("UTF-16LE");
					
					int lenBytes = 0;
					if (length > 0x7FFF) {
						lenBytes = 4;
						int x = (length >> 16) | 0x8000;
						baos.write(x & 0xFF);
						baos.write((x >> 8) & 0xFF);
						baos.write(length & 0xFF);
						baos.write((length >> 8) & 0xFF);
					} else {
						lenBytes = 2;
						baos.write(length & 0xFF);
						baos.write((length >> 8) & 0xFF);
					}
					
					baos.write(data);
					baos.write(0);
					baos.write(0);
					
					// 正确计算offset增量
					offset += lenBytes + data.length + 2;
				}
			}
		}
		
		stringData = baos.toByteArray();
	}

	public void write(ByteBuffer out) throws IOException {
		// ResStringPool_header 结构（参考 Apktool ResStringPool.java:66-72）
		// 字段1: stringCount
		out.putInt(this.size());
		
		// 字段2: styleCount（样式偏移数组的数量）
		out.putInt(styleItems.size());
		
		// 字段3: flags（UTF-8标志）
		out.putInt(useUTF8 ? UTF8_FLAG : 0);
		
		// 字段4: stringsOffset（字符串数据起始偏移）
		// stringsOffset是相对于StringPool chunk起始的偏移（包括chunk header 8字节）
		// 正确计算：chunkHeader(8) + headerSize(20) + stringOffsets(N*4) + styleOffsets(M*4)
		int chunkHeaderSize = 8;  // StringPool chunk header (type + size)
		int headerSize = 5 * 4;  // 5个int字段 = 20字节
		int stringOffsetsSize = this.size() * 4;
		int styleOffsetsSize = styleItems.size() * 4;
		int stringsDataOffset = chunkHeaderSize + headerSize + stringOffsetsSize + styleOffsetsSize;
		out.putInt(stringsDataOffset);
		
		// 字段5: stylesOffset（样式数据起始偏移，如果没有样式则为0）
		int stylesOffset = 0;
		if (styleData != null && styleData.length > 0) {
			int stringDataSize = stringData != null ? stringData.length : 0;
			stylesOffset = stringsDataOffset + stringDataSize;  // 也是相对于chunk起始
		}
		out.putInt(stylesOffset);
		
		// 写入字符串偏移数组
		for (StringItem item : this) {
			out.putInt(item.dataOffset);
		}
		
		// 写入样式偏移数组（如果存在）
		for (StringItem styleItem : styleItems) {
			out.putInt(styleItem.dataOffset);
		}
		
		// 写入字符串数据
		if (stringData != null && stringData.length > 0) {
			out.put(stringData);
		}
		
		// 写入样式数据（如果存在，修复问题2）
		if (styleData != null && styleData.length > 0) {
			for (int style : styleData) {
				out.putInt(style);
			}
		}
	}
	
	/**
	 * 获取原始flags
	 * @return 原始ResStringPool_header.flags
	 */
	public int getOriginalFlags() {
		return this.originalFlags;
	}
	
	/**
	 * 设置原始flags（用于手动传递编码信息）
	 * @param flags 原始ResStringPool_header.flags
	 */
	public void setOriginalFlags(int flags) {
		this.originalFlags = flags;
		this.hasOriginalFlags = true;
	}
	
	/**
	 * 强制使用指定编码格式（覆盖原始编码）
	 * 此方法会设置originalFlags，使prepare()使用指定的编码
	 * 
	 * @param forceUtf8 true=强制UTF-8, false=强制UTF-16
	 */
	public void setForceEncoding(boolean forceUtf8) {
		// 设置originalFlags而不是直接设置useUTF8
		// 这样prepare()会根据originalFlags正确初始化useUTF8
		this.originalFlags = forceUtf8 ? UTF8_FLAG : 0;
		this.hasOriginalFlags = true;
	}
}
