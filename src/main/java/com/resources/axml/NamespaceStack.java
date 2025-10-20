package com.resources.axml;

/**
 * NamespaceStack - 命名空间栈管理器
 * 
 * 支持嵌套命名空间的正确管理（同一URI在不同深度可以有不同prefix）
 * 
 * 数据结构：
 * - mData: 存储 [prefix, uri, prefix, uri, ...] 交替的int索引
 * - mDepth: 当前栈深度
 * - mDataLength: mData数组中已使用的长度
 *
 * @author Resources Processor Team
 */
public class NamespaceStack {
    /**
     * 命名空间数据数组
     * 存储格式：[prefix0, uri0, prefix1, uri1, ...]
     * 每个命名空间占用2个int（prefix索引和uri索引）
     */
    private int[] mData;
    
    /**
     * 当前栈深度
     * 每次push增加1，每次pop减少1
     */
    private int mDepth;
    
    /**
     * mData数组中已使用的长度
     * 指向下一个可用位置
     */
    private int mDataLength;
    
    /**
     * 每个深度层级的命名空间计数
     * mCount[depth] = 该深度的命名空间数量
     */
    private int[] mCount;

    /**
     * 构造函数
     * 初始化容量为32个int（可存储16个命名空间）
     */
    public NamespaceStack() {
        mData = new int[32];
        mCount = new int[16];
        mDepth = 0;
        mDataLength = 0;
    }

    /**
     * 重置栈状态
     */
    public void reset() {
        mDataLength = 0;
        mDepth = 0;
    }

    /**
     * 获取当前深度
     * @return 当前深度
     */
    public int getDepth() {
        return mDepth;
    }

    /**
     * 获取当前深度的命名空间数量
     * @return 当前深度的命名空间数量
     */
    public int getCurrentCount() {
        if (mDepth == 0) {
            return 0;
        }
        return mCount[mDepth - 1];
    }

    /**
     * 获取累计到指定深度的命名空间总数
     * @param depth 深度
     * @return 累计命名空间数量
     */
    public int getAccumulatedCount(int depth) {
        if (mDepth == 0 || depth < 0) {
            return 0;
        }
        if (depth > mDepth) {
            depth = mDepth;
        }
        int count = 0;
        for (int i = 0; i < depth; i++) {
            count += mCount[i];
        }
        return count;
    }

    /**
     * 增加深度（进入新的元素）
     */
    public void increaseDepth() {
        ensureCountCapacity();
        mCount[mDepth] = 0;
        mDepth++;
    }

    /**
     * 减少深度（离开元素）
     * 同时移除该深度的所有命名空间
     */
    public void decreaseDepth() {
        if (mDepth == 0) {
            return;
        }
        mDepth--;
        int count = mCount[mDepth];
        if (count == 0) {
            return;
        }
        // 移除该深度的所有命名空间
        // 每个命名空间占用2个int
        mDataLength -= count * 2;
        mCount[mDepth] = 0;
    }

    /**
     * 压入一个命名空间
     * @param prefix 前缀索引
     * @param uri URI索引
     */
    public void push(int prefix, int uri) {
        if (mDepth == 0) {
            return;
        }
        ensureDataCapacity();
        int base = mDataLength;
        mData[base] = prefix;
        mData[base + 1] = uri;
        mDataLength += 2;
        mCount[mDepth - 1]++;
    }

    /**
     * 弹出最后一个命名空间（已由decreaseDepth处理）
     */
    public void pop() {
        // 空实现，实际的pop逻辑在decreaseDepth中
        // 保留此方法以保持API兼容性
    }

    /**
     * 根据URI索引查找对应的前缀索引
     * 从栈顶向下查找（最近的优先）
     * 
     * @param uri URI索引
     * @return 前缀索引，未找到返回-1
     */
    public int findPrefix(int uri) {
        // 从后向前查找（栈顶优先）
        for (int i = mDataLength - 2; i >= 0; i -= 2) {
            if (mData[i + 1] == uri) {
                return mData[i];
            }
        }
        return -1;
    }

    /**
     * 根据URI索引查找对应的URI索引（用于验证）
     * @param uri URI索引
     * @return URI索引，未找到返回-1
     */
    public int findUri(int uri) {
        // 从后向前查找
        for (int i = mDataLength - 2; i >= 0; i -= 2) {
            if (mData[i + 1] == uri) {
                return mData[i + 1];
            }
        }
        return -1;
    }

    /**
     * 获取指定位置的前缀索引
     * @param pos 位置（从0开始）
     * @return 前缀索引
     */
    public int getPrefix(int pos) {
        if (pos < 0 || pos * 2 >= mDataLength) {
            return -1;
        }
        return mData[pos * 2];
    }

    /**
     * 获取指定位置的URI索引
     * @param pos 位置（从0开始）
     * @return URI索引
     */
    public int getUri(int pos) {
        if (pos < 0 || pos * 2 + 1 >= mDataLength) {
            return -1;
        }
        return mData[pos * 2 + 1];
    }

    /**
     * 确保mData数组有足够容量
     */
    private void ensureDataCapacity() {
        if (mDataLength + 2 > mData.length) {
            // 扩容：翻倍
            int[] newData = new int[mData.length * 2];
            System.arraycopy(mData, 0, newData, 0, mDataLength);
            mData = newData;
        }
    }

    /**
     * 确保mCount数组有足够容量
     */
    private void ensureCountCapacity() {
        if (mDepth >= mCount.length) {
            // 扩容：翻倍
            int[] newCount = new int[mCount.length * 2];
            System.arraycopy(mCount, 0, newCount, 0, mDepth);
            mCount = newCount;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NamespaceStack{depth=");
        sb.append(mDepth);
        sb.append(", dataLength=").append(mDataLength);
        sb.append(", namespaces=[");
        for (int i = 0; i < mDataLength; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("(prefix=").append(mData[i]);
            sb.append(", uri=").append(mData[i + 1]).append(")");
        }
        sb.append("]}");
        return sb.toString();
    }
}

