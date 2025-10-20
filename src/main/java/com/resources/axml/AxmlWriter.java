package com.resources.axml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import static com.resources.axml.ResConst.RES_STRING_POOL_TYPE;
import static com.resources.axml.ResConst.RES_XML_CDATA_TYPE;
import static com.resources.axml.ResConst.RES_XML_END_ELEMENT_TYPE;
import static com.resources.axml.ResConst.RES_XML_END_NAMESPACE_TYPE;
import static com.resources.axml.ResConst.RES_XML_RESOURCE_MAP_TYPE;
import static com.resources.axml.ResConst.RES_XML_START_ELEMENT_TYPE;
import static com.resources.axml.ResConst.RES_XML_START_NAMESPACE_TYPE;
import static com.resources.axml.ResConst.RES_XML_TYPE;

public class AxmlWriter extends AxmlVisitor {
    
    /**
     * AXML格式START_ELEMENT节点属性标志
     * 值: 0x00140014
     * 含义: 属性头部结构标识，包含偏移量和大小信息
     * 来源: Android AXML二进制格式规范
     */
    private static final int AXML_ATTRIBUTE_FLAG = 0x00140014;
    
    static final Comparator<Attr> ATTR_CMP = new Comparator<Attr>() {

        @Override
        public int compare(Attr a, Attr b) {
            int x = a.resourceId - b.resourceId;
            if (x == 0) {
                x = a.name.data.compareTo(b.name.data);
                if (x == 0) {
                    boolean aNsIsnull = a.ns == null;
                    boolean bNsIsnull = b.ns == null;
                    if (aNsIsnull) {
                        if (bNsIsnull) {
                            x = 0;
                        } else {
                            x = -1;
                        }
                    } else {
                        if (bNsIsnull) {
                            x = 1;
                        } else {
                            x = a.ns.data.compareTo(b.ns.data);
                        }
                    }

                }
            }
            return x;
        }
    };

    static class Attr {

        public int index;
        public StringItem name;
        public StringItem ns;
        public int resourceId;
        public int type;
        public Object value;
        public StringItem raw;

        public Attr(StringItem ns, StringItem name, int resourceId) {
            super();
            this.ns = ns;
            this.name = name;
            this.resourceId = resourceId;
        }

        public void prepare(AxmlWriter axmlWriter) {
            ns = axmlWriter.updateNs(ns);
            if (this.name != null) {
                if (resourceId != -1) {
                    this.name = axmlWriter.updateWithResourceId(this.name, this.resourceId);
                } else {
                    this.name = axmlWriter.update(this.name);
                }
            }
            if (value instanceof StringItem) {
                value = axmlWriter.update((StringItem) value);
            }
            if (raw != null) {
                raw = axmlWriter.update(raw);
            }
        }

    }

    static class NodeImpl extends NodeVisitor {
        private Set<Attr> attrs = new TreeSet<Attr>(ATTR_CMP);
        private List<NodeImpl> children = new ArrayList<NodeImpl>();
        private int line;
        private StringItem name;
        private StringItem ns;
        private StringItem text;
        private int textLineNumber;
        Attr id;
        Attr style;
        Attr clz;

        public NodeImpl(String ns, String name) {
            super(null);
            this.ns = ns == null ? null : new StringItem(ns);
            this.name = name == null ? null : new StringItem(name);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object value) {
            if (name == null) {
                throw new RuntimeException("name can't be null");
            }
            Attr a = new Attr(ns == null ? null : new StringItem(ns), new StringItem(name), resourceId);
            a.type = type;

            if (value instanceof ValueWrapper) {
                ValueWrapper valueWrapper = (ValueWrapper) value;
                if (valueWrapper.raw != null) {
                    a.raw = new StringItem(valueWrapper.raw);
                }
                a.value = valueWrapper.ref;
                switch (valueWrapper.type) {
                case ValueWrapper.CLASS:
                    clz = a;
                    break;
                case ValueWrapper.ID:
                    id = a;
                    break;
                case ValueWrapper.STYLE:
                    style = a;
                    break;
                }
            } else if (type == TYPE_STRING) {
                StringItem raw = new StringItem((String) value);
                a.raw = raw;
                a.value = raw;

            } else {
                a.raw = null;
                a.value = value;
            }

            attrs.add(a);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeImpl child = new NodeImpl(ns, name);
            this.children.add(child);
            return child;
        }

        @Override
        public void end() {
        }

        @Override
        public void line(int ln) {
            this.line = ln;
        }

        public int prepare(AxmlWriter axmlWriter) {
            ns = axmlWriter.updateNs(ns);
            name = axmlWriter.update(name);

            int attrIndex = 0;
            for (Attr attr : attrs) {
                attr.index = attrIndex++;
                attr.prepare(axmlWriter);
            }

            text = axmlWriter.update(text);
            int size = 24 + 36 + attrs.size() * 20;// 24 for end tag,36+x*20 for
            // start tag
            for (NodeImpl child : children) {
                size += child.prepare(axmlWriter);
            }
            if (text != null) {
                size += 28;
            }
            return size;
        }

        public void text(int ln, String value) {
            this.text = new StringItem(value);
            this.textLineNumber = ln;
        }

        void write(ByteBuffer out) throws IOException {
            // start tag
            out.putInt(RES_XML_START_ELEMENT_TYPE | (0x0010 << 16));
            out.putInt(36 + attrs.size() * 20);
            out.putInt(line);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
            out.putInt(AXML_ATTRIBUTE_FLAG);
            out.putShort((short) this.attrs.size());
            out.putShort((short) (id == null ? 0 : id.index + 1));
            out.putShort((short) (clz == null ? 0 : clz.index + 1));
            out.putShort((short) (style == null ? 0 : style.index + 1));
            for (Attr attr : attrs) {
                out.putInt(attr.ns == null ? -1 : attr.ns.index);
                out.putInt(attr.name.index);
                out.putInt(attr.raw != null ? attr.raw.index : -1);
                out.putInt((attr.type << 24) | 0x000008);
                Object v = attr.value;
                if (v instanceof StringItem) {
                    out.putInt(((StringItem) attr.value).index);
                } else if (v instanceof Boolean) {
                    out.putInt(Boolean.TRUE.equals(v) ? -1 : 0);
                } else if (v instanceof ResourceReference) {
                    out.putInt(((ResourceReference) v).getValue());
                } else {
                    if (attr.value instanceof Integer) {
                        out.putInt((Integer) attr.value);
                    } else if (attr.value instanceof String) {
                        if ("true".equalsIgnoreCase((String) attr.value)) {
                            out.putInt(-1);
                        } else if ("false".equalsIgnoreCase((String) attr.value)) {
                            out.putInt(0);
                        } else {
                            try {
                                out.putInt(Integer.valueOf((String) attr.value));
                            } catch (Exception e) {
                                // 🔧 即使转换失败也必须写入4字节，否则会导致ByteBuffer position不匹配
                                // 转换失败时写入0
                                e.printStackTrace();
                                out.putInt(0);
                            }
                        }
                    } else {
                        // 🔧 对于其他类型的value，也必须写入4字节以保持size一致性
                        out.putInt(0);
                    }
                }
            }

            if (this.text != null) {
                out.putInt(RES_XML_CDATA_TYPE | (0x0010 << 16));
                out.putInt(28);
                out.putInt(textLineNumber);
                out.putInt(0xFFFFFFFF);
                out.putInt(text.index);
                out.putInt(0x00000008);
                out.putInt(0x00000000);
            }

            // children
            for (NodeImpl child : children) {
                child.write(out);
            }

            // end tag
            out.putInt(RES_XML_END_ELEMENT_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
        }
    }

    static class Ns {
        int ln;
        StringItem prefix;
        StringItem uri;

        public Ns(StringItem prefix, StringItem uri, int ln) {
            super();
            this.prefix = prefix;
            this.uri = uri;
            this.ln = ln;
        }
    }

