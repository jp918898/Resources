package com.resources.axml;

/**
 * Node访问者接口
 * 
 * @author Manifest Builder Team
 * @version 1.0.0
 */
public abstract class NodeVisitor {
    
    // 常量定义
    public static final int TYPE_STRING = 3;
    public static final int TYPE_INT_BOOLEAN = 18;
    
    protected NodeVisitor parent;
    protected NodeVisitor nv;
    
    public NodeVisitor() {
        this(null);
    }
    
    public NodeVisitor(NodeVisitor parent) {
        this.parent = parent;
        this.nv = parent;
    }
    
    /**
     * 访问子节点
     * 
     * @param ns 命名空间
     * @param name 节点名称
     * @return NodeVisitor 子节点访问者
     */
    public NodeVisitor child(String ns, String name) {
        if (nv != null) {
            return nv.child(ns, name);
        }
        return null;
    }
    
    /**
     * 访问属性
     * 
     * @param ns 命名空间
     * @param name 属性名称
     * @param resourceId 资源ID
     * @param type 类型
     * @param obj 属性值
     */
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (nv != null) {
            nv.attr(ns, name, resourceId, type, obj);  // 必须传递所有类型的属性
        }
    }
    
    /**
     * 访问属性（字符串）
     * 
     * @param ns 命名空间
     * @param name 属性名称
     * @param value 属性值
     */
    public void attribute(String ns, String name, String value) {
        // 默认实现
    }
    
    /**
     * 访问文本内容（带行号）
     * 
     * @param ln 行号
     * @param text 文本内容
     */
    public void text(int ln, String text) {
        if (nv != null) {
            nv.text(ln, text);
        }
    }
    
    /**
     * 访问文本内容（兼容旧版本）
     * 
     * @param text 文本内容
     */
    public void text(String text) {
        text(0, text);  // 委托到带行号的方法
    }
    
    /**
     * 结束当前节点
     */
    public void end() {
        if (nv != null) {
            nv.end();
        }
    }
    
    /**
     * 设置行号
     * 
     * @param line 行号
     */
    public void line(int line) {
        if (nv != null) {
            nv.line(line);
        }
    }
}