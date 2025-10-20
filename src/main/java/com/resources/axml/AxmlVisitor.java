package com.resources.axml;

/**
 * AxmlVisitor - AXML访问器
 * 从Randomization模块完整提取，保持所有功能和特性
 *
 * @author Manifest Builder Team
 */
public class AxmlVisitor extends NodeVisitor {

    public AxmlVisitor() {
        super();
    }

    public AxmlVisitor(NodeVisitor av) {
        super(av);
    }

    /**
     * create a ns
     * 
     * @param prefix
     * @param uri
     * @param ln
     */
    public void ns(String prefix, String uri, int ln) {
        if (nv != null && nv instanceof AxmlVisitor) {
            ((AxmlVisitor) nv).ns(prefix, uri, ln);
        }
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        if (nv != null) {
            return nv.child(ns, name);
        }
        return super.child(ns, name);
    }
}