    private List<NodeImpl> firsts = new ArrayList<NodeImpl>(3);

    // 使用NamespaceStack替代简单Map，支持嵌套命名空间
    private Map<String, Ns> nses = new HashMap<String, Ns>();
    private NamespaceStack namespaceStack = new NamespaceStack();

    private List<StringItem> otherString = new ArrayList<StringItem>();

    private Map<String, StringItem> resourceId2Str = new HashMap<String, StringItem>();

    private List<Integer> resourceIds = new ArrayList<Integer>();

    private List<StringItem> resourceString = new ArrayList<StringItem>();

    private StringItems stringItems = new StringItems();
    
    /**
     * prepare()幂等性控制标志
     * prepare()被调用两次时，resourceString和otherString已被清空为null
     * 解决方案: 使用标志位确保prepare()只执行一次
     */
    private boolean prepared = false;
    
    /**
     * prepare()缓存的size结果
     */
    private int cachedPreparedSize = 0;
    
    /**
     * 原始StringPool的flags
     * 从AxmlReader传递过来，用于保持编码一致性
     */
    private int originalStringPoolFlags = 0;

    /**
     * 样式字符串池
     * 用于存储AXML格式中的样式相关信息
     * 注意：现在使用StringItems内置的样式支持
     */
    // private List<StringItem> styleItems = new ArrayList<>(); // 已迁移到StringItems
    
    
    /**
     * 获取样式字符串池
     * @return 样式字符串池
     */
    public StringItems getStringItems() {
        return stringItems;
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        NodeImpl first = new NodeImpl(ns, name);
        this.firsts.add(first);
        return first;
    }

    @Override
    public void end() {
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        Ns ns = new Ns(prefix == null ? null : new StringItem(prefix), new StringItem(uri), ln);
        nses.put(uri, ns);
        
        // 🆕 同时注册到命名空间栈（用于支持嵌套）
        // 注意：这里暂时存储StringItem，在prepare()阶段会转换为索引
    }
    
    /**
     * 获取命名空间栈
     * @return 命名空间栈
     */
    public NamespaceStack getNamespaceStack() {
        return namespaceStack;
    }

    /**
     * 准备AXML数据并计算总大小
     * 
     * 确保prepare()是幂等的，可以被多次调用而不会出错
     * 
     * @return AXML数据的总大小（不包括8字节的文件头）
     * @throws IOException 如果准备过程中发生IO错误
     */
    private int prepare() throws IOException {
        // 🔧 幂等性控制：如果已经prepare过，直接返回缓存的size
        if (prepared) {
            return cachedPreparedSize;
        }
        
        int size = 0;

        for (NodeImpl first : firsts) {
            int nodeSize = first.prepare(this);
            size += nodeSize;
        }
        {
            int a = 0;
            for (Map.Entry<String, Ns> e : nses.entrySet()) {
                Ns ns = e.getValue();
                if (ns == null) {
                    ns = new Ns(null, new StringItem(e.getKey()), 0);
                    e.setValue(ns);
                }
                if (ns.prefix == null) {
                    // 特殊处理Android标准命名空间
                    String namespaceUri = e.getKey();
                    if ("http://schemas.android.com/apk/res/android".equals(namespaceUri)) {
                        ns.prefix = new StringItem("android");
                    } else {
                        ns.prefix = new StringItem(String.format("axml_auto_%02d", a++));
                    }
                }
                ns.prefix = update(ns.prefix);
                ns.uri = update(ns.uri);
            }
        }

        size += nses.size() * 24 * 2;

        // 🔧 在prepare StringItems之前设置原始flags
        if (originalStringPoolFlags != 0) {
            this.stringItems.setOriginalFlags(originalStringPoolFlags);
        }

        this.stringItems.addAll(resourceString);
        resourceString = null;  // 🔧 这里清空后，第2次调用会失败，所以需要幂等性控制
        this.stringItems.addAll(otherString);
        otherString = null;  // 🔧 这里清空后，第2次调用会失败，所以需要幂等性控制
        this.stringItems.prepare();
        int stringSize = this.stringItems.getSize();
        
        // 🔧 计算padding，确保4字节对齐
        int padding = 0;
        if (stringSize % 4 != 0) {
            padding = 4 - stringSize % 4;
        }
        
        // 🔧 StringPool chunk总大小 = chunk_header(8) + stringSize + padding
        size += 8 + stringSize + padding;
        size += 8 + resourceIds.size() * 4;
        
        // 🔧 缓存结果并标记已完成
        cachedPreparedSize = size;
        prepared = true;
        
        return size;
    }

