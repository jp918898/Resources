# Resources Processor

工业生产级resources.arsc和二进制XML处理工具 - 用于Android APK包名/类名随机化场景

[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](https://github.com/frezrik/jiagu-resources)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-120%2B%20passing-brightgreen.svg)](build/reports/tests/test/index.html)

---

## ✨ 核心功能

- ✅ **resources.arsc处理**: 替换包名和全局字符串池中的类名/包名
- ✅ **二进制XML处理**: 支持layout、menu、navigation、xml配置
- ✅ **Data Binding支持**: 处理variable type、import type和T(FQCN)表达式
- ✅ **语义验证**: 区分类名/包名vs普通UI文案，避免误改
- ✅ **白名单过滤**: 仅替换自有包，保留系统/三方库
- ✅ **DEX交叉验证**: 确保新类名在DEX中存在
- ✅ **事务回滚**: 失败时自动恢复，零数据损坏风险
- ✅ **完整性验证**: aapt2静态验证 + 结构完整性检查

---

## 🚀 快速开始

### 安装

#### 方式1: 下载预编译JAR（推荐）
```bash
# 下载Fat JAR（包含所有依赖）
wget https://github.com/frezrik/jiagu-resources/releases/download/v1.0.1/resources-processor-1.0.1-all.jar

# 或从项目构建
./gradlew fatJar
```

#### 方式2: 克隆并构建
```bash
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources
./gradlew build
```

### 快速使用

#### 1. 准备配置文件
```yaml
# config/my-config.yaml
version: "1.0"

own_package_prefixes:
  - "com.myapp"

package_mappings:
  "com.myapp": "com.a"

class_mappings:
  "com.myapp.MainActivity": "com.a.A"
  "com.myapp.ui.Fragment": "com.a.B"

dex_paths:
  - "input/dex/classes.dex"
```

#### 2. 运行命令
```bash
# 扫描APK（预览修改点）
java -jar resources-processor-1.0.1-all.jar scan input/myapp.apk -c config/my-config.yaml

# 处理APK（默认自动对齐和签名）
java -jar resources-processor-1.0.1-all.jar process-apk input/myapp.apk -c config/my-config.yaml

# 验证结果
java -jar resources-processor-1.0.1-all.jar validate output/myapp.apk
```

**注意**: 
- ✅ **默认已对齐和签名**: 处理后的APK已使用测试证书签名，可直接安装测试
- ⚠️ **正式发布**: 使用 `--no-auto-sign` 参数，然后用正式证书手动签名

#### 3. 正式发布签名（可选）
```bash
# 禁用自动签名
java -jar resources-processor-1.0.1-all.jar process-apk input/myapp.apk \
  -c config/my-config.yaml \
  --no-auto-sign

# 手动签名（使用发布证书）
apksigner sign --ks my-release-key.jks output/myapp.apk
```

---

## 📖 完整文档

### 用户文档
- 📘 [完整用户手册](docs/USER_MANUAL.md) - 快速入门、配置详解、最佳实践、FAQ
- 📗 [CLI完整参考手册](docs/CLI_REFERENCE.md) - 所有命令详细说明、参数、示例
- 📙 [CLI快速参考卡片](docs/CLI_QUICK_REFERENCE.md) - 速查表、常用命令
- 📋 [CLI命令索引](docs/CLI_COMMANDS_INDEX.md) - 命令总览和导航

### 技术文档
- 🏗️ [架构设计文档](docs/ARCHITECTURE.md) - 系统架构、模块设计、数据流
- 🔨 [构建与运行指南](docs/BUILD_AND_RUN.md) - 构建、测试、调试、CI/CD
- 💻 [开发指南](docs/DEVELOPMENT_GUIDE.md) - 代码规范、贡献流程

### 维护文档
- 🛠️ [修复实施报告](docs/FIXES_IMPLEMENTED.md) - 缺陷修复和改进记录
- 📋 [可执行清单](docs/可执行清单.md) - Android Resources处理规范
- 📖 [API参考文档](docs/API_REFERENCE.md) - 公共API、接口说明

---

## 💻 命令速查

### 三个主命令

| 命令 | 功能 | 是否修改APK | 必需配置文件 |
|------|------|------------|-------------|
| `process-apk` | 处理APK | ✅ 是 | ✅ 是 |
| `scan` | 扫描APK | ❌ 否 | ✅ 是 |
| `validate` | 验证APK | ❌ 否 | ❌ 否 |

### 快速命令
```bash
# 查看帮助
java -jar rp.jar --help

# 查看版本
java -jar rp.jar --version

# 处理APK（最常用）
java -jar rp.jar process-apk input/app.apk -c config.yaml

# 扫描APK
java -jar rp.jar scan input/app.apk -c config.yaml -o report.txt

# 验证APK
java -jar rp.jar validate output/app.apk -v
```

---

## 🏗️ 架构概览

### 核心模块
```
ResourceCLI (命令行入口)
    ↓
ResourceProcessor (主控制器)
    ↓
├── ResourceScanner (扫描定位)
│   ├── AxmlScanner (扫描AXML)
│   └── ArscScanner (扫描ARSC)
├── TransactionManager (事务管理)
│   ├── SnapshotManager (快照备份)
│   ├── MappingValidator (映射验证)
│   └── DexCrossValidator (DEX交叉验证)
├── AxmlReplacer (AXML替换)
│   ├── LayoutProcessor
│   ├── MenuProcessor
│   ├── NavigationProcessor
│   ├── XmlConfigProcessor
│   └── DataBindingProcessor
├── ArscReplacer (ARSC替换)
│   ├── ArscParser
│   ├── ArscWriter
│   └── ResStringPool
└── Aapt2Validator (aapt2验证)
```

### 工具模块
- `VirtualFileSystem` - 虚拟文件系统（内存APK）
- `DexUtils` - DEX工具类
- `DexClassCache` - DEX类加载缓存
- `SemanticValidator` - 语义验证器
- `WhitelistFilter` - 白名单过滤器

---

## 🧪 测试

### 运行测试
```bash
# 所有测试
./gradlew test

# 单元测试
./gradlew test --tests "*Test"

# 集成测试
./gradlew test --tests "*IntegrationTest"

# 查看报告
open build/reports/tests/test/index.html
```

### 测试覆盖
- **单元测试**: 100+ 测试
- **集成测试**: 20+ 测试
- **真实APK测试**: Dragonfly.apk, Telegram.apk
- **覆盖率**: 85%+

---

## 🔥 核心特性

### 数据保真度
- ✅ **资源ID稳定**: 不改packageId/typeId/entryId
- ✅ **结构完整**: 不新增/删除资源条目
- ✅ **UTF-8严格验证**: 可配置STRICT/LENIENT/WARN模式
- ✅ **往返测试**: ARSC/AXML解析-写入往返无损

### 健壮性
- ✅ **边界检查**: 所有ByteBuffer操作带边界保护
- ✅ **文件大小限制**: 防止OOM（单文件100MB，总大小2GB）
- ✅ **事务机制**: 失败自动回滚
- ✅ **线程安全**: AtomicBoolean + putIfAbsent原子操作

### 性能
- ✅ **DEX缓存**: LRU缓存避免重复加载（加速350倍）
- ✅ **VFS**: 内存虚拟文件系统避免频繁ZIP操作
- ✅ **安全边界**: 智能Buffer分配（+10%安全边界）
- ✅ **ZIP元数据保留**: 保持压缩方法和资源对齐

---

## 📦 依赖

### 核心依赖
- **dexlib2** 3.0.3 - DEX文件解析
- **picocli** 4.7.5 - 命令行工具
- **snakeyaml** 2.2 - YAML配置
- **slf4j + logback** - 日志框架
- **guava** 32.1.3 - 工具类

### 测试依赖
- **JUnit Jupiter** 5.10.0 - 测试框架
- **Mockito** 5.6.0 - Mock框架
- **AssertJ** 3.24.2 - 断言库

---

## 🌟 v1.0.1 更新亮点

### 修复的缺陷
- ✅ 消除DEX工具代码100%重复（提取DexUtils）
- ✅ 修复UTFDataFormatException缺失import
- ✅ 添加ResStringPool严格验证模式
- ✅ 实现ArscWriter精确大小计算（+10%安全边界）
- ✅ VFS添加文件大小限制（防止OOM）
- ✅ VFS保留ZIP元数据（压缩方法、CRC、extra）
- ✅ 修复VirtualFileSystem线程安全（AtomicBoolean）
- ✅ ClassMapping/PackageMapping原子操作（putIfAbsent）
- ✅ 实现CLI --dex-path参数合并
- ✅ 添加DEX类加载缓存（LRU）

### 新增功能
- ✅ 所有ByteBuffer操作边界检查
- ✅ ResStringPool三种验证模式
- ✅ DexUtils工具类
- ✅ DexClassCache缓存类
- ✅ ResourceConfig.toBuilder()方法
- ✅ **自动对齐和签名**: 新增 `--auto-sign`/`--no-auto-sign` CLI参数
- ✅ **集成工具**: zipalign和apksigner自动调用（默认启用）
- ✅ **测试证书**: 内置测试证书用于快速测试

### 详细记录
见 [修复实施报告](docs/FIXES_IMPLEMENTED.md)

---

## 📊 性能基准

| APK大小 | 扫描 | 处理 | 内存 | 测试环境 |
|---------|------|------|------|---------|
| 10 MB | 0.5s | 2.1s | 150 MB | i7-10700K |
| 50 MB | 2.1s | 8.3s | 280 MB | 16GB RAM |
| 100 MB | 4.5s | 18.7s | 520 MB | NVMe SSD |

**DEX缓存**: 首次350ms → 缓存命中<1ms（加速350倍）

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

### 开发流程
```bash
# 1. Fork项目
# 2. 创建分支
git checkout -b feature/my-feature

# 3. 修改代码
# 4. 运行测试
./gradlew test

# 5. 提交更改
git commit -m "Add my feature"

# 6. 推送分支
git push origin feature/my-feature

# 7. 创建Pull Request
```

---

## 📄 许可证

Apache License 2.0 - 详见 [LICENSE](LICENSE)

---

## 🔗 相关链接

- **GitHub**: https://github.com/frezrik/jiagu-resources
- **文档Wiki**: https://github.com/frezrik/jiagu-resources/wiki
- **问题反馈**: https://github.com/frezrik/jiagu-resources/issues

---

## 📧 联系方式

- **作者**: Resources Processor Team
- **邮箱**: frezrik@example.com
- **支持**: GitHub Issues

---

**最后更新**: 2025-10-19  
**版本**: 1.0.1  
**状态**: ✅ 生产就绪

