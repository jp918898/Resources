# Resources Processor - CLI完整参考手册

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 命令行接口完整参考

---

## 📚 目录

1. [概述](#概述)
2. [主命令](#主命令)
3. [子命令详解](#子命令详解)
   - [process-apk](#process-apk)
   - [scan](#scan)
   - [validate](#validate)
4. [全局选项](#全局选项)
5. [返回码](#返回码)
6. [使用示例](#使用示例)
7. [环境变量](#环境变量)
8. [配置文件](#配置文件)

---

## 概述

### 命令行语法

```bash
java -jar resources-processor-<version>-all.jar <COMMAND> [OPTIONS]
```

### 命令总览

| 命令 | 功能 | 是否修改APK | 必需配置文件 |
|------|------|------------|-------------|
| `process-apk` | 处理APK文件，替换包名和类名 | ✅ 是 | ✅ 是 |
| `scan` | 扫描APK，定位需要修改的位置 | ❌ 否 | ✅ 是 |
| `validate` | 验证APK资源的合法性 | ❌ 否 | ❌ 否 |

### 快速示例

```bash
# 查看帮助
java -jar resources-processor.jar --help

# 查看版本
java -jar resources-processor.jar --version

# 处理APK
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml

# 扫描APK
java -jar resources-processor.jar scan input/app.apk -c config.yaml

# 验证APK
java -jar resources-processor.jar validate output/app.apk
```

---

## 主命令

### 基本信息

**命令名**: `resource-processor`

**版本**: 1.0.0

**描述**: 工业生产级resources.arsc和二进制XML处理工具

### 主命令选项

#### --help / -h

显示帮助信息。

**语法**:
```bash
java -jar resources-processor.jar --help
```

**输出**:
```
Usage: resource-processor [-hV] [COMMAND]
工业生产级resources.arsc和二进制XML处理工具
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  process-apk  处理APK文件，替换包名和类名
  scan         扫描APK，定位需要修改的位置
  validate     验证APK资源的合法性
```

#### --version / -V

显示版本信息。

**语法**:
```bash
java -jar resources-processor.jar --version
```

**输出**:
```
Resources Processor 1.0.0
```

### 无子命令运行

如果不指定子命令，会显示错误提示：

```bash
java -jar resources-processor.jar
```

**输出**:
```
错误: 请使用子命令。使用 --help 查看帮助。

可用命令:
  process-apk  处理APK文件，替换包名和类名
  scan         扫描APK，定位需要修改的位置
  validate     验证APK资源的合法性

使用 resource-processor <命令> --help 查看命令详情
```

**返回码**: 2（用法错误）

---

## 子命令详解

## process-apk

处理APK文件，替换包名和类名。

### 语法

```bash
java -jar resources-processor.jar process-apk <APK文件> -c <配置文件> [OPTIONS]
```

### 位置参数

#### APK文件

**索引**: 0  
**类型**: String  
**必需**: ✅ 是  
**描述**: 输入APK文件路径

**示例**:
```bash
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

**验证**:
- 文件必须存在
- 必须是常规文件（非目录）
- 必须可读

**错误示例**:
```bash
# 文件不存在
java -jar resources-processor.jar process-apk notfound.apk -c config.yaml
# 输出: ✗ 错误: APK文件不存在: notfound.apk
```

### 必需选项

#### -c, --config

配置文件路径（YAML格式）。

**类型**: String  
**必需**: ✅ 是  
**格式**: YAML  
**描述**: 包含包名映射、类名映射等配置的YAML文件

**示例**:
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  --config config/my-config.yaml
```

**简写形式**:
```bash
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

**验证**:
- 文件必须存在
- 必须是有效的YAML格式
- 必须包含必需字段（version, own_package_prefixes等）

**配置文件结构**: 参见 [配置文件](#配置文件) 章节

### 可选选项

#### -o, --output

输出APK文件路径。

**类型**: String  
**必需**: ❌ 否  
**默认**: 覆盖输入文件  
**描述**: 指定处理后的APK保存路径

**示例**:
```bash
# 保存到新文件
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk
```

**行为**:
- 如果未指定: 直接修改输入APK（有快照备份）
- 如果指定: 先复制到输出路径，再处理（输入APK不变）

**路径创建**:
- 如果父目录不存在，会自动创建

#### --dex-path

DEX文件路径，用于交叉验证。

**类型**: String[]  
**必需**: ❌ 否  
**可重复**: ✅ 是  
**描述**: 指定一个或多个DEX文件，用于验证新类名是否存在

**示例1: 单个DEX**:
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  --dex-path input/classes.dex
```

**示例2: 多个DEX**:
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  --dex-path input/classes.dex \
  --dex-path input/classes2.dex \
  --dex-path input/classes3.dex
```

**合并行为**:
- CLI参数中的DEX路径会与配置文件中的 `dex_paths` 合并
- CLI参数优先级更高

**验证流程**:
1. 加载所有指定的DEX文件
2. 提取DEX中的所有类名
3. 检查配置中的新类名是否在DEX中存在
4. 如果不存在，终止处理并报错

#### --auto-sign / --no-auto-sign

启用或禁用自动对齐和签名APK。

**类型**: Boolean  
**必需**: ❌ 否  
**默认**: `--auto-sign` (启用)  
**描述**: 控制是否在处理APK后自动执行对齐(zipalign)和签名(apksigner)

**使用说明**:
- **默认行为**: 处理APK后会自动执行对齐和签名
- **对齐工具**: `bin/win/zipalign.exe` (4字节对齐)
- **签名工具**: `bin/win/apksigner.bat`
- **测试证书**: `config/keystore/testkey.jks` (密码: `testkey`)

**示例1: 默认启用（可省略）**:
```bash
# 默认已启用对齐和签名
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml

# 显式启用（效果相同）
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml --auto-sign
```

**示例2: 禁用对齐和签名**:
```bash
# 不对齐、不签名（需要手动处理）
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  --no-auto-sign
```

**示例3: 禁用后手动签名**:
```bash
# 第1步: 处理APK（不签名）
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app.apk \
  --no-auto-sign

# 第2步: 手动对齐
bin/win/zipalign.exe 4 output/app.apk output/app-aligned.apk

# 第3步: 手动签名（使用自己的证书）
apksigner sign --ks my-release-key.jks output/app-aligned.apk
```

**YAML配置文件控制**:
```yaml
options:
  auto_sign: false  # 在配置文件中禁用
```

**优先级**:
- CLI参数 `--auto-sign` / `--no-auto-sign` **高于** YAML配置
- 如果CLI未指定，使用YAML配置
- 如果YAML未配置，默认启用

**使用场景**:
- ✅ **启用**: 快速测试、本地开发、CI/CD自动化
- ❌ **禁用**: 需要使用正式发布证书签名时

**工具要求**:
- Windows: 需要 `bin/win/zipalign.exe` 和 `bin/win/apksigner.bat`
- 测试证书: `config/keystore/testkey.jks` (仅用于测试，不可用于发布)

**注意事项**:
⚠️ **默认使用测试证书**: 自动签名使用测试证书，仅供测试使用  
⚠️ **正式发布**: 必须使用 `--no-auto-sign`，然后用正式证书手动签名  
⚠️ **工具路径**: 对齐和签名工具必须在 `bin/win/` 目录下

#### -v, --verbose

详细输出模式。

**类型**: Boolean  
**必需**: ❌ 否  
**默认**: false  
**描述**: 启用详细日志输出，包括异常堆栈跟踪

**示例**:
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -v
```

**效果**:
- 显示详细处理步骤
- 输出异常的完整堆栈跟踪
- 显示每个文件的处理状态

### 执行流程

```
1. 加载配置文件
   └─ 验证配置有效性

2. 验证输入文件
   ├─ 检查APK存在性
   └─ 检查配置文件存在性

3. 准备工作文件
   ├─ 如指定-o: 复制到输出路径
   └─ 否则: 使用输入文件

4. 开启事务
   └─ 创建快照备份

5. Phase 1: 扫描定位
   ├─ 扫描resources.arsc
   ├─ 扫描所有XML文件
   └─ 生成修改点清单

6. Phase 2: 预验证
   ├─ 映射一致性验证
   └─ DEX交叉验证（如指定）

7. Phase 3: 执行替换
   ├─ 处理AXML文件
   └─ 处理resources.arsc

8. Phase 4: 对齐和签名（可选）
   ├─ zipalign对齐APK
   └─ apksigner签名APK

9. Phase 5: 后验证（跳过）
   └─ aapt2验证（混淆APK跳过）

10. 提交事务
    └─ 生成处理报告

11. 返回结果
```

### 输出示例

**成功输出**:
```
════════════════════════════════════════
  Resources Processor - 处理APK
════════════════════════════════════════

加载配置: config.yaml
复制到输出路径: output/app-processed.apk
✓ 已复制到: output/app-processed.apk
处理APK: output/app-processed.apk
────────────────────────────────────────
  Phase 1: 扫描定位
────────────────────────────────────────
扫描完成: 发现 32 处需要修改
────────────────────────────────────────
  Phase 2: 预验证
────────────────────────────────────────
合并CLI DEX路径: [input/classes.dex]
预验证通过
────────────────────────────────────────
  Phase 3: 执行替换
────────────────────────────────────────
VFS加载完成: 2341 个文件
发现 18 个XML文件待处理
AXML处理完成: 18 个文件已修改
ARSC包名替换: 'com.example.app' -> 'com.secure.app'
ARSC字符串池替换: 14 处
resources.arsc已更新到VFS
替换完成: 32 处修改
APK已更新: output/app-processed.apk
════════════════════════════════════════
  处理成功完成
════════════════════════════════════════

处理报告：
  - 扫描文件数: 32
  - 修改次数: 32
  - 处理时间: 3.45s
  - 状态: ✓ 成功

✓ 处理成功！
```

**失败输出**:
```
════════════════════════════════════════
  Resources Processor - 处理APK
════════════════════════════════════════

✗ 错误: APK文件不存在: input/notfound.apk
```

### 返回码

| 返回码 | 含义 |
|--------|------|
| 0 | 成功 |
| 1 | 失败（错误详情见日志） |

### 完整示例

**示例1: 基本使用**
```bash
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

**示例2: 指定输出路径**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk
```

**示例3: 启用DEX验证**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  --dex-path input/classes.dex \
  --dex-path input/classes2.dex
```

**示例4: 详细输出**
```bash
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk \
  -v
```

**示例5: 增加JVM内存（大型APK）**
```bash
java -Xmx4g -jar resources-processor.jar process-apk large-app.apk \
  -c config.yaml \
  -o output/large-app-processed.apk
```

**示例6: 禁用自动签名（用于正式发布）**
```bash
# 处理但不签名
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app.apk \
  --no-auto-sign

# 手动使用发布证书签名
apksigner sign --ks release.jks output/app.apk
```

---

## scan

扫描APK，定位所有需要修改的位置（不修改APK）。

### 语法

```bash
java -jar resources-processor.jar scan <APK文件> -c <配置文件> [OPTIONS]
```

### 位置参数

#### APK文件

**索引**: 0  
**类型**: String  
**必需**: ✅ 是  
**描述**: 输入APK文件路径

**示例**:
```bash
java -jar resources-processor.jar scan input/app.apk -c config.yaml
```

### 必需选项

#### -c, --config

配置文件路径（YAML格式）。

**类型**: String  
**必需**: ✅ 是  
**描述**: 同 `process-apk` 命令

**示例**:
```bash
java -jar resources-processor.jar scan input/app.apk --config config.yaml
```

### 可选选项

#### -o, --output

输出报告路径。

**类型**: String  
**必需**: ❌ 否  
**默认**: 仅控制台输出  
**描述**: 将扫描报告保存到文件

**示例**:
```bash
java -jar resources-processor.jar scan input/app.apk \
  -c config.yaml \
  -o reports/scan-report.txt
```

**文件编码**: UTF-8

#### -v, --verbose

详细输出模式。

**类型**: Boolean  
**必需**: ❌ 否  
**默认**: false  
**描述**: 启用详细日志输出

### 执行流程

```
1. 加载配置文件

2. 创建扫描器
   ├─ 初始化语义验证器
   ├─ 初始化白名单过滤器
   └─ 配置自有包前缀

3. 扫描APK
   ├─ 扫描resources.arsc
   │  ├─ 包名
   │  └─ 全局字符串池
   ├─ 扫描XML文件
   │  ├─ Layout XML
   │  ├─ Menu XML
   │  ├─ Navigation XML
   │  └─ Config XML
   └─ 扫描Data Binding

4. 生成报告
   ├─ 汇总修改点
   ├─ 按文件分组
   └─ 统计数量

5. 显示/保存报告
   └─ 控制台 / 文件

6. 返回结果
```

### 输出示例

**成功输出**:
```
════════════════════════════════════════
  Resources Processor - 扫描APK
════════════════════════════════════════

加载配置: config.yaml
扫描APK: input/app.apk

扫描报告：
════════════════════════════════════════

总计: 32 处需要修改

按文件分组：
────────────────────────────────────────
res/layout/activity_main.xml: 5处
  - 行12: com.example.MainActivity
  - 行18: com.example.ui.LoginActivity
  - 行25: com.example.ui.HomeFragment
  - 行30: com.example.widget.CustomView
  - 行42: com.example.util.Helper

res/layout/fragment_login.xml: 2处
  - 行8: com.example.ui.LoginActivity
  - 行15: com.example.viewmodel.LoginViewModel

res/menu/main_menu.xml: 1处
  - 行5: com.example.MainActivity

resources.arsc: 24处
  - 包名: com.example.app -> com.secure.app
  - 字符串池: 23处类名引用

════════════════════════════════════════
报告已保存: reports/scan-report.txt
✓ 扫描完成！
```

### 返回码

| 返回码 | 含义 |
|--------|------|
| 0 | 成功 |
| 1 | 失败 |

### 完整示例

**示例1: 基本扫描**
```bash
java -jar resources-processor.jar scan input/app.apk -c config.yaml
```

**示例2: 保存报告**
```bash
java -jar resources-processor.jar scan input/app.apk \
  -c config.yaml \
  -o reports/scan-$(date +%Y%m%d_%H%M%S).txt
```

**示例3: 详细输出**
```bash
java -jar resources-processor.jar scan input/app.apk \
  -c config.yaml \
  -o reports/scan-report.txt \
  -v
```

---

## validate

验证APK资源的合法性。

### 语法

```bash
java -jar resources-processor.jar validate <APK文件> [OPTIONS]
```

### 位置参数

#### APK文件

**索引**: 0  
**类型**: String  
**必需**: ✅ 是  
**描述**: 输入APK文件路径

**示例**:
```bash
java -jar resources-processor.jar validate output/app.apk
```

### 可选选项

#### --dex-path

DEX文件路径，用于交叉验证。

**类型**: String[]  
**必需**: ❌ 否  
**可重复**: ✅ 是  
**描述**: 指定一个或多个DEX文件，验证DEX可加载性

**示例**:
```bash
java -jar resources-processor.jar validate output/app.apk \
  --dex-path output/classes.dex \
  --dex-path output/classes2.dex
```

**验证内容**:
1. DEX文件是否存在
2. DEX文件是否可加载
3. 提取类列表
4. 统计类数量

#### -v, --verbose

详细输出模式。

**类型**: Boolean  
**必需**: ❌ 否  
**默认**: false  
**描述**: 显示详细验证信息和异常堆栈

### 执行流程

```
1. 验证APK文件存在性

2. aapt2验证
   └─ 调用系统aapt2工具验证APK

3. DEX交叉验证（如指定）
   ├─ 验证DEX文件存在性
   ├─ 加载DEX文件
   ├─ 提取类列表
   └─ 统计类数量

4. 生成验证报告
   ├─ aapt2验证结果
   └─ DEX验证结果

5. 返回结果
```

### 输出示例

**成功输出（仅aapt2）**:
```
════════════════════════════════════════
  Resources Processor - 验证APK
════════════════════════════════════════

aapt2验证: output/app.apk
✓ aapt2验证通过

验证报告：
  aapt2静态验证: ✓ 通过

✓ 验证通过！
```

**成功输出（包含DEX）**:
```
════════════════════════════════════════
  Resources Processor - 验证APK
════════════════════════════════════════

aapt2验证: output/app.apk
✓ aapt2验证通过

DEX交叉验证:
  [1/2] output/classes.dex
    ✓ 加载成功: 1523 个类
  [2/2] output/classes2.dex
    ✓ 加载成功: 342 个类

验证报告：
  aapt2静态验证: ✓ 通过
  DEX加载验证: ✓ 通过
    - classes.dex: 1523 类
    - classes2.dex: 342 类
    - 总计: 1865 类

✓ 验证通过！
```

**失败输出**:
```
════════════════════════════════════════
  Resources Processor - 验证APK
════════════════════════════════════════

aapt2验证: output/app.apk
✗ aapt2验证失败

验证报告：
  aapt2静态验证: ✗ 失败
    - 错误: resources.arsc is corrupt

✗ 验证失败！
```

### 返回码

| 返回码 | 含义 |
|--------|------|
| 0 | 验证通过 |
| 1 | 验证失败 |

### 完整示例

**示例1: 基本验证**
```bash
java -jar resources-processor.jar validate output/app.apk
```

**示例2: 包含DEX验证**
```bash
java -jar resources-processor.jar validate output/app.apk \
  --dex-path output/classes.dex \
  --dex-path output/classes2.dex
```

**示例3: 详细输出**
```bash
java -jar resources-processor.jar validate output/app.apk \
  --dex-path output/classes.dex \
  -v
```

---

## 全局选项

### -h, --help

显示命令帮助信息。

**适用范围**: 所有命令

**示例**:
```bash
# 主命令帮助
java -jar resources-processor.jar --help

# 子命令帮助
java -jar resources-processor.jar process-apk --help
java -jar resources-processor.jar scan --help
java -jar resources-processor.jar validate --help
```

### -V, --version

显示版本信息。

**适用范围**: 主命令

**示例**:
```bash
java -jar resources-processor.jar --version
```

**输出**:
```
Resources Processor 1.0.0
```

---

## 返回码

### 标准返回码

| 返回码 | 含义 | 说明 |
|--------|------|------|
| 0 | 成功 | 命令执行成功 |
| 1 | 失败 | 命令执行失败，详情见错误输出 |
| 2 | 用法错误 | 命令参数错误或未指定子命令 |

### 返回码使用

**Shell脚本**:
```bash
#!/bin/bash

java -jar resources-processor.jar process-apk input/app.apk -c config.yaml

if [ $? -eq 0 ]; then
  echo "处理成功"
else
  echo "处理失败"
  exit 1
fi
```

**批处理（Windows）**:
```batch
@echo off

java -jar resources-processor.jar process-apk input\app.apk -c config.yaml

if %ERRORLEVEL% EQU 0 (
  echo 处理成功
) else (
  echo 处理失败
  exit /b 1
)
```

**PowerShell**:
```powershell
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml

if ($LASTEXITCODE -eq 0) {
  Write-Host "处理成功"
} else {
  Write-Host "处理失败"
  exit 1
}
```

---

## 使用示例

### 场景1: 完整处理流程

```bash
# 1. 扫描APK（预览）
java -jar resources-processor.jar scan input/app.apk \
  -c config.yaml \
  -o reports/scan-report.txt

# 2. 检查扫描报告
cat reports/scan-report.txt

# 3. 处理APK
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk \
  --dex-path input/classes.dex \
  -v

# 4. 验证结果
java -jar resources-processor.jar validate output/app-processed.apk \
  --dex-path output/classes.dex \
  -v

# 5. 重新签名
apksigner sign --ks release.jks output/app-processed.apk

# 6. 验证签名
apksigner verify output/app-processed.apk
```

### 场景2: 批量处理

```bash
#!/bin/bash

# 批量处理多个APK
for apk in input/*.apk; do
  basename=$(basename "$apk" .apk)
  
  echo "处理: $basename"
  
  java -jar resources-processor.jar process-apk "$apk" \
    -c "config/${basename}-config.yaml" \
    -o "output/${basename}-processed.apk"
    
  if [ $? -eq 0 ]; then
    echo "✓ $basename 处理成功"
  else
    echo "✗ $basename 处理失败"
  fi
done
```

### 场景3: CI/CD集成

```yaml
# .gitlab-ci.yml
process-apk:
  stage: build
  script:
    - java -jar resources-processor.jar process-apk \
        build/outputs/apk/release/app-release.apk \
        -c config/production.yaml \
        -o build/outputs/apk/release/app-processed.apk \
        --dex-path build/outputs/apk/release/classes.dex
    - apksigner sign --ks $KEYSTORE_FILE \
        build/outputs/apk/release/app-processed.apk
  artifacts:
    paths:
      - build/outputs/apk/release/app-processed.apk
```

### 场景4: 调试和日志

```bash
# 启用详细日志并重定向到文件
java -jar resources-processor.jar process-apk input/app.apk \
  -c config.yaml \
  -o output/app-processed.apk \
  -v > process.log 2>&1

# 查看日志
less process.log

# 查看错误日志
grep "✗ 错误" process.log
grep "失败" process.log
```

### 场景5: 大型APK处理

```bash
# 增加JVM内存和GC调优
java -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar resources-processor.jar process-apk large-app.apk \
  -c config.yaml \
  -o output/large-app-processed.apk \
  -v
```

---

## 环境变量

### JAVA_OPTS

配置JVM参数。

**示例**:
```bash
export JAVA_OPTS="-Xmx4g -Dfile.encoding=UTF-8"
java $JAVA_OPTS -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

### 常用JVM参数

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `-Xmx` | 最大堆内存 | `4g` ~ `8g` |
| `-Xms` | 初始堆内存 | `2g` |
| `-Dfile.encoding` | 文件编码 | `UTF-8` |
| `-XX:+UseG1GC` | 使用G1垃圾回收器 | 推荐 |

**完整示例**:
```bash
java -Xmx4g \
  -Xms2g \
  -Dfile.encoding=UTF-8 \
  -XX:+UseG1GC \
  -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

---

## 配置文件

### 配置文件格式

配置文件使用YAML格式。

### 最小配置

```yaml
version: "1.0"

own_package_prefixes:
  - "com.myapp"

package_mappings:
  "com.myapp": "com.newapp"

class_mappings:
  "com.myapp.MainActivity": "com.newapp.MainActivity"
```

### 完整配置

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
  "com.myapp.ui.LoginActivity": "com.secure.app.ui.LoginActivity"

targets:
  - "res/layout/**/*.xml"
  - "res/menu/**/*.xml"
  - "resources.arsc"

dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"

options:
  process_tools_context: true
  enable_runtime_validation: false
  keep_backup: true
  parallel_processing: false
```

### 配置验证

验证配置文件语法：

```bash
# 使用Python验证YAML
python -c "import yaml; yaml.safe_load(open('config.yaml'))"
```

---

## 附录

### A. 命令速查表

| 任务 | 命令 |
|------|------|
| 查看帮助 | `java -jar rp.jar --help` |
| 查看版本 | `java -jar rp.jar --version` |
| 扫描APK | `java -jar rp.jar scan app.apk -c config.yaml` |
| 处理APK | `java -jar rp.jar process-apk app.apk -c config.yaml` |
| 指定输出 | `java -jar rp.jar process-apk app.apk -c config.yaml -o out.apk` |
| DEX验证 | `java -jar rp.jar process-apk app.apk -c config.yaml --dex-path classes.dex` |
| 验证APK | `java -jar rp.jar validate app.apk` |
| 详细输出 | `添加 -v 选项` |

### B. 故障排查

| 问题 | 解决方案 |
|------|---------|
| `找不到主类` | 使用Fat JAR（`-all.jar`） |
| `OutOfMemoryError` | 增加 `-Xmx` 参数 |
| `配置文件不存在` | 检查路径，使用绝对路径 |
| `APK文件不存在` | 检查路径，确认文件存在 |
| `DEX验证失败` | 检查类名映射是否正确 |

### C. 性能调优

```bash
# 大型APK（200MB+）
java -Xmx8g -XX:+UseG1GC -jar resources-processor.jar process-apk large.apk -c config.yaml

# 极大型APK（500MB+）
java -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar resources-processor.jar process-apk huge.apk -c config.yaml
```

---

**文档版本**: 1.0.0  
**最后更新**: 2025-10-20  
**维护者**: Resources Processor Team


