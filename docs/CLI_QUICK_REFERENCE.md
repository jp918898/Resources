# Resources Processor - CLI快速参考卡片

**版本**: 1.0.0 | **更新**: 2025-10-20

---

## 🚀 快速开始

```bash
# 1. 扫描APK（预览）
java -jar resources-processor.jar scan input/app.apk -c config.yaml

# 2. 处理APK
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml

# 3. 验证APK
java -jar resources-processor.jar validate output/app.apk
```

---

## 📋 命令总览

| 命令 | 功能 | 修改APK | 需要配置 |
|------|------|:-------:|:--------:|
| `process-apk` | 处理APK，替换包名和类名 | ✅ | ✅ |
| `scan` | 扫描APK，定位修改点 | ❌ | ✅ |
| `validate` | 验证APK资源合法性 | ❌ | ❌ |

---

## 🔧 process-apk

**功能**: 处理APK文件

**语法**:
```bash
java -jar resources-processor.jar process-apk <APK> -c <CONFIG> [OPTIONS]
```

**必需参数**:
- `<APK>` - APK文件路径
- `-c <CONFIG>` - 配置文件路径

**可选参数**:
- `-o <FILE>` - 输出APK路径
- `--dex-path <FILE>` - DEX文件路径（可多次指定）
- `--auto-sign` / `--no-auto-sign` - 启用/禁用自动对齐和签名（默认启用）
- `-v` - 详细输出

**示例**:
```bash
# 基本使用（默认自动对齐和签名）
java -jar rp.jar process-apk app.apk -c config.yaml

# 指定输出
java -jar rp.jar process-apk app.apk -c config.yaml -o output/app.apk

# 启用DEX验证
java -jar rp.jar process-apk app.apk -c config.yaml \
  --dex-path classes.dex --dex-path classes2.dex -v

# 禁用自动签名（手动签名）
java -jar rp.jar process-apk app.apk -c config.yaml --no-auto-sign
```

---

## 🔍 scan

**功能**: 扫描APK（不修改）

**语法**:
```bash
java -jar resources-processor.jar scan <APK> -c <CONFIG> [OPTIONS]
```

**必需参数**:
- `<APK>` - APK文件路径
- `-c <CONFIG>` - 配置文件路径

**可选参数**:
- `-o <FILE>` - 报告输出路径
- `-v` - 详细输出

**示例**:
```bash
# 扫描并显示
java -jar rp.jar scan app.apk -c config.yaml

# 保存报告
java -jar rp.jar scan app.apk -c config.yaml -o report.txt
```

---

## ✅ validate

**功能**: 验证APK资源

**语法**:
```bash
java -jar resources-processor.jar validate <APK> [OPTIONS]
```

**必需参数**:
- `<APK>` - APK文件路径

**可选参数**:
- `--dex-path <FILE>` - DEX文件路径（可多次指定）
- `-v` - 详细输出

**示例**:
```bash
# 基本验证
java -jar rp.jar validate output/app.apk

# 包含DEX验证
java -jar rp.jar validate output/app.apk --dex-path classes.dex -v
```

---

## ⚙️ 配置文件

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

# 自有包前缀（白名单）
own_package_prefixes:
  - "com.myapp"
  - "com.mycompany"

# 包名映射
package_mappings:
  "com.myapp": "com.secure.app"

# 类名映射
class_mappings:
  "com.myapp.MainActivity": "com.secure.app.MainActivity"
  "com.myapp.ui.LoginActivity": "com.secure.app.ui.LoginActivity"

# DEX路径
dex_paths:
  - "input/classes.dex"
  - "input/classes2.dex"

# 高级选项
options:
  process_tools_context: true
  keep_backup: true
  parallel_processing: false
```

---

## 🔄 典型工作流

```bash
# 第1步: 扫描（预览修改点）
java -jar rp.jar scan input/app.apk -c config.yaml -o scan.txt

# 第2步: 检查扫描报告
cat scan.txt

# 第3步: 处理APK
java -jar rp.jar process-apk input/app.apk -c config.yaml -o output/app.apk