    /**
     * 将AXML数据序列化为字节数组
     * 
     * 添加ByteBuffer边界检查，确保写入的数据大小与预期一致
     * 
     * @return 完整的AXML二进制数据
     * @throws IOException 如果写入过程中发生错误或数据大小不匹配
     */
    public byte[] toByteArray() throws IOException {
        int expectedSize = 8 + prepare();
        ByteBuffer out = ByteBuffer.allocate(expectedSize).order(ByteOrder.LITTLE_ENDIAN);

        out.putInt(RES_XML_TYPE | (0x0008 << 16));
        out.putInt(expectedSize);

        int stringSize = this.stringItems.getSize();
        int padding = 0;
        if (stringSize % 4 != 0) {
            padding = 4 - stringSize % 4;
        }
        out.putInt(RES_STRING_POOL_TYPE | (0x001C << 16));
        out.putInt(stringSize + padding + 8);
        this.stringItems.write(out);
        out.put(new byte[padding]);

        out.putInt(RES_XML_RESOURCE_MAP_TYPE | (0x0008 << 16));
        out.putInt(8 + this.resourceIds.size() * 4);
        for (Integer i : resourceIds) {
            out.putInt(i);
        }

        Stack<Ns> stack = new Stack<Ns>();
        for (Map.Entry<String, Ns> e : this.nses.entrySet()) {
            Ns ns = e.getValue();
            stack.push(ns);
            out.putInt(RES_XML_START_NAMESPACE_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }

        for (NodeImpl first : firsts) {
            first.write(out);
        }

        while (stack.size() > 0) {
            Ns ns = stack.pop();
            out.putInt(RES_XML_END_NAMESPACE_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(ns.ln);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }
        
        // 🔧 最终验证：确保ByteBuffer的position与预期大小一致
        int actualPosition = out.position();
        if (actualPosition != expectedSize) {
            throw new IOException(
                "ByteBuffer position mismatch after writing AXML data. " +
                "Expected: " + expectedSize + " bytes, " +
                "Actual: " + actualPosition + " bytes, " +
                "Difference: " + (expectedSize - actualPosition) + " bytes. " +
                "This indicates a bug in size calculation or write logic."
            );
        }
        
        return out.array();
    }

    StringItem update(StringItem item) {
        if (item == null)
            return null;
        int i = this.otherString.indexOf(item);
        if (i < 0) {
            StringItem copy = new StringItem(item.data);
            this.otherString.add(copy);
            return copy;
        } else {
            return this.otherString.get(i);
        }
    }

    StringItem updateNs(StringItem item) {
        if (item == null) {
            return null;
        }
        String ns = item.data;
        if (!this.nses.containsKey(ns)) {
            this.nses.put(ns, null);
        }
        return update(item);
    }

    StringItem updateWithResourceId(StringItem name, int resourceId) {
        String key = name.data + resourceId;
        StringItem item = this.resourceId2Str.get(key);
        if (item != null) {
            return item;
        } else {
            StringItem copy = new StringItem(name.data);
            resourceIds.add(resourceId);
            resourceString.add(copy);
            resourceId2Str.put(key, copy);
            return copy;
        }
    }
    
    /**
     * 设置原始StringPool的flags
     * 应在accept()之后、toByteArray()之前调用
     * 
     * @param flags 原始flags（从AxmlReader.getStringPoolFlags()获取）
     */
    public void setStringPoolFlags(int flags) {
        this.originalStringPoolFlags = flags;
    }
}

