# Resources Processor - API参考文档

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 公共API参考手册

---

## 📚 目录

1. [概述](#概述)
2. [核心API](#核心api)
3. [配置API](#配置api)
4. [模型API](#模型api)
5. [工具API](#工具api)
6. [验证器API](#验证器api)
7. [使用示例](#使用示例)

---

## 概述

### 包结构

```
com.resources
├── core                 # 核心处理器
├── config               # 配置管理
├── model                # 数据模型
├── arsc                 # ARSC处理
├── axml                 # AXML处理
├── scanner              # 扫描器
├── transaction          # 事务管理
├── validator            # 验证器
├── mapping              # 映射管理
├── report               # 报告生成
└── util                 # 工具类
```

### API分类

| 类别 | 包 | 说明 |
|------|----|----|
| **核心** | `core` | 主处理逻辑 |
| **配置** | `config` | 配置加载和管理 |
| **模型** | `model` | 数据模型类 |
| **工具** | `util` | 工具类 |
| **验证** | `validator` | 验证器 |

---

## 核心API

### ResourceProcessor

**包**: `com.resources.core`

**用途**: 主处理器，协调所有处理流程

#### 构造方法

```java
public ResourceProcessor()
```

创建默认的ResourceProcessor实例。

**示例**:
```java
ResourceProcessor processor = new ResourceProcessor();
```

#### processApk

```java
public ProcessingResult processApk(String apkPath, ResourceConfig config) 
        throws IOException
```

处理APK文件。

**参数**:
- `apkPath` - APK文件路径
- `config` - 资源配置

**返回**: `ProcessingResult` - 处理结果

**抛出**:
- `IOException` - 处理失败

**示例**:
```java
ResourceProcessor processor = new ResourceProcessor();
ResourceConfig config = ResourceConfig.loadFromYaml("config.yaml");
ProcessingResult result = processor.processApk("input/app.apk", config);

if (result.isSuccess()) {
    System.out.println("处理成功");
} else {
    System.out.println("处理失败: " + result.getErrors());
}
```

---

## 配置API

### ResourceConfig

**包**: `com.resources.config`

**用途**: 资源处理配置

#### 静态方法

##### loadFromYaml

```java
public static ResourceConfig loadFromYaml(String path) throws IOException
```

从YAML文件加载配置。

**参数**:
- `path` - YAML文件路径

**返回**: `ResourceConfig` - 配置对象

**抛出**:
- `IOException` - 加载失败

**示例**:
```java
ResourceConfig config = ResourceConfig.loadFromYaml("config/test-config.yaml");
```

#### 实例方法

##### getPackageMappings

```java
public PackageMapping getPackageMappings()
```

获取包名映射。

**返回**: `PackageMapping` - 包名映射对象

##### getClassMappings

```java
public ClassMapping getClassMappings()
```

获取类名映射。

**返回**: `ClassMapping` - 类名映射对象

##### getOwnPackagePrefixes

```java
public Set<String> getOwnPackagePrefixes()
```

获取自有包前缀集合。

**返回**: 不可变的包前缀集合

##### getDexPaths

```java
public List<String> getDexPaths()
```

获取DEX文件路径列表。

**返回**: 不可变的DEX路径列表

##### toBuilder

```java
public Builder toBuilder()
```

转换为Builder以修改配置。

**返回**: `Builder` - 包含当前配置的Builder

**示例**:
```java
ResourceConfig config = ResourceConfig.loadFromYaml("config.yaml");

// 添加额外的DEX路径
ResourceConfig.Builder builder = config.toBuilder();
builder.addDexPath("classes2.dex");
ResourceConfig newConfig = builder.build();
```

#### Builder类

##### Builder

```java
public Builder()
```

创建空Builder。

##### addPackageMapping

```java
public Builder addPackageMapping(String oldPkg, String newPkg)
```

添加包名映射。

**参数**:
- `oldPkg` - 旧包名
- `newPkg` - 新包名

**返回**: Builder实例（链式调用）

##### addClassMapping

```java
public Builder addClassMapping(String oldClass, String newClass)
```

添加类名映射。

**参数**:
- `oldClass` - 旧类名（完全限定）
- `newClass` - 新类名（完全限定）

**返回**: Builder实例

##### addOwnPackagePrefix

```java
public Builder addOwnPackagePrefix(String prefix)
```

添加自有包前缀。

**参数**:
- `prefix` - 包前缀（如 "com.myapp"）

**返回**: Builder实例

##### addDexPath

```java
public Builder addDexPath(String path)
```

添加DEX文件路径。

**参数**:
- `path` - DEX文件路径

**返回**: Builder实例

##### build

```java
public ResourceConfig build()
```

构建ResourceConfig对象。

**返回**: `ResourceConfig` - 配置对象

**示例**:
```java
ResourceConfig config = new ResourceConfig.Builder()
    .addOwnPackagePrefix("com.example")
    .addPackageMapping("com.example", "com.test")
    .addClassMapping("com.example.MainActivity", "com.test.MainActivity")
    .addDexPath("input/classes.dex")
    .build();
```

---

## 模型API

### ProcessingResult

**包**: `com.resources.model`

**用途**: 处理结果封装

#### 实例方法

##### isSuccess

```java
public boolean isSuccess()
```

判断处理是否成功。

**返回**: `true` 如果成功

##### getTotalModifications

```java
public int getTotalModifications()
```

获取修改次数。

**返回**: 修改次数

##### getErrors

```java
public List<String> getErrors()
```

获取错误列表。

**返回**: 错误信息列表

##### getSummary

```java
public String getSummary()
```

获取处理摘要。

**返回**: 格式化的摘要字符串

**示例**:
```java
ProcessingResult result = processor.processApk(apkPath, config);

System.out.println("成功: " + result.isSuccess());
System.out.println("修改次数: " + result.getTotalModifications());
System.out.println(result.getSummary());
```

### ClassMapping

**包**: `com.resources.model`

**用途**: 类名映射管理

#### 实例方法

##### addMapping

```java
public void addMapping(String oldClass, String newClass)
```

添加类名映射。

**参数**:
- `oldClass` - 旧类名
- `newClass` - 新类名

**抛出**:
- `IllegalArgumentException` - 如果映射冲突

##### getNewClass

```java
public String getNewClass(String oldClass)
```

获取新类名。

**参数**:
- `oldClass` - 旧类名

**返回**: 新类名，如果不存在则返回 `null`

##### replace

```java
public String replace(String className)
```

替换类名（如果存在映射）。

**参数**:
- `className` - 类名

**返回**: 新类名或原类名

##### getAllOldClasses

```java
public Set<String> getAllOldClasses()
```

获取所有旧类名。

**返回**: 旧类名集合

##### size

```java
public int size()
```

获取映射数量。

**返回**: 映射数量

**示例**:
```java
ClassMapping mapping = new ClassMapping();
mapping.addMapping("com.old.MainActivity", "com.new.MainActivity");

String newClass = mapping.replace("com.old.MainActivity");
// newClass = "com.new.MainActivity"
```

### PackageMapping

**包**: `com.resources.model`

**用途**: 包名映射管理（支持前缀匹配）

#### 实例方法

##### addPrefixMapping

```java
public void addPrefixMapping(String oldPkg, String newPkg)
```

添加包名前缀映射。

**参数**:
- `oldPkg` - 旧包名前缀
- `newPkg` - 新包名前缀

##### replace

```java
public String replace(String packageName)
```

替换包名（支持前缀匹配）。

**参数**:
- `packageName` - 包名

**返回**: 替换后的包名

**示例**:
```java
PackageMapping mapping = new PackageMapping();
mapping.addPrefixMapping("com.old", "com.new");

String result1 = mapping.replace("com.old");
// result1 = "com.new"

String result2 = mapping.replace("com.old.ui.MainActivity");
// result2 = "com.new.ui.MainActivity"
```

---

## 工具API

### DexUtils

**包**: `com.resources.util`

**用途**: DEX文件工具类

#### 静态方法

##### loadDexClasses

```java
public static Set<String> loadDexClasses(String dexPath) throws IOException
```

加载DEX文件中的所有类。

**参数**:
- `dexPath` - DEX文件路径

**返回**: 类名集合（Java格式）

**抛出**:
- `IOException` - 加载失败

**示例**:
```java
Set<String> classes = DexUtils.loadDexClasses("input/classes.dex");
System.out.println("类数量: " + classes.size());

if (classes.contains("com.example.MainActivity")) {
    System.out.println("包含MainActivity");
}
```

##### convertDexName

```java
public static String convertDexName(String dexName)
```

将DEX格式类名转换为Java格式。

**参数**:
- `dexName` - DEX格式（如 `Lcom/example/MainActivity;`）

**返回**: Java格式（如 `com.example.MainActivity`）

**示例**:
```java
String javaName = DexUtils.convertDexName("Lcom/example/MainActivity;");
// javaName = "com.example.MainActivity"
```

### DexClassCache

**包**: `com.resources.util`

**用途**: DEX类加载缓存（LRU）

#### 实例方法

##### getClasses

```java
public Set<String> getClasses(String dexPath) throws IOException
```

获取DEX中的类（带缓存）。

**参数**:
- `dexPath` - DEX文件路径

**返回**: 类名集合

**抛出**:
- `IOException` - 加载失败

**示例**:
```java
DexClassCache cache = new DexClassCache();

// 首次加载（慢）
Set<String> classes1 = cache.getClasses("input/classes.dex"); // 350ms

// 缓存命中（快）
Set<String> classes2 = cache.getClasses("input/classes.dex"); // <1ms
```

### VirtualFileSystem

**包**: `com.resources.util`

**用途**: 内存虚拟文件系统

#### 实例方法

##### loadFromApk

```java
public int loadFromApk(String apkPath) throws IOException
```

从APK加载所有文件到内存。

**参数**:
- `apkPath` - APK文件路径

**返回**: 加载的文件数量

**抛出**:
- `IOException` - 加载失败

##### saveToApk

```java
public void saveToApk(String apkPath) throws IOException
```

保存所有文件到APK。

**参数**:
- `apkPath` - APK文件路径

**抛出**:
- `IOException` - 保存失败

##### exists

```java
public boolean exists(String path)
```

检查文件是否存在。

**参数**:
- `path` - 文件路径

**返回**: `true` 如果存在

##### getFile

```java
public byte[] getFile(String path)
```

获取文件内容。

**参数**:
- `path` - 文件路径

**返回**: 文件字节数据

##### putFile

```java
public void putFile(String path, byte[] data)
```

放入文件。

**参数**:
- `path` - 文件路径
- `data` - 文件数据

**示例**:
```java
VirtualFileSystem vfs = new VirtualFileSystem();

// 加载APK
int fileCount = vfs.loadFromApk("input/app.apk");
System.out.println("加载了 " + fileCount + " 个文件");

// 读取文件
byte[] manifest = vfs.getFile("AndroidManifest.xml");

// 修改文件
byte[] modified = processFile(manifest);
vfs.putFile("AndroidManifest.xml", modified);

// 保存APK
vfs.saveToApk("output/app.apk");
```

---

## 验证器API

### SemanticValidator

**包**: `com.resources.validator`

**用途**: 语义验证器（区分类名 vs UI文案）

#### 构造方法

```java
public SemanticValidator(WhitelistFilter whitelistFilter)
```

创建语义验证器。

**参数**:
- `whitelistFilter` - 白名单过滤器

#### 实例方法

##### isClassReference

```java
public boolean isClassReference(String text)
```

判断是否为类名引用。

**参数**:
- `text` - 文本

**返回**: `true` 如果是类名

**示例**:
```java
WhitelistFilter filter = new WhitelistFilter();
filter.addOwnPackages(Set.of("com.example"));

SemanticValidator validator = new SemanticValidator(filter);

boolean r1 = validator.isClassReference("com.example.MainActivity");
// r1 = true

boolean r2 = validator.isClassReference("欢迎使用 com.example");
// r2 = false
```

### Aapt2Validator

**包**: `com.resources.validator`

**用途**: aapt2静态验证器

#### 实例方法

##### validate

```java
public ValidationResult validate(String apkPath) throws IOException
```

验证APK。

**参数**:
- `apkPath` - APK文件路径

**返回**: `ValidationResult` - 验证结果

**抛出**:
- `IOException` - 验证失败

**示例**:
```java
Aapt2Validator validator = new Aapt2Validator();
ValidationResult result = validator.validate("output/app.apk");

if (result.isOverallSuccess()) {
    System.out.println("验证通过");
} else {
    System.out.println("验证失败: " + result.getSummary());
}
```

### DexCrossValidator

**包**: `com.resources.validator`

**用途**: DEX交叉验证器

#### 实例方法

##### validate

```java
public ValidationResult validate(ClassMapping mappings, List<String> dexPaths)
```

验证类名映射。

**参数**:
- `mappings` - 类名映射
- `dexPaths` - DEX文件路径列表

**返回**: `ValidationResult` - 验证结果

**示例**:
```java
ClassMapping mappings = new ClassMapping();
mappings.addMapping("com.old.MainActivity", "com.new.MainActivity");

List<String> dexPaths = Arrays.asList("input/classes.dex");

DexCrossValidator validator = new DexCrossValidator();
ValidationResult result = validator.validate(mappings, dexPaths);

if (result.isOverallSuccess()) {
    System.out.println("所有新类名在DEX中存在");
}
```

---

## 使用示例

### 示例1: 基本处理流程

```java
import com.resources.core.ResourceProcessor;
import com.resources.config.ResourceConfig;
import com.resources.model.ProcessingResult;

public class Example1 {
    public static void main(String[] args) throws Exception {
        // 1. 加载配置
        ResourceConfig config = ResourceConfig.loadFromYaml("config.yaml");
        
        // 2. 创建处理器
        ResourceProcessor processor = new ResourceProcessor();
        
        // 3. 处理APK
        ProcessingResult result = processor.processApk("input/app.apk", config);
        
        // 4. 检查结果
        if (result.isSuccess()) {
            System.out.println("处理成功");
            System.out.println("修改次数: " + result.getTotalModifications());
        } else {
            System.out.println("处理失败");
            for (String error : result.getErrors()) {
                System.out.println("错误: " + error);
            }
        }
    }
}
```

### 示例2: 程序化配置

```java
import com.resources.config.ResourceConfig;
import com.resources.model.ClassMapping;
import com.resources.model.PackageMapping;

public class Example2 {
    public static void main(String[] args) throws Exception {
        // 使用Builder创建配置
        ResourceConfig config = new ResourceConfig.Builder()
            .addOwnPackagePrefix("com.example")
            .addOwnPackagePrefix("com.mycompany")
            .addPackageMapping("com.example", "com.test")
            .addClassMapping("com.example.MainActivity", "com.test.MainActivity")
            .addClassMapping("com.example.ui.Fragment", "com.test.ui.Fragment")
            .addDexPath("input/classes.dex")
            .addDexPath("input/classes2.dex")
            .build();
        
        // 使用配置
        ResourceProcessor processor = new ResourceProcessor();
        ProcessingResult result = processor.processApk("input/app.apk", config);
        
        System.out.println(result.getSummary());
    }
}
```

### 示例3: DEX验证

```java
import com.resources.util.DexUtils;
import com.resources.util.DexClassCache;

public class Example3 {
    public static void main(String[] args) throws Exception {
        // 方式1: 直接加载（慢）
        Set<String> classes1 = DexUtils.loadDexClasses("input/classes.dex");
        System.out.println("加载了 " + classes1.size() + " 个类");
        
        // 方式2: 使用缓存（快）
        DexClassCache cache = new DexClassCache();
        Set<String> classes2 = cache.getClasses("input/classes.dex");
        
        // 检查类是否存在
        if (classes2.contains("com.example.MainActivity")) {
            System.out.println("找到MainActivity");
        }
    }
}
```

### 示例4: VFS操作

```java
import com.resources.util.VirtualFileSystem;

public class Example4 {
    public static void main(String[] args) throws Exception {
        VirtualFileSystem vfs = new VirtualFileSystem();
        
        // 加载APK
        int fileCount = vfs.loadFromApk("input/app.apk");
        System.out.println("加载了 " + fileCount + " 个文件");
        
        // 检查文件
        if (vfs.exists("resources.arsc")) {
            // 读取文件
            byte[] arsc = vfs.getFile("resources.arsc");
            
            // 处理文件
            byte[] modified = processArsc(arsc);
            
            // 写回VFS
            vfs.putFile("resources.arsc", modified);
        }
        
        // 保存APK
        vfs.saveToApk("output/app.apk");
        
        System.out.println(vfs.getStatistics());
    }
    
    private static byte[] processArsc(byte[] data) {
        // 处理逻辑
        return data;
    }
}
```

---

## 附录

### 异常处理

所有API方法可能抛出以下异常：

| 异常 | 说明 |
|------|------|
| `IOException` | 文件读写失败 |
| `IllegalArgumentException` | 参数无效 |
| `IllegalStateException` | 状态无效 |
| `NullPointerException` | 空指针 |

### 线程安全性

| 类 | 线程安全性 |
|----|---------| 
| `ResourceProcessor` | 否 |
| `ResourceConfig` | 是（不可变） |
| `ClassMapping` | 是 |
| `PackageMapping` | 是 |
| `VirtualFileSystem` | 否 |
| `DexClassCache` | 是 |

### 最佳实践

1. **重用ResourceProcessor**: 可重用，但不要并发使用
2. **配置不可变**: 创建后不会被修改，可安全共享
3. **使用DEX缓存**: 显著提升性能
4. **VFS一次性使用**: 加载-处理-保存后丢弃
5. **异常处理**: 始终捕获并处理IOException

---

**文档版本**: 1.0.0  
**最后更新**: 2025-10-20  
**维护者**: Resources Processor Team