# 第4步: 验证结果
java -jar rp.jar validate output/app.apk --dex-path output/classes.dex

# 第5步: 验证签名（已自动签名）
apksigner verify output/app.apk

# 第6步: 安装测试
adb install -r output/app.apk

# 注: 如使用 --no-auto-sign，需在第5步前手动签名:
# apksigner sign --ks release.jks output/app.apk
```

---

## 💡 常用选项

| 选项 | 简写 | 说明 |
|------|------|------|
| `--help` | `-h` | 显示帮助 |
| `--version` | `-V` | 显示版本 |
| `--config` | `-c` | 配置文件 |
| `--output` | `-o` | 输出文件 |
| `--verbose` | `-v` | 详细输出 |
| `--dex-path` | - | DEX文件 |

---

## 📊 返回码

| 返回码 | 含义 |
|--------|------|
| `0` | ✅ 成功 |
| `1` | ❌ 失败 |
| `2` | ⚠️ 用法错误 |

**使用示例**:
```bash
java -jar rp.jar process-apk app.apk -c config.yaml
if [ $? -eq 0 ]; then
  echo "成功"
else
  echo "失败"
fi
```

---

## 🔥 高级用法

### 大型APK处理

```bash
# 增加JVM内存
java -Xmx4g -jar rp.jar process-apk large-app.apk -c config.yaml

# 大型APK + 详细日志
java -Xmx8g -jar rp.jar process-apk huge-app.apk -c config.yaml -v
```

### 批量处理

```bash
# 处理多个APK
for apk in input/*.apk; do
  java -jar rp.jar process-apk "$apk" -c config.yaml -o "output/$(basename $apk)"
done
```

### 日志重定向

```bash
# 保存完整日志
java -jar rp.jar process-apk app.apk -c config.yaml -v > process.log 2>&1

# 仅保存错误
java -jar rp.jar process-apk app.apk -c config.yaml 2> error.log
```

---

## ⚠️ 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| `APK文件不存在` | 文件路径错误 | 检查路径 |
| `配置文件不存在` | 配置路径错误 | 检查路径 |
| `OutOfMemoryError` | 内存不足 | 增加 `-Xmx` |
| `DEX验证失败` | 类名映射错误 | 检查配置 |
| `aapt2验证失败` | APK损坏 | 检查处理日志 |

---

## 📌 最佳实践

✅ **始终先扫描**: 在处理前先用 `scan` 预览修改点

✅ **启用DEX验证**: 配置 `dex_paths` 避免映射错误

✅ **保留备份**: 设置 `keep_backup: true`

✅ **详细日志**: 处理时加 `-v` 参数

✅ **测试验证**: 处理后使用 `validate` 命令验证

---

## 🛠️ JVM调优

```bash
# 标准配置（10-50MB APK）
java -Xmx2g -jar rp.jar process-apk app.apk -c config.yaml

# 大型APK（50-100MB）
java -Xmx4g -XX:+UseG1GC -jar rp.jar process-apk app.apk -c config.yaml

# 超大APK（100MB+）
java -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -jar rp.jar process-apk app.apk -c config.yaml
```

---

## 🔗 相关文档

- 📘 [完整用户手册](USER_MANUAL.md)
- 📗 [CLI完整参考](CLI_REFERENCE.md)
- 📙 [命令索引](CLI_COMMANDS_INDEX.md)
- 🏗️ [架构设计](ARCHITECTURE.md)
- 🔨 [构建指南](BUILD_AND_RUN.md)

---

## 📞 获取帮助

```bash
# 查看主命令帮助
java -jar rp.jar --help

# 查看子命令帮助
java -jar rp.jar process-apk --help
java -jar rp.jar scan --help
java -jar rp.jar validate --help
```

**在线帮助**:
- GitHub: https://github.com/frezrik/jiagu-resources
- Issues: https://github.com/frezrik/jiagu-resources/issues
- Wiki: https://github.com/frezrik/jiagu-resources/wiki

---

**快速参考卡片** | v1.0.0 | 2025-10-20

