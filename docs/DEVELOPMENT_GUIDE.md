# Resources Processor - 开发指南

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 开发规范和贡献指南

---

## 📚 目录

1. [开发环境设置](#开发环境设置)
2. [代码规范](#代码规范)
3. [Git工作流](#git工作流)
4. [测试规范](#测试规范)
5. [文档规范](#文档规范)
6. [贡献流程](#贡献流程)
7. [代码审查](#代码审查)
8. [发布流程](#发布流程)

---

## 开发环境设置

### 必需工具

```bash
# Java 17+
java -version

# Git
git --version

# Gradle (使用项目自带的gradlew)
./gradlew --version

# IDE (推荐)
# - IntelliJ IDEA 2023+
# - Eclipse 2023+
```

### 克隆项目

```bash
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources
```

### 导入IDE

#### IntelliJ IDEA

1. File → Open → 选择项目根目录
2. 等待Gradle同步完成
3. 配置JDK 17: File → Project Structure → Project SDK

#### Eclipse

1. File → Import → Gradle → Existing Gradle Project
2. 选择项目根目录
3. 等待Gradle同步

### 安装Git Hooks（推荐）

创建 `.git/hooks/pre-commit`:

```bash
#!/bin/bash

# 运行测试
./gradlew test

if [ $? -ne 0 ]; then
  echo "Tests failed. Commit aborted."
  exit 1
fi

echo "All tests passed. Proceeding with commit."
```

设置可执行权限：

```bash
chmod +x .git/hooks/pre-commit
```

---

## 代码规范

### Java代码规范

#### 1. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| **类名** | PascalCase | `ResourceProcessor`, `ArscParser` |
| **接口** | PascalCase | `XmlProcessor`, `Validator` |
| **方法** | camelCase | `processApk()`, `replaceStringPool()` |
| **变量** | camelCase | `apkPath`, `globalStringPool` |
| **常量** | UPPER_SNAKE_CASE | `MAX_FILE_SIZE`, `RES_TABLE_TYPE` |
| **包名** | 小写 | `com.resources.arsc` |

#### 2. 代码格式

**缩进**: 4个空格（禁用Tab）

**示例**:
```java
public class ResourceProcessor {
    
    private final TransactionManager transactionManager;
    
    public ProcessingResult processApk(String apkPath, 
                                      ResourceConfig config) 
            throws IOException {
        
        Transaction tx = null;
        
        try {
            tx = transactionManager.beginTransaction(apkPath);
            
            ScanReport scanReport = phase1_Scan(apkPath, config);
            ValidationResult preValidation = phase2_Validate(tx, config);
            
            return resultBuilder.success(true).build();
            
        } catch (Exception e) {
            transactionManager.rollback(tx);
            throw new IOException("APK处理失败", e);
        }
    }
}
```

#### 3. 注释规范

**类注释**:
```java
/**
 * resources.arsc 完整解析器
 * 
 * 解析AAPT2生成的完整resources.arsc文件，包括：
 * - ResTable header
 * - 全局字符串池
 * - 资源包（一个或多个）
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 */
public class ArscParser {
    // ...
}
```

**方法注释**:
```java
/**
 * 解析resources.arsc文件
 * 
 * @param data resources.arsc文件的完整字节数据
 * @throws IllegalArgumentException 解析失败
 */
public void parse(byte[] data) throws IllegalArgumentException {
    // ...
}
```

**行内注释**:
```java
// 1. 解析ResTable头
int type = buffer.getShort() & 0xFFFF;

// 2. 解析全局字符串池
globalStringPool = parseStringPool(buffer);
```

#### 4. 异常处理

**规范**:
- 不要吞掉异常
- 记录日志
- 抛出有意义的异常

**良好示例**:
```java
public void processFile(String path) throws IOException {
    try {
        byte[] data = Files.readAllBytes(Paths.get(path));
        process(data);
    } catch (IOException e) {
        log.error("文件处理失败: {}", path, e);
        throw new IOException("无法处理文件: " + path, e);
    }
}
```

**不良示例**:
```java
// ❌ 吞掉异常
try {
    process(data);
} catch (Exception e) {
    // 什么都不做
}

// ❌ 泛泛的异常
throw new Exception("error");

// ❌ 打印堆栈
e.printStackTrace();
```

#### 5. 日志规范

**日志级别**:
```java
log.trace("详细的调试信息");        // TRACE: 最详细
log.debug("调试信息: {}", value);   // DEBUG: 调试
log.info("处理APK: {}", apkPath);  // INFO: 正常流程
log.warn("配置缺失，使用默认值");    // WARN: 警告
log.error("处理失败: {}", msg, e); // ERROR: 错误
```

**参数化日志**:
```java
// ✅ 正确：使用占位符
log.info("处理APK: {}, 大小: {}", apkPath, size);

// ❌ 错误：字符串拼接
log.info("处理APK: " + apkPath + ", 大小: " + size);
```

#### 6. 空值检查

**使用Objects.requireNonNull**:
```java
public ResourceProcessor(TransactionManager transactionManager) {
    this.transactionManager = Objects.requireNonNull(
        transactionManager, "transactionManager不能为null");
}
```

**Optional使用**:
```java
public Optional<String> findMapping(String oldClass) {
    return Optional.ofNullable(mappings.get(oldClass));
}
```

#### 7. 资源管理

**使用try-with-resources**:
```java
// ✅ 正确
try (ZipFile zipFile = new ZipFile(apkPath)) {
    // 处理ZIP文件
}

// ❌ 错误
ZipFile zipFile = new ZipFile(apkPath);
// 处理ZIP文件
zipFile.close();  // 可能不会执行
```

---

### 包结构规范

```
com.resources/
├── cli/               # 命令行接口
├── core/              # 核心处理器
├── arsc/              # ARSC处理
├── axml/              # AXML处理
├── scanner/           # 扫描器
├── transaction/       # 事务管理
├── validator/         # 验证器
├── mapping/           # 映射管理
├── model/             # 数据模型
├── report/            # 报告生成
├── config/            # 配置管理
└── util/              # 工具类
```

**规则**:
- 每个包一个职责
- 包之间避免循环依赖
- 公共类放在util包

---

## Git工作流

### 分支策略

采用**Git Flow**模型：

```
main          # 生产分支（稳定版本）
  ↓
develop       # 开发分支（最新代码）
  ↓
feature/*     # 功能分支
bugfix/*      # Bug修复分支
hotfix/*      # 紧急修复分支
release/*     # 发布分支
```

### 分支命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 功能 | `feature/<功能名>` | `feature/dex-cache` |
| Bug修复 | `bugfix/<问题描述>` | `bugfix/arsc-parsing-error` |
| 紧急修复 | `hotfix/<问题描述>` | `hotfix/memory-leak` |
| 发布 | `release/v<版本号>` | `release/v1.0.1` |

### 提交信息规范

**格式**: `<type>(<scope>): <subject>`

**类型**:
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 添加测试
- `chore`: 构建/工具变更

**示例**:
```bash
feat(arsc): 添加字符串池LRU缓存
fix(axml): 修复Layout处理器空指针异常
docs(readme): 更新安装说明
refactor(core): 简化Phase3替换逻辑
test(arsc): 添加ArscParser边界测试
chore(build): 升级Gradle到8.5
```

### 开发流程

#### 1. 创建功能分支

```bash
# 从develop分支创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/my-feature
```

#### 2. 开发和提交

```bash
# 频繁提交，小步前进
git add .
git commit -m "feat(arsc): 实现字符串池缓存基础结构"

git add .
git commit -m "feat(arsc): 添加LRU淘汰策略"

git add .
git commit -m "test(arsc): 添加缓存命中率测试"
```

#### 3. 同步develop分支

```bash
# 定期同步develop分支的最新代码
git checkout develop
git pull origin develop
git checkout feature/my-feature
git rebase develop
```

#### 4. 推送分支

```bash
git push origin feature/my-feature
```

#### 5. 创建Pull Request

1. 访问GitHub/GitLab仓库
2. 点击"New Pull Request"
3. 选择 `feature/my-feature` → `develop`
4. 填写PR描述（参见下文）
5. 请求代码审查

#### 6. 合并分支

代码审查通过后：

```bash
# 合并到develop（使用Squash Merge）
git checkout develop
git pull origin develop
git merge --squash feature/my-feature
git commit -m "feat(arsc): 添加字符串池LRU缓存 (#123)"
git push origin develop

# 删除功能分支
git branch -d feature/my-feature
git push origin --delete feature/my-feature
```

---

## 测试规范

### 测试原则

1. **覆盖率目标**: 85%+
2. **测试金字塔**: 70%单元测试 + 20%集成测试 + 10%端到端测试
3. **测试独立性**: 每个测试独立运行，不依赖执行顺序
4. **测试可重复性**: 多次运行结果一致

### 单元测试

#### 命名规范

**格式**: `test<方法名>_<场景>_<预期结果>`

**示例**:
```java
@Test
void testParse_validArsc_success() { }

@Test
void testParse_emptyData_throwsException() { }

@Test
void testReplaceStringPool_multipleReplacements_correctCount() { }
```

#### 测试结构（AAA模式）

```java
@Test
void testReplaceStringPool_multipleReplacements_correctCount() {
    // Arrange（准备）
    ResStringPool pool = new ResStringPool();
    pool.addString("com.example.MainActivity");
    pool.addString("com.example.ui.Fragment");
    
    Map<String, String> replacements = new HashMap<>();
    replacements.put("com.example", "com.test");
    
    ArscReplacer replacer = new ArscReplacer();
    
    // Act（执行）
    int count = replacer.replaceStringPool(pool, replacements);
    
    // Assert（断言）
    assertEquals(2, count);
    assertEquals("com.test.MainActivity", pool.getString(0));
    assertEquals("com.test.ui.Fragment", pool.getString(1));
}
```

#### 边界测试

```java
@Test
void testParse_emptyData_throwsException() {
    ArscParser parser = new ArscParser();
    
    assertThrows(IllegalArgumentException.class, () -> {
        parser.parse(new byte[0]);
    });
}

@Test
void testParse_nullData_throwsNullPointerException() {
    ArscParser parser = new ArscParser();
    
    assertThrows(NullPointerException.class, () -> {
        parser.parse(null);
    });
}
```

### 集成测试

**位置**: `src/test/java/com/resources/integration/`

**示例**:
```java
@Test
void testFullProcessing_realApk_success() throws IOException {
    // 1. 准备
    String inputApk = "input/Dragonfly.apk";
    String outputApk = "output/test-processed.apk";
    ResourceConfig config = ResourceConfig.loadFromYaml(
        "config/test-config.yaml");
    
    // 2. 处理
    ResourceProcessor processor = new ResourceProcessor();
    ProcessingResult result = processor.processApk(inputApk, config);
    
    // 3. 验证
    assertTrue(result.isSuccess());
    assertTrue(Files.exists(Paths.get(outputApk)));
    
    // 4. aapt2验证
    Aapt2Validator validator = new Aapt2Validator();
    ValidationResult validation = validator.validate(outputApk);
    assertTrue(validation.isOverallSuccess());
}
```

### Mock使用

```java
@Test
void testProcessApk_scannerThrowsException_rollback() {
    // 使用Mockito模拟依赖
    ResourceScanner scanner = mock(ResourceScanner.class);
    when(scanner.scanApk(anyString()))
        .thenThrow(new IOException("Scan failed"));
    
    TransactionManager txManager = mock(TransactionManager.class);
    
    ResourceProcessor processor = new ResourceProcessor(scanner, txManager);
    
    // 验证异常和回滚
    assertThrows(IOException.class, () -> {
        processor.processApk("app.apk", config);
    });
    
    verify(txManager, times(1)).rollback(any());
}
```

---

## 文档规范

### 代码文档

#### JavaDoc规范

**类文档**:
```java
/**
 * resources.arsc 完整解析器
 * 
 * <p>解析AAPT2生成的完整resources.arsc文件，包括：
 * <ul>
 *   <li>ResTable header</li>
 *   <li>全局字符串池</li>
 *   <li>资源包（一个或多个）</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * ArscParser parser = new ArscParser();
 * parser.parse(arscData);
 * ResStringPool pool = parser.getGlobalStringPool();
 * }</pre>
 * 
 * @author Resources Processor Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ArscParser {
    // ...
}
```

**方法文档**:
```java
/**
 * 解析resources.arsc文件
 * 
 * <p>解析完整的ARSC文件，提取全局字符串池和所有资源包。
 * 
 * @param data resources.arsc文件的完整字节数据
 * @throws IllegalArgumentException 如果数据为空或格式无效
 * @throws IOException 如果读取数据失败
 */
public void parse(byte[] data) throws IllegalArgumentException, IOException {
    // ...
}
```

### Markdown文档

**文档结构**:
```markdown
# 文档标题

**版本**: 1.0.0  
**更新日期**: 2025-10-20

---

## 概述

简要介绍...

## 目录

1. [章节1](#章节1)
2. [章节2](#章节2)

## 章节1

内容...

### 小节1.1

内容...
```

---

## 贡献流程

### 第一次贡献

#### 1. Fork项目

访问 https://github.com/frezrik/jiagu-resources  
点击 "Fork" 按钮

#### 2. 克隆Fork

```bash
git clone https://github.com/YOUR_USERNAME/jiagu-resources.git
cd jiagu-resources
```

#### 3. 添加上游仓库

```bash
git remote add upstream https://github.com/frezrik/jiagu-resources.git
```

#### 4. 创建分支

```bash
git checkout -b feature/my-feature
```

#### 5. 开发和测试

```bash
# 编写代码
# 运行测试
./gradlew test

# 提交代码
git add .
git commit -m "feat(xxx): 添加xxx功能"
```

#### 6. 推送分支

```bash
git push origin feature/my-feature
```

#### 7. 创建Pull Request

1. 访问您的Fork仓库
2. 点击 "New Pull Request"
3. 选择 `feature/my-feature` → `upstream/develop`
4. 填写PR模板：

```markdown
## 变更类型
- [ ] Bug修复
- [x] 新功能
- [ ] 文档更新
- [ ] 性能优化

## 变更描述
添加字符串池LRU缓存，提升DEX加载性能350倍。

## 相关Issue
Closes #123

## 测试
- [x] 单元测试通过
- [x] 集成测试通过
- [x] 手动测试通过

## 检查清单
- [x] 代码遵循项目规范
- [x] 添加了必要的测试
- [x] 更新了文档
- [x] 通过了所有CI检查
```

### Pull Request规范

#### 标题格式

`<type>(<scope>): <subject> (#issue)`

**示例**:
```
feat(arsc): 添加字符串池LRU缓存 (#123)
fix(axml): 修复Layout处理器NPE (#456)
docs(readme): 更新安装说明
```

#### PR描述模板

```markdown
## 变更类型
- [ ] Bug修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 性能优化
- [ ] 代码重构
- [ ] 测试改进

## 变更描述
简要描述本次变更的目的和实现方式。

## 相关Issue
Closes #<issue_number>
Relates to #<issue_number>

## 测试
描述测试方法和结果：
- [ ] 单元测试
- [ ] 集成测试
- [ ] 手动测试

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 添加了必要的测试
- [ ] 更新了相关文档
- [ ] 通过了所有CI检查
- [ ] 代码已经过自我审查

## 截图（如适用）
添加截图帮助说明变更。
```

---

## 代码审查

### 审查检查清单

#### 代码质量
- [ ] 代码逻辑清晰，易于理解
- [ ] 命名规范，有意义
- [ ] 无重复代码
- [ ] 无过长的方法（>50行需拆分）
- [ ] 无过深的嵌套（>3层需重构）

#### 功能正确性
- [ ] 功能符合需求
- [ ] 边界条件处理正确
- [ ] 异常处理完善
- [ ] 无明显Bug

#### 测试
- [ ] 包含单元测试
- [ ] 测试覆盖率>=85%
- [ ] 测试通过
- [ ] 边界测试充分

#### 文档
- [ ] JavaDoc完整
- [ ] 复杂逻辑有注释
- [ ] 更新了README（如需要）
- [ ] 更新了CHANGELOG（如需要）

#### 性能
- [ ] 无明显性能问题
- [ ] 资源正确释放
- [ ] 无内存泄漏

### 审查评论规范

**建议性评论**:
```
💡 建议：这里可以使用Optional来避免空指针。
```

**必须修改**:
```
⚠️ 必须修改：这里存在内存泄漏，需要在finally中关闭资源。
```

**点赞**:
```
👍 很好的实现，代码清晰易懂。
```

---

## 发布流程

### 版本号规范

采用**语义化版本**（Semantic Versioning）：

**格式**: `MAJOR.MINOR.PATCH`

**规则**:
- `MAJOR`: 不兼容的API变更
- `MINOR`: 向后兼容的功能新增
- `PATCH`: 向后兼容的Bug修复

**示例**:
- `1.0.0` → `1.0.1`: Bug修复
- `1.0.1` → `1.1.0`: 新功能
- `1.1.0` → `2.0.0`: 破坏性变更

### 发布步骤

参见 [BUILD_AND_RUN.md - 发布流程](BUILD_AND_RUN.md#发布流程)

---

## 最佳实践

### 1. 小步提交

```bash
# ✅ 好：每个功能点独立提交
git commit -m "feat(arsc): 添加字符串池类"
git commit -m "feat(arsc): 实现LRU淘汰策略"
git commit -m "test(arsc): 添加缓存测试"

# ❌ 差：一次性提交所有变更
git commit -m "feat(arsc): 完成缓存功能"
```

### 2. 测试驱动开发（TDD）

```
1. 编写测试（Red）
2. 实现功能（Green）
3. 重构代码（Refactor）
4. 重复
```

### 3. 代码审查

- 每个PR必须经过至少1人审查
- 审查者应运行代码并测试
- 发现问题及时沟通

### 4. 持续集成

- 每次提交触发CI构建
- 所有测试必须通过
- 代码覆盖率不能降低

---

**文档版本**: 1.0.0  
**最后更新**: 2025-10-20  
**维护者**: Resources Processor Team

