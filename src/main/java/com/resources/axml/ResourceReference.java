package com.resources.axml;

/**
 * 资源引用类 - 用于保存TYPE_REFERENCE类型的完整信息
 * 
 * @author Manifest Builder Team
 * @version 1.0.0
 */
public class ResourceReference {
    private final int value;
    private final String rawString;
    
    public ResourceReference(int value) {
        this.value = value;
        this.rawString = null;
    }
    
    public ResourceReference(int value, String rawString) {
        this.value = value;
        this.rawString = rawString;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getRawString() {
        return rawString;
    }
    
    /**
     * 获取资源引用的十六进制表示
     */
    public String getHexValue() {
        return "0x" + Integer.toHexString(value);
    }
    
    /**
     * 获取@格式的资源引用
     */
    public String getAtReference() {
        return "@" + getHexValue();
    }
    
    @Override
    public String toString() {
        return rawString != null ? rawString : getAtReference();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResourceReference that = (ResourceReference) obj;
        return value == that.value;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}
