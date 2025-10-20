# Resources Processor - 用户手册

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 完整用户手册

---

## 📚 目录

1. [简介](#简介)
2. [快速入门](#快速入门)
3. [安装与配置](#安装与配置)
4. [核心概念](#核心概念)
5. [配置文件详解](#配置文件详解)
6. [命令使用指南](#命令使用指南)
7. [使用场景与最佳实践](#使用场景与最佳实践)
8. [常见问题FAQ](#常见问题faq)
9. [故障排查](#故障排查)
10. [附录](#附录)

---

## 简介

### 什么是 Resources Processor？

Resources Processor 是一个**工业生产级**的 Android APK 资源处理工具，专门用于 **包名/类名随机化** 场景。它能够安全、可靠地修改 APK 中的资源文件，同时保持应用的完整功能。

### 核心功能

- ✅ **resources.arsc处理**: 替换包名和全局字符串池中的类名/包名
- ✅ **二进制XML处理**: 支持layout、menu、navigation、xml配置
- ✅ **Data Binding支持**: 处理variable type、import type和`T(FQCN)`表达式
- ✅ **语义验证**: 区分类名/包名 vs 普通UI文案，避免误改
- ✅ **白名单过滤**: 仅替换自有包，保留系统/三方库
- ✅ **DEX交叉验证**: 确保新类名在DEX中存在
- ✅ **事务回滚**: 失败时自动恢复，零数据损坏风险
- ✅ **完整性验证**: aapt2静态验证 + 结构完整性检查

### 适用场景

1. **APK加固与混淆**: 配合代码混淆工具，同步修改资源文件中的类名引用
2. **应用马甲包制作**: 快速生成多个不同包名的应用版本
3. **代码重构支持**: 批量重命名包名和类名后，自动更新资源文件
4. **安全加固**: 隐藏真实的包名和类名结构

### 技术特点

- **数据保真度**: 不改变资源ID、不新增/删除资源条目
- **健壮性**: 完整的边界检查、事务机制、自动回滚
- **性能**: DEX缓存、VFS内存文件系统、智能批处理
- **安全性**: 文件大小限制、UTF-8严格验证、结构完整性检查

---

## 快速入门

### 5分钟快速开始

#### 第1步：准备JAR文件

```bash
# 方式1: 下载预编译JAR（推荐）
wget https://github.com/frezrik/jiagu-resources/releases/download/v1.0.0/resources-processor-1.0.0-all.jar

# 方式2: 从源码构建
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources
./gradlew fatJar
# 生成的JAR位于: build/libs/resources-processor-1.0.0-all.jar
```

#### 第2步：创建配置文件

创建 `config.yaml`:

```yaml
version: "1.0"

# 自有包前缀（白名单）
own_package_prefixes:
  - "com.myapp"

# 包名映射
package_mappings:
  "com.myapp": "com.secure.app"

# 类名映射
class_mappings:
  "com.myapp.MainActivity": "com.secure.app.MainActivity"
  "com.myapp.ui.LoginActivity": "com.secure.app.ui.LoginActivity"

# DEX文件路径（用于验证）
dex_paths:
  - "input/classes.dex"
```

#### 第3步：扫描APK（预览）

```bash
java -jar resources-processor-1.0.0-all.jar scan input/myapp.apk -c config.yaml
```

输出示例：
```
════════════════════════════════════════
  Resources Processor - 扫描APK
════════════════════════════════════════

扫描APK: input/myapp.apk
✓ 发现 15 处需要修改

扫描报告：
  - res/layout/activity_main.xml: 3处
  - res/layout/fragment_login.xml: 2处
  - resources.arsc: 10处

✓ 扫描完成！
```

#### 第4步：处理APK

```bash
# 默认已启用自动对齐和签名（使用测试证书）
java -jar resources-processor-1.0.0-all.jar process-apk input/myapp.apk \
  -c config.yaml \
  -o output/myapp-processed.apk
```

**注意**: 
- ✅ **默认已签名**: 处理后的APK已使用测试证书自动对齐和签名，可直接安装测试
- ⚠️ **正式发布**: 需要使用 `--no-auto-sign` 参数，然后用正式证书手动签名

#### 第5步：验证结果

```bash
# 验证APK资源
java -jar resources-processor-1.0.0-all.jar validate output/myapp-processed.apk -v

# 验证签名（已自动签名）
apksigner verify output/myapp-processed.apk

# 安装测试
adb install output/myapp-processed.apk
```

**完成！** 您的APK已成功处理并签名。

---

### 正式发布流程（使用正式证书）

如果需要使用正式发布证书签名，请按以下步骤操作：

#### 第1步：处理APK（禁用自动签名）

```bash
java -jar resources-processor-1.0.0-all.jar process-apk input/myapp.apk \
  -c config.yaml \
  -o output/myapp-processed.apk \
  --no-auto-sign
```

#### 第2步：手动对齐

```bash
zipalign -p -f 4 output/myapp-processed.apk output/myapp-aligned.apk
```

#### 第3步：手动签名（使用正式证书）

```bash
apksigner sign --ks my-release-key.jks \
  --out output/myapp-final.apk \
  output/myapp-aligned.apk
```

#### 第4步：验证签名

```bash
apksigner verify --verbose output/myapp-final.apk
```

**完成！** 您的APK已使用正式证书签名，可以发布。

---

## 安装与配置

### 系统要求

#### 最低要求
- **Java**: JDK 17 或更高版本
- **内存**: 2GB RAM
- **磁盘**: 500MB 可用空间
- **操作系统**: Windows / Linux / macOS

#### 推荐配置
- **Java**: JDK 17+
- **内存**: 4GB+ RAM
- **磁盘**: 2GB+ 可用空间（用于大型APK处理）

### 安装步骤

#### 方式1: 使用预编译JAR（推荐）

```bash
# 1. 下载JAR文件
wget https://github.com/frezrik/jiagu-resources/releases/download/v1.0.0/resources-processor-1.0.0-all.jar

# 2. 验证安装
java -jar resources-processor-1.0.0-all.jar --version

# 3. 查看帮助
java -jar resources-processor-1.0.0-all.jar --help
```

#### 方式2: 从源码构建

```bash
# 1. 克隆仓库
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources

# 2. 构建项目
./gradlew fatJar

# 3. 运行
java -jar build/libs/resources-processor-1.0.0-all.jar --version
```

### 环境配置

#### Java环境

确保Java 17+已安装：

```bash
# 检查Java版本
java -version

# 应输出类似：
# openjdk version "17.0.1" 2021-10-19
# OpenJDK Runtime Environment (build 17.0.1+12-39)
```

如果未安装，请访问 [OpenJDK官网](https://openjdk.org/) 下载安装。

#### Android工具（可选）

如果需要重新签名APK，请安装Android SDK：

```bash
# 安装Android SDK Build Tools
sdkmanager "build-tools;33.0.0"

# 验证apksigner
apksigner --version
```

---

## 核心概念

### 处理流程

Resources Processor 使用**7阶段事务处理流程**：

```
Phase 1: 扫描定位
  ├─ 扫描resources.arsc
  ├─ 扫描所有XML文件
  └─ 生成修改点清单

Phase 2: 预验证
  ├─ 语义验证（区分类名 vs UI文案）
  ├─ 映射一致性验证
  └─ DEX交叉验证（确保新类名存在）

Phase 3: 执行替换
  ├─ 处理AXML文件（layout/menu/navigation/xml）
  ├─ 处理Data Binding表达式
  └─ 处理resources.arsc

Phase 4: 后验证
  └─ aapt2静态验证（可选，混淆APK会跳过）

Phase 5: 重新打包
  └─ 生成新APK

Phase 6: 提交事务
  └─ 确认修改

Phase 7: 回滚（如失败）
  └─ 恢复原始APK
```

### 关键组件

#### 1. 资源映射

**包名映射（Package Mapping）**:
- 支持前缀匹配
- 自动处理子包
- 示例: `com.myapp` → `com.secure.app`

**类名映射（Class Mapping）**:
- 精确匹配
- 需要列出所有类
- 示例: `com.myapp.MainActivity` → `com.secure.app.MainActivity`

#### 2. 白名单过滤

**自有包前缀（Own Package Prefixes）**:
- 定义哪些包属于"自有代码"
- 只有自有包会被替换
- 系统包（`android.*`）和三方库会被保留

示例：
```yaml
own_package_prefixes:
  - "com.myapp"           # 自有包
  - "com.mycompany.lib"   # 自有库
# android.* 自动保留
# androidx.* 自动保留
# com.google.* 自动保留
```

#### 3. 语义验证

Resources Processor 能区分：
- **类名/包名**: 如 `com.myapp.MainActivity`（需要替换）
- **UI文案**: 如 "欢迎使用 com.myapp"（不应替换）

验证规则：
1. 检查是否为完整的类名格式
2. 检查是否在白名单内
3. 检查上下文（避免误判UI文案）

#### 4. DEX交叉验证

在替换前，验证新类名是否在DEX文件中存在：

```yaml
dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"
```

验证流程：
1. 加载DEX文件
2. 提取所有类名
3. 检查配置中的新类名是否存在
4. 失败则终止处理

#### 5. 事务机制

**快照备份**:
- 处理前自动创建APK快照
- 存储在 `temp/snapshots/` 目录

**原子操作**:
- 要么全部成功，要么全部回滚
- 保证APK完整性

**自动回滚**:
- 任何阶段失败，自动恢复原始APK
- 零数据损坏风险

---

## 配置文件详解

### 配置文件结构

完整的配置文件示例：

```yaml
# ============================================
# Resources Processor 配置文件
# 版本: 1.0
# ============================================

version: "1.0"

# --------------------------------------------
# 1. 自有包前缀（白名单）
# --------------------------------------------
# 只有这些包前缀的类/包会被替换
# 系统包和第三方库会被保留
own_package_prefixes:
  - "com.example.app"
  - "com.example.lib"

# --------------------------------------------
# 2. 包名映射
# --------------------------------------------
# 格式：oldPackage: newPackage
# 支持前缀匹配：
#   com.example.app -> com.newapp
#   会自动处理：
#     com.example.app.ui -> com.newapp.ui
#     com.example.app.model -> com.newapp.model
package_mappings:
  "com.example.app": "com.newapp.secure"
  "com.example.lib": "com.newlib.util"

# --------------------------------------------
# 3. 类名映射
# --------------------------------------------
# 格式：oldClass: newClass
# 需要列出所有需要精确替换的类名
class_mappings:
  "com.example.MainActivity": "com.newapp.MainActivity"
  "com.example.ui.LoginActivity": "com.newapp.ui.LoginActivity"
  "com.example.ui.HomeFragment": "com.newapp.ui.HomeFragment"
  "com.example.util.Helper": "com.newapp.util.Helper"
  "com.example.widget.CustomView": "com.newapp.widget.CustomView"

# --------------------------------------------
# 4. 处理目标（可选）
# --------------------------------------------
# 支持glob模式
# 默认处理所有resources.arsc和XML文件
targets:
  - "res/layout/**/*.xml"
  - "res/menu/**/*.xml"
  - "res/navigation/**/*.xml"
  - "res/xml/**/*.xml"
  - "resources.arsc"

# --------------------------------------------
# 5. DEX文件路径（用于DEX交叉验证）
# --------------------------------------------
# 确保所有新类名在DEX中存在
dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"
  - "input/classes3.dex"

# --------------------------------------------
# 6. 高级选项
# --------------------------------------------
options:
  # 是否处理tools:context属性（用于脱敏，不影响运行）
  process_tools_context: true
  
  # 是否启用运行时验证（需要连接Android设备）
  enable_runtime_validation: false
  
  # 是否保留备份
  keep_backup: true
  
  # 是否并行处理（false=极致稳定，true=更快但风险略高）
  parallel_processing: false
```

### 配置项详解

#### version（必需）

配置文件版本号，当前必须为 `"1.0"`。

```yaml
version: "1.0"
```

#### own_package_prefixes（必需）

定义自有包的前缀列表。只有匹配这些前缀的类/包会被替换。

**示例**:
```yaml
own_package_prefixes:
  - "com.myapp"
  - "com.mycompany.sdk"
```

**规则**:
- 至少需要1个前缀
- 使用完整的包名前缀
- 系统包（`android.*`、`androidx.*`等）会自动保留

#### package_mappings（必需）

定义包名替换规则。

**格式**: `"旧包名": "新包名"`

**示例**:
```yaml
package_mappings:
  "com.example.app": "com.secure.app"
```

**前缀匹配特性**:
```yaml
# 配置
"com.example.app": "com.secure.app"

# 自动处理：
# com.example.app         -> com.secure.app
# com.example.app.ui      -> com.secure.app.ui
# com.example.app.model   -> com.secure.app.model
```

#### class_mappings（必需）

定义类名替换规则。

**格式**: `"旧类名（全限定）": "新类名（全限定）"`

**示例**:
```yaml
class_mappings:
  "com.example.MainActivity": "com.secure.MainActivity"
  "com.example.ui.LoginActivity": "com.secure.ui.LoginActivity"
```

**注意**:
- 必须使用完整的类名（包含包名）
- 需要列出所有需要替换的类
- 不支持通配符

#### targets（可选）

定义需要处理的文件模式。

**默认值**: 处理所有 `resources.arsc` 和 XML 文件

**示例**:
```yaml
targets:
  - "res/layout/**/*.xml"
  - "res/menu/**/*.xml"
  - "resources.arsc"
```

#### dex_paths（推荐）

定义DEX文件路径，用于交叉验证。

**示例**:
```yaml
dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"
```

**建议**: 始终配置此项，可避免映射错误。

#### options（可选）

高级选项配置。

**process_tools_context** (默认: `true`):
- 是否处理 `tools:context` 属性
- 该属性仅用于IDE预览，不影响运行

**enable_runtime_validation** (默认: `false`):
- 是否启用运行时验证
- 需要连接Android设备

**keep_backup** (默认: `true`):
- 是否保留备份快照
- 建议保持开启

**parallel_processing** (默认: `false`):
- 是否启用并行处理
- `false` = 极致稳定，`true` = 更快但风险略高

**auto_sign** (默认: `true`):
- 是否自动对齐和签名APK
- `true` = 自动使用测试证书签名（快速测试）
- `false` = 不签名（需手动签名）

**示例**:
```yaml
options:
  auto_sign: false  # 禁用自动签名，用于正式发布
```

---

## 命令使用指南

Resources Processor 提供 3 个主命令：

| 命令 | 功能 | 修改APK | 需要配置 |
|------|------|---------|---------|
| `process-apk` | 处理APK | ✅ 是 | ✅ 是 |
| `scan` | 扫描APK | ❌ 否 | ✅ 是 |
| `validate` | 验证APK | ❌ 否 | ❌ 否 |

### 命令1: process-apk

**功能**: 处理APK文件，替换包名和类名。

**语法**:
```bash
java -jar resources-processor.jar process-apk <APK文件> -c <配置文件> [选项]
```

**参数**:
- `<APK文件>` - 输入APK文件路径（必需）
- `-c, --config <文件>` - 配置文件路径（必需）
- `-o, --output <文件>` - 输出APK文件路径（可选）
- `--dex-path <文件>` - DEX文件路径，可多次指定（可选）
- `--auto-sign` / `--no-auto-sign` - 启用/禁用自动对齐和签名（可选，默认启用）
- `-v, --verbose` - 详细输出模式（可选）

**示例1: 基本使用（默认自动签名）**
```bash
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

**示例2: 指定输出路径**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk
```

**示例3: 添加DEX验证**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  --dex-path input/classes.dex \
  --dex-path input/classes2.dex \
  -v
```

**示例4: 禁用自动签名（正式发布）**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk \
  --no-auto-sign
```

**输出示例**:
```
════════════════════════════════════════
  Resources Processor - 处理APK
════════════════════════════════════════

加载配置: config.yaml
处理APK: input/app.apk
────────────────────────────────────────
  Phase 1: 扫描定位
────────────────────────────────────────
扫描完成: 发现 25 处需要修改
────────────────────────────────────────
  Phase 2: 预验证
────────────────────────────────────────
预验证通过
────────────────────────────────────────
  Phase 3: 执行替换
────────────────────────────────────────
AXML处理完成: 15 个文件已修改
ARSC处理完成
替换完成: 25 处修改
════════════════════════════════════════
  处理成功完成
════════════════════════════════════════
✓ 处理成功！
```

### 命令2: scan

**功能**: 扫描APK，定位所有需要修改的位置（不修改APK）。

**语法**:
```bash
java -jar resources-processor.jar scan <APK文件> -c <配置文件> [选项]
```

**参数**:
- `<APK文件>` - 输入APK文件路径（必需）
- `-c, --config <文件>` - 配置文件路径（必需）
- `-o, --output <文件>` - 输出报告路径（可选）
- `-v, --verbose` - 详细输出模式（可选）

**示例1: 扫描并显示结果**
```bash
java -jar resources-processor.jar scan input/app.apk -c config.yaml
```

**示例2: 保存扫描报告**
```bash
java -jar resources-processor.jar scan input/app.apk \
  -c config.yaml \
  -o reports/scan-report.txt
```

**输出示例**:
```
════════════════════════════════════════
  Resources Processor - 扫描APK
════════════════════════════════════════

扫描APK: input/app.apk
✓ 发现 25 处需要修改

扫描报告：
res/layout/activity_main.xml:
  - 第12行: com.example.MainActivity
  - 第34行: com.example.ui.LoginActivity

res/layout/fragment_home.xml:
  - 第8行: com.example.ui.HomeFragment

resources.arsc:
  - 包名: com.example.app
  - 字符串池: 18处类名引用

报告已保存: reports/scan-report.txt
✓ 扫描完成！
```

### 命令3: validate

**功能**: 验证APK资源的合法性。

**语法**:
```bash
java -jar resources-processor.jar validate <APK文件> [选项]
```

**参数**:
- `<APK文件>` - 输入APK文件路径（必需）
- `--dex-path <文件>` - DEX文件路径，可多次指定（可选）
- `-v, --verbose` - 详细输出模式（可选）

**示例1: 基本验证**
```bash
java -jar resources-processor.jar validate output/app-processed.apk
```

**示例2: 包含DEX验证**
```bash
java -jar resources-processor.jar validate output/app-processed.apk \
  --dex-path output/classes.dex \
  -v
```

**输出示例**:
```
════════════════════════════════════════
  Resources Processor - 验证APK
════════════════════════════════════════

aapt2验证: output/app-processed.apk
✓ aapt2验证通过

DEX交叉验证:
  [1/1] output/classes.dex
    ✓ 加载成功: 1523 个类

验证报告：
  aapt2静态验证: ✓ 通过
  DEX加载验证: ✓ 通过

✓ 验证通过！
```

---

## 使用场景与最佳实践

### 场景1: APK加固配合使用

**需求**: 代码混淆后，同步更新资源文件中的类名引用。

**步骤**:

1. **使用ProGuard/R8混淆代码**:
```bash
# 生成混淆APK和mapping文件
./gradlew assembleRelease
```

2. **从mapping文件生成配置**:
```python
# parse_mapping.py
with open('mapping.txt') as f:
    for line in f:
        if '->' in line:
            old_class = line.split('->')[0].strip()
            new_class = line.split('->')[1].strip()
            print(f'  "{old_class}": "{new_class}"')
```

3. **创建配置文件**:
```yaml
version: "1.0"
own_package_prefixes:
  - "com.myapp"

class_mappings:
  # 从mapping.txt生成的映射
  "com.myapp.MainActivity": "com.a.A"
  "com.myapp.ui.LoginActivity": "com.a.B"
  # ...

dex_paths:
  - "app/build/outputs/apk/release/classes.dex"
```

4. **处理APK**:
```bash
# 禁用自动签名（需要用正式证书签名）
java -jar resources-processor.jar process-apk \
  app/build/outputs/apk/release/app-release.apk \
  -c config.yaml \
  -o output/app-hardened.apk \
  --no-auto-sign
```

5. **重新签名（使用正式证书）**:
```bash
# 对齐
zipalign -p -f 4 output/app-hardened.apk output/app-aligned.apk

# 签名
apksigner sign --ks release.jks output/app-aligned.apk
```

### 场景2: 马甲包批量生成

**需求**: 生成多个不同包名的应用版本。

**步骤**:

1. **创建批处理脚本** (`batch_process.sh`):
```bash
#!/bin/bash

# 定义多个配置
configs=(
  "config-variant1.yaml"
  "config-variant2.yaml"
  "config-variant3.yaml"
)

# 批量处理
for config in "${configs[@]}"; do
  echo "处理: $config"
  
  # 处理APK（默认自动签名，使用测试证书）
  java -jar resources-processor.jar process-apk \
    input/base.apk \
    -c "$config" \
    -o "output/$(basename $config .yaml).apk"
    
  # 注：如需正式发布，添加 --no-auto-sign 参数，然后手动签名
  # java -jar resources-processor.jar process-apk \
  #   input/base.apk \
  #   -c "$config" \
  #   -o "output/$(basename $config .yaml).apk" \
  #   --no-auto-sign
  # apksigner sign --ks release.jks "output/$(basename $config .yaml).apk"
done

echo "批量处理完成！"
```

2. **准备配置文件**:

`config-variant1.yaml`:
```yaml
version: "1.0"
own_package_prefixes:
  - "com.myapp"
package_mappings:
  "com.myapp": "com.variant1.app"
class_mappings:
  "com.myapp.MainActivity": "com.variant1.app.MainActivity"
```

`config-variant2.yaml`:
```yaml
version: "1.0"
own_package_prefixes:
  - "com.myapp"
package_mappings:
  "com.myapp": "com.variant2.app"
class_mappings:
  "com.myapp.MainActivity": "com.variant2.app.MainActivity"
```

3. **执行批处理**:
```bash
chmod +x batch_process.sh
./batch_process.sh
```

### 场景3: 代码重构支持

**需求**: 重构后批量更新资源文件中的类名引用。

**步骤**:

1. **先使用IDE重构代码**:
   - Android Studio: `Refactor > Rename`
   - 重命名包名或类名

2. **导出重构映射**:
   - 记录所有变更的类名

3. **创建配置文件**:
```yaml
version: "1.0"
own_package_prefixes:
  - "com.myapp"

# 重构前后的映射
class_mappings:
  "com.myapp.old.MainActivity": "com.myapp.new.MainActivity"
  "com.myapp.old.ui.Fragment": "com.myapp.new.ui.Fragment"
```

4. **更新APK**:
```bash
java -jar resources-processor.jar process-apk \
  build/outputs/apk/debug/app-debug.apk \
  -c refactor-config.yaml
```

### 最佳实践

#### 1. 始终先扫描

在实际处理前，先使用 `scan` 命令预览修改点：

```bash
# 先扫描
java -jar resources-processor.jar scan input/app.apk -c config.yaml

# 确认无误后再处理
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

#### 2. 启用DEX交叉验证

配置DEX路径，避免映射错误：

```yaml
dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"
```

#### 3. 保留备份

确保配置中 `keep_backup: true`：

```yaml
options:
  keep_backup: true
```

备份文件位于: `temp/snapshots/`

#### 4. 使用版本控制

将配置文件纳入版本控制：

```bash
git add config.yaml
git commit -m "Add resources processor config"
```

#### 5. 测试验证流程

```bash
# 1. 扫描
java -jar rp.jar scan app.apk -c config.yaml

# 2. 处理
java -jar rp.jar process-apk app.apk -c config.yaml -o app-new.apk

# 3. 验证
java -jar rp.jar validate app-new.apk -v

# 4. 安装测试
adb install -r app-new.apk

# 5. 运行测试
adb shell am start -n <package>/<activity>
```

#### 6. 处理大型APK

对于超过100MB的APK，增加JVM内存：

```bash
java -Xmx4g -jar resources-processor.jar process-apk large-app.apk -c config.yaml
```

#### 7. 日志记录

启用详细日志以便调试：

```bash
java -jar resources-processor.jar process-apk app.apk -c config.yaml -v > process.log 2>&1
```

---

## 常见问题FAQ

### Q1: 处理后APK无法安装？

**原因1**: APK签名无效（使用了`--no-auto-sign`但未手动签名）。

**解决**:
```bash
# 手动签名
apksigner sign --ks my-release-key.jks output/app.apk

# 验证签名
apksigner verify output/app.apk
```

**原因2**: 证书不匹配（覆盖安装）。

**解决**:
```bash
# 先卸载旧版本
adb uninstall <package-name>

# 然后安装新版本
adb install output/app.apk
```

**注意**: 
- ✅ **默认已签名**: 如果使用默认配置，APK已自动使用测试证书签名
- ⚠️ **测试证书**: 测试证书只能用于开发测试，不能用于正式发布

### Q2: 提示"DEX验证失败"？

**原因**: 配置中的新类名在DEX中不存在。

**解决**:
1. 检查 `class_mappings` 配置
2. 确保新类名与DEX中的类名完全一致
3. 使用 `jadx` 查看DEX中的实际类名：
```bash
jadx -d output input/classes.dex
```

### Q3: 某些类名没有被替换？

**原因**: 未在 `own_package_prefixes` 中声明。

**解决**:
```yaml
own_package_prefixes:
  - "com.myapp"        # 添加所有自有包前缀
  - "com.mycompany"
```

### Q4: UI文案被错误替换了？

**原因**: 语义验证失败，误判UI文案为类名。

**解决**: 检查日志，如确实误判，请提交Issue。

**临时方案**: 在配置中排除该文件：
```yaml
targets:
  - "res/layout/**/*.xml"
  - "!res/layout/problematic.xml"  # 排除
```

### Q5: 处理速度慢？

**优化**:
1. 增加JVM内存:
```bash
java -Xmx4g -jar resources-processor.jar ...
```

2. 使用SSD存储

3. 关闭实时病毒扫描（临时）

### Q6: OutOfMemoryError错误？

**解决**:
```bash
# 方式1: 增加堆内存
java -Xmx8g -jar resources-processor.jar ...

# 方式2: 增加直接内存
java -XX:MaxDirectMemorySize=2g -jar resources-processor.jar ...
```

### Q7: 找不到resources.arsc？

**原因**: APK可能被混淆，resources.arsc被重命名。

**解决**: Resources Processor 会自动扫描整个APK。

### Q8: Data Binding表达式未处理？

**确认**: Data Binding支持已内置。

**检查**:
1. 确保配置文件包含类名映射
2. 查看日志中的"Data Binding"处理信息

**示例**:
```xml
<!-- 处理前 -->
<variable name="viewModel" type="com.myapp.MainViewModel"/>

<!-- 处理后 -->
<variable name="viewModel" type="com.secure.MainViewModel"/>
```

### Q9: 混淆APK验证失败？

**正常**: 混淆APK无法通过aapt2验证，这是预期行为。

**说明**: 处理阶段会跳过aapt2验证，直接验证结构完整性。

### Q10: 如何恢复失败的处理？

**自动恢复**: Resources Processor 会自动回滚。

**手动恢复**:
```bash
# 快照位于
ls temp/snapshots/

# 手动恢复
cp temp/snapshots/<transaction-id>/app.apk output/app-restored.apk
```

### Q11: 处理后的APK已经签名了吗？

**是的**: 默认情况下，处理后的APK已自动对齐和签名。

**详细说明**:
- ✅ **默认启用**: `--auto-sign` 是默认行为
- 🔑 **使用证书**: 测试证书 `config/keystore/testkey.jks`
- ⚠️ **仅供测试**: 测试证书**不能**用于正式发布

**验证签名**:
```bash
# 查看签名信息
apksigner verify --verbose output/app.apk

# 输出示例：
# Verified using v1 scheme (JAR signing): true
# Verified using v2 scheme (APK Signature Scheme v2): true
```

**安装测试**:
```bash
# 已签名的APK可以直接安装
adb install output/app.apk
```

### Q12: 如何使用正式证书签名？

**步骤**: 使用 `--no-auto-sign` 参数，然后手动签名。

**完整流程**:
```bash
# 第1步: 处理APK（禁用自动签名）
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app.apk \
  --no-auto-sign

# 第2步: 对齐APK
zipalign -p -f 4 output/app.apk output/app-aligned.apk

# 第3步: 使用正式证书签名
apksigner sign --ks my-release-key.jks \
  --ks-key-alias my-key-alias \
  --out output/app-final.apk \
  output/app-aligned.apk

# 第4步: 验证签名
apksigner verify --verbose output/app-final.apk
```

**YAML配置方式**:
```yaml
options:
  auto_sign: false  # 在配置文件中禁用
```

### Q13: 自动签名使用的是哪个证书？

**证书路径**: `config/keystore/testkey.jks`  
**证书密码**: `testkey`  
**密钥别名**: `testkey`  
**密钥密码**: `testkey`

**证书用途**: 
- ✅ 开发测试
- ✅ 本地调试
- ✅ CI/CD测试
- ❌ **不可用于**正式发布到应用商店

**自定义证书**: 当前版本暂不支持在CLI中指定自定义证书，请使用 `--no-auto-sign` 然后手动签名。

---

## 故障排查

### 日志位置

```
logs/
  ├── resources-processor.log         # 主日志
  ├── resources-processor-error.log   # 错误日志
  └── resources-processor-performance.log  # 性能日志
```

### 启用详细日志

```bash
java -jar resources-processor.jar process-apk app.apk -c config.yaml -v
```

### 常见错误码

| 错误码 | 含义 | 解决方案 |
|--------|------|---------|
| 0 | 成功 | - |
| 1 | 处理失败 | 查看日志，检查配置 |
| 2 | 用法错误 | 使用 `--help` 查看帮助 |

### 调试步骤

1. **检查Java版本**:
```bash
java -version  # 需要 17+
```

2. **验证配置文件**:
```bash
# 检查YAML语法
python -c "import yaml; yaml.safe_load(open('config.yaml'))"
```

3. **检查文件权限**:
```bash
# Linux/macOS
ls -l input/app.apk

# Windows
icacls input\app.apk
```

4. **查看详细日志**:
```bash
tail -f logs/resources-processor.log
```

5. **测试小APK**:
```bash
# 先用小APK测试配置
java -jar resources-processor.jar process-apk small-app.apk -c config.yaml
```

### 性能问题排查

**检查内存使用**:
```bash
# 添加JVM监控参数
java -Xmx4g -XX:+PrintGCDetails -jar resources-processor.jar ...
```

**检查磁盘IO**:
```bash
# Linux
iotop

# Windows
resmon.exe
```

### 获取帮助

如果问题仍未解决，请提供以下信息：

1. Resources Processor 版本
2. Java 版本
3. 操作系统
4. APK大小
5. 完整错误日志
6. 配置文件（脱敏后）

提交Issue: https://github.com/frezrik/jiagu-resources/issues

---

## 附录

### A. 配置文件模板

#### 模板1: 基础配置
```yaml
version: "1.0"

own_package_prefixes:
  - "com.myapp"

package_mappings:
  "com.myapp": "com.newapp"

class_mappings:
  "com.myapp.MainActivity": "com.newapp.MainActivity"

dex_paths:
  - "input/classes.dex"
```

#### 模板2: 多包配置
```yaml
version: "1.0"

own_package_prefixes:
  - "com.myapp"
  - "com.mycompany.sdk"

package_mappings:
  "com.myapp": "com.secure.app"
  "com.mycompany.sdk": "com.secure.sdk"

class_mappings:
  "com.myapp.MainActivity": "com.secure.app.MainActivity"
  "com.mycompany.sdk.Helper": "com.secure.sdk.Helper"

dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"
```

#### 模板3: 完整配置
```yaml
version: "1.0"

own_package_prefixes:
  - "com.myapp"

package_mappings:
  "com.myapp": "com.secure.app"

class_mappings:
  "com.myapp.MainActivity": "com.secure.app.MainActivity"
  "com.myapp.ui.LoginActivity": "com.secure.app.ui.LoginActivity"
  "com.myapp.ui.HomeFragment": "com.secure.app.ui.HomeFragment"

targets:
  - "res/layout/**/*.xml"
  - "res/menu/**/*.xml"
  - "resources.arsc"

dex_paths:
  - "input/classes.dex"

options:
  process_tools_context: true
  enable_runtime_validation: false
  keep_backup: true
  parallel_processing: false
```

### B. 支持的文件类型

| 文件类型 | 扩展名 | 支持程度 |
|---------|--------|---------|
| resources.arsc | `.arsc` | ✅ 完全支持 |
| Layout XML | `.xml` | ✅ 完全支持 |
| Menu XML | `.xml` | ✅ 完全支持 |
| Navigation XML | `.xml` | ✅ 完全支持 |
| XML Config | `.xml` | ✅ 完全支持 |
| Data Binding | `.xml` | ✅ 完全支持 |
| AndroidManifest.xml | `.xml` | ❌ 不处理 |

### C. 性能基准

| APK大小 | 扫描时间 | 处理时间 | 内存占用 |
|---------|---------|---------|---------|
| 10 MB | 0.5s | 2.1s | 150 MB |
| 50 MB | 2.1s | 8.3s | 280 MB |
| 100 MB | 4.5s | 18.7s | 520 MB |
| 200 MB | 9.8s | 42.3s | 1.2 GB |

测试环境: i7-10700K, 16GB RAM, NVMe SSD

### D. 术语表

| 术语 | 说明 |
|-----|------|
| **ARSC** | Android Resources，资源索引表文件 |
| **AXML** | Android Binary XML，二进制XML |
| **DEX** | Dalvik Executable，Android字节码文件 |
| **FQCN** | Fully Qualified Class Name，完全限定类名 |
| **VFS** | Virtual File System，虚拟文件系统 |
| **aapt2** | Android Asset Packaging Tool 2 |
| **apksigner** | APK签名工具 |

### E. 相关链接

- **项目主页**: https://github.com/frezrik/jiagu-resources
- **文档Wiki**: https://github.com/frezrik/jiagu-resources/wiki
- **问题反馈**: https://github.com/frezrik/jiagu-resources/issues
- **发布版本**: https://github.com/frezrik/jiagu-resources/releases

### F. 许可证

Apache License 2.0

---

**文档版本**: 1.0.0  
**最后更新**: 2025-10-20  
**维护者**: Resources Processor Team


