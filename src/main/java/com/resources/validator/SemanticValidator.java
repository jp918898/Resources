package com.resources.validator;

import com.resources.mapping.WhitelistFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 语义验证器 - 区分"类名/包名"vs"普通字符串"
 * 
 * 这是防止误改UI文案的关键组件
 * 
 * 验证逻辑：
 * 1. 标签名本身是FQCN？
 * 2. 属性名在类语义白名单中？
 * 3. Data Binding表达式T(FQCN)？
 * 4. 白名单过滤：系统/三方包不改
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class SemanticValidator {
    
    private static final Logger log = LoggerFactory.getLogger(SemanticValidator.class);
    
    // 类名/包名语义的属性白名单
    private static final Set<String> CLASS_SEMANTIC_ATTRS = Set.of(
        "android:name",           // Fragment/Activity/Service等
        "class",                  // 显式类名
        "android:fragment",       // Preference fragment
        "app:actionViewClass",    // 菜单Action View
        "app:actionProviderClass",// 菜单Action Provider
        "app:layoutManager",      // RecyclerView布局管理器
        "type",                   // Data Binding variable/import
        "tools:context"           // 设计时上下文
    );
    
    // Data Binding表达式模式
    private static final Pattern DATA_BINDING_EXPR_PATTERN = Pattern.compile("T\\(([a-zA-Z0-9._]+)\\)");
    
    private final WhitelistFilter whitelistFilter;
    
    public SemanticValidator(WhitelistFilter whitelistFilter) {
        this.whitelistFilter = Objects.requireNonNull(whitelistFilter, 
                                                      "whitelistFilter不能为null");
    }
    
    /**
     * 验证上下文
     */
    public static class Context {
        private final String filePath;
        private final String tagName;
        private final String attributeName;
        private final boolean isTagName;
        private final boolean isDataBindingExpression;
        
        private Context(Builder builder) {
            this.filePath = builder.filePath;
            this.tagName = builder.tagName;
            this.attributeName = builder.attributeName;
            this.isTagName = builder.isTagName;
            this.isDataBindingExpression = builder.isDataBindingExpression;
        }
        
        public String getFilePath() { return filePath; }
        public String getTagName() { return tagName; }
        public String getAttributeName() { return attributeName; }
        public boolean isTagName() { return isTagName; }
        public boolean isDataBindingExpression() { return isDataBindingExpression; }
        
        public static class Builder {
            private String filePath;
            private String tagName;
            private String attributeName;
            private boolean isTagName;
            private boolean isDataBindingExpression;
            
            public Builder filePath(String filePath) {
                this.filePath = filePath;
                return this;
            }
            
            public Builder tagName(String tagName) {
                this.tagName = tagName;
                return this;
            }
            
            public Builder attributeName(String attributeName) {
                this.attributeName = attributeName;
                return this;
            }
            
            public Builder isTagName(boolean isTagName) {
                this.isTagName = isTagName;
                return this;
            }
            
            public Builder isDataBindingExpression(boolean isDataBindingExpression) {
                this.isDataBindingExpression = isDataBindingExpression;
                return this;
            }
            
            public Context build() {
                return new Context(this);
            }
        }
    }
    
    /**
     * 验证字符串是否为类名/包名语义
     * 
     * @param context 上下文（标签名、属性名、父节点等）
     * @param value 待验证的字符串值
     * @return true=是类名/包名，false=普通字符串
     */
    public boolean isClassOrPackageSemantic(Context context, String value) {
        Objects.requireNonNull(context, "context不能为null");
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 1. 标签名本身是FQCN（形如com.example.MyView）
        if (context.isTagName() && isFullyQualifiedClassName(value)) {
            log.trace("标签名是FQCN: '{}'", value);
            return true;
        }
        
        // 2. 属性名在类语义白名单中
        if (context.getAttributeName() != null && 
            CLASS_SEMANTIC_ATTRS.contains(context.getAttributeName())) {
            log.trace("属性名在类语义白名单中: {} = '{}'", 
                     context.getAttributeName(), value);
            return true;
        }
        
        // 3. Data Binding表达式中的T(FQCN)
        if (context.isDataBindingExpression()) {
            String className = extractClassNameFromExpression(value);
            if (className != null && isFullyQualifiedClassName(className)) {
                log.trace("Data Binding表达式: '{}'", value);
                return true;
            }
        }
        
        // 4. 其他情况：可能是UI文案，不处理
        log.trace("非类名/包名语义: '{}'", value);
        return false;
    }
    
    /**
     * 验证是否为完全限定类名（FQCN）
     * 
     * 特征：
     * - 至少包含一个点号
     * - 符合Java类名规范
     * - 首字母大写（类名约定）- 可选但推荐
     * 
     * @param value 字符串值
     * @return true=是FQCN
     */
    public boolean isFullyQualifiedClassName(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 1. 至少包含一个点号
        if (!value.contains(".")) {
            return false;
        }
        
        // 2. 不能是资源引用
        if (value.startsWith("@")) {
            return false;
        }
        
        // 3. 符合Java类名规范
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return false; // 至少两个部分（如com.Example）
        }
        
        for (String part : parts) {
            if (!isValidJavaIdentifier(part)) {
                return false;
            }
        }
        
        // 4. 可选：首字母大写检查（类名约定）
        String lastName = parts[parts.length - 1];
        if (!Character.isUpperCase(lastName.charAt(0))) {
            // 可能是包名而不是类名，但仍然是有效的标识符
            log.trace("可能是包名而非类名（首字母小写）: '{}'", value);
        }
        
        return true;
    }
    
    /**
     * 验证是否为有效的Java标识符
     */
    private boolean isValidJavaIdentifier(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 从Data Binding表达式中提取类名
     * 例如：@{T(com.example.Util).method()} -> com.example.Util
     * 
     * @param expression 表达式
     * @return 类名，如果没有则返回null
     */
    private String extractClassNameFromExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        
        var matcher = DATA_BINDING_EXPR_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 白名单过滤：只处理自有包，保留系统/三方
     * 
     * @param className 类名
     * @return true=应该替换，false=不应替换
     */
    public boolean shouldReplace(String className) {
        return whitelistFilter.shouldReplace(className);
    }
    
    /**
     * 验证并过滤
     * 组合语义验证和白名单过滤
     * 
     * @param context 上下文
     * @param value 值
     * @return true=应该处理（是类名且应替换）
     */
    public boolean validateAndFilter(Context context, String value) {
        // 1. 语义验证
        if (!isClassOrPackageSemantic(context, value)) {
            log.trace("语义验证失败: '{}'", value);
            return false;
        }
        
        // 2. 白名单过滤
        if (!shouldReplace(value)) {
            log.trace("白名单过滤失败: '{}'", value);
            return false;
        }
        
        log.debug("验证通过，应该处理: '{}'", value);
        return true;
    }
    
    /**
     * 批量验证
     * 
     * @param items 待验证的项目列表（Context + Value）
     * @return 验证通过的项目
     */
    public List<Map.Entry<Context, String>> batchValidate(
            List<Map.Entry<Context, String>> items) {
        
        List<Map.Entry<Context, String>> validated = new ArrayList<>();
        
        for (Map.Entry<Context, String> item : items) {
            if (validateAndFilter(item.getKey(), item.getValue())) {
                validated.add(item);
            }
        }
        
        log.info("批量验证: 输入={}, 通过={}", items.size(), validated.size());
        return validated;
    }
    
    /**
     * 生成验证报告
     * 
     * @param items 所有项目
     * @return 报告内容
     */
    public String generateValidationReport(List<Map.Entry<Context, String>> items) {
        StringBuilder report = new StringBuilder();
        report.append("════════════════════════════════════════\n");
        report.append("  语义验证报告\n");
        report.append("════════════════════════════════════════\n");
        
        int total = items.size();
        int passed = 0;
        int semanticFailed = 0;
        int whitelistFailed = 0;
        
        for (Map.Entry<Context, String> item : items) {
            Context ctx = item.getKey();
            String value = item.getValue();
            
            boolean semantic = isClassOrPackageSemantic(ctx, value);
            boolean whitelist = semantic && shouldReplace(value);
            
            if (semantic && whitelist) {
                passed++;
                report.append(String.format("✓ [%s] %s = '%s'\n", 
                                          ctx.getFilePath(), 
                                          ctx.getAttributeName() != null ? 
                                              ctx.getAttributeName() : "TAG", 
                                          value));
            } else if (!semantic) {
                semanticFailed++;
                report.append(String.format("✗ [语义] %s = '%s'\n", 
                                          ctx.getFilePath(), value));
            } else {
                whitelistFailed++;
                report.append(String.format("⊘ [白名单] %s = '%s'\n", 
                                          ctx.getFilePath(), value));
            }
        }
        
        report.append("────────────────────────────────────────\n");
        report.append(String.format("总计: %d\n", total));
        report.append(String.format("通过: %d (%.1f%%)\n", passed, 
                                   total > 0 ? (passed * 100.0 / total) : 0));
        report.append(String.format("语义失败: %d\n", semanticFailed));
        report.append(String.format("白名单失败: %d\n", whitelistFailed));
        report.append("════════════════════════════════════════\n");
        
        return report.toString();
    }
}

