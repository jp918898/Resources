# Resources Processor - 命令索引

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 命令总览和快速导航

---

## 📋 命令总览

Resources Processor提供3个主命令，用于处理Android APK资源文件。

| # | 命令 | 功能描述 | 修改APK | 配置文件 | 页面 |
|---|------|---------|:-------:|:--------:|------|
| 1 | `process-apk` | 处理APK，替换包名和类名 | ✅ | ✅ | [详情](#1-process-apk) |
| 2 | `scan` | 扫描APK，定位修改点 | ❌ | ✅ | [详情](#2-scan) |
| 3 | `validate` | 验证APK资源合法性 | ❌ | ❌ | [详情](#3-validate) |

---

## 🎯 按使用场景查找

### 场景1: 我要处理APK

**推荐命令**: [`process-apk`](#1-process-apk)

**典型用法**:
```bash
java -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

### 场景2: 我要预览修改点

**推荐命令**: [`scan`](#2-scan)

**典型用法**:
```bash
java -jar resources-processor.jar scan input/app.apk -c config.yaml
```

### 场景3: 我要验证APK是否正常

**推荐命令**: [`validate`](#3-validate)

**典型用法**:
```bash
java -jar resources-processor.jar validate output/app.apk
```

### 场景4: 完整工作流

```bash
# 1. 扫描预览
java -jar rp.jar scan input/app.apk -c config.yaml

# 2. 处理APK
java -jar rp.jar process-apk input/app.apk -c config.yaml -o output/app.apk

# 3. 验证结果
java -jar rp.jar validate output/app.apk
```

---

## 📖 命令详细说明

### 1. process-apk

#### 功能
处理APK文件，根据配置替换包名和类名。

#### 使用时机
- 代码混淆后，需要同步更新资源文件
- 制作马甲包，需要修改包名
- 代码重构后，批量更新资源引用

#### 基本语法
```bash
java -jar resources-processor.jar process-apk <APK文件> -c <配置文件> [选项]
```

#### 必需参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `<APK文件>` | 位置参数 | 输入APK文件路径 |
| `-c, --config` | 选项 | 配置文件路径（YAML） |

#### 可选参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `-o, --output` | String | 输出APK路径 |
| `--dex-path` | String[] | DEX文件路径（可多次指定） |
| `--auto-sign` / `--no-auto-sign` | Boolean | 启用/禁用自动对齐和签名（默认启用） |
| `-v, --verbose` | Boolean | 详细输出模式 |

#### 常用示例
```bash
# 基本使用（默认自动对齐和签名）
java -jar rp.jar process-apk input/app.apk -c config.yaml

# 指定输出路径
java -jar rp.jar process-apk input/app.apk -c config.yaml -o output/app.apk

# 启用DEX验证
java -jar rp.jar process-apk input/app.apk -c config.yaml --dex-path classes.dex -v

# 禁用自动签名（正式发布）
java -jar rp.jar process-apk input/app.apk -c config.yaml --no-auto-sign
```

#### 返回码
- `0` - 成功
- `1` - 失败

#### 相关文档
- [CLI完整参考 - process-apk](CLI_REFERENCE.md#process-apk)
- [用户手册 - 命令使用指南](USER_MANUAL.md#命令使用指南)

---

### 2. scan

#### 功能
扫描APK，定位所有需要修改的位置，但**不修改**APK。

#### 使用时机
- 处理前预览将要修改的位置
- 检查配置文件是否正确
- 生成修改点报告供审核

#### 基本语法
```bash
java -jar resources-processor.jar scan <APK文件> -c <配置文件> [选项]
```

#### 必需参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `<APK文件>` | 位置参数 | 输入APK文件路径 |
| `-c, --config` | 选项 | 配置文件路径（YAML） |

#### 可选参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `-o, --output` | String | 报告输出路径 |
| `-v, --verbose` | Boolean | 详细输出模式 |

#### 常用示例
```bash
# 扫描并显示结果
java -jar rp.jar scan input/app.apk -c config.yaml

# 保存扫描报告
java -jar rp.jar scan input/app.apk -c config.yaml -o reports/scan.txt

# 详细输出
java -jar rp.jar scan input/app.apk -c config.yaml -v
```

#### 输出内容
- 需要修改的文件列表
- 每个文件中的修改点位置
- 修改前后的值对比
- 总修改点数量统计

#### 返回码
- `0` - 成功
- `1` - 失败

#### 相关文档
- [CLI完整参考 - scan](CLI_REFERENCE.md#scan)
- [用户手册 - 命令使用指南](USER_MANUAL.md#命令2-scan)

---

### 3. validate

#### 功能
验证APK资源的合法性，包括aapt2静态验证和DEX加载验证。

#### 使用时机
- 处理后验证APK是否正常
- 检查APK资源完整性
- 验证DEX文件可加载性

#### 基本语法
```bash
java -jar resources-processor.jar validate <APK文件> [选项]
```

#### 必需参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `<APK文件>` | 位置参数 | 输入APK文件路径 |

#### 可选参数
| 参数 | 类型 | 说明 |
|------|------|------|
| `--dex-path` | String[] | DEX文件路径（可多次指定） |
| `-v, --verbose` | Boolean | 详细输出模式 |

#### 常用示例
```bash
# 基本验证（仅aapt2）
java -jar rp.jar validate output/app.apk

# 包含DEX验证
java -jar rp.jar validate output/app.apk --dex-path classes.dex

# 多DEX验证
java -jar rp.jar validate output/app.apk \
  --dex-path classes.dex \
  --dex-path classes2.dex \
  -v
```

#### 验证内容
- **aapt2静态验证**: 检查resources.arsc和XML文件合法性
- **DEX加载验证**: 检查DEX文件可加载性和类数量

#### 返回码
- `0` - 验证通过
- `1` - 验证失败

#### 相关文档
- [CLI完整参考 - validate](CLI_REFERENCE.md#validate)
- [用户手册 - 命令使用指南](USER_MANUAL.md#命令3-validate)

---

## 🔧 全局选项

这些选项适用于所有命令：

| 选项 | 说明 | 示例 |
|------|------|------|
| `-h, --help` | 显示帮助信息 | `java -jar rp.jar --help` |
| `-V, --version` | 显示版本信息 | `java -jar rp.jar --version` |

---

## 📊 参数对比表

### 参数可用性

| 参数 | process-apk | scan | validate |
|------|:-----------:|:----:|:--------:|
| `<APK文件>` | ✅ | ✅ | ✅ |
| `-c, --config` | ✅ 必需 | ✅ 必需 | ❌ |
| `-o, --output` | ✅ | ✅ | ❌ |
| `--dex-path` | ✅ | ❌ | ✅ |
| `-v, --verbose` | ✅ | ✅ | ✅ |

### 命令特点对比

| 特性 | process-apk | scan | validate |
|------|-------------|------|----------|
| 修改APK | ✅ | ❌ | ❌ |
| 需要配置文件 | ✅ | ✅ | ❌ |
| 生成报告 | ✅ | ✅ | ✅ |
| DEX验证 | ✅ | ❌ | ✅ |
| aapt2验证 | ✅ | ❌ | ✅ |
| 事务回滚 | ✅ | ❌ | ❌ |

---

## 🔄 命令执行流程

### process-apk 流程

```
输入APK
  ↓
加载配置
  ↓
开启事务（创建快照）
  ↓
Phase 1: 扫描定位
  ↓
Phase 2: 预验证
  ↓
Phase 3: 执行替换
  ↓
Phase 4: 后验证（aapt2）
  ↓
提交事务
  ↓
输出APK
```

### scan 流程

```
输入APK
  ↓
加载配置
  ↓
扫描resources.arsc
  ↓
扫描XML文件
  ↓
汇总修改点
  ↓
生成报告
  ↓
输出/保存报告
```

### validate 流程

```
输入APK
  ↓
aapt2静态验证
  ↓
DEX加载验证（可选）
  ↓
汇总验证结果
  ↓
输出验证报告
```

---

## 💡 使用建议

### 建议1: 遵循标准流程

```bash
scan → process-apk → validate
```

**理由**: 
- `scan` 预览修改点，确保配置正确
- `process-apk` 执行处理
- `validate` 验证结果

### 建议2: 启用DEX验证

```bash
# process-apk 时指定 --dex-path
java -jar rp.jar process-apk app.apk -c config.yaml --dex-path classes.dex

# validate 时也验证DEX
java -jar rp.jar validate app.apk --dex-path classes.dex
```

**理由**: 避免类名映射错误

### 建议3: 保存扫描报告

```bash
java -jar rp.jar scan app.apk -c config.yaml -o scan-$(date +%Y%m%d).txt
```

**理由**: 便于审核和存档

### 建议4: 详细输出用于调试

```bash
java -jar rp.jar process-apk app.apk -c config.yaml -v > process.log 2>&1
```

**理由**: 完整日志便于排查问题

---

## 📚 相关文档索引

### 新手入门
1. [快速开始](USER_MANUAL.md#快速入门) - 5分钟快速上手
2. [安装与配置](USER_MANUAL.md#安装与配置) - 环境准备
3. [CLI快速参考](CLI_QUICK_REFERENCE.md) - 速查表

### 命令参考
1. [CLI完整参考](CLI_REFERENCE.md) - 所有命令详细说明
2. [CLI快速参考](CLI_QUICK_REFERENCE.md) - 命令速查卡片
3. [本文档] - 命令总览和导航

### 深入学习
1. [用户手册](USER_MANUAL.md) - 完整使用指南
2. [架构设计](ARCHITECTURE.md) - 系统架构和原理
3. [开发指南](DEVELOPMENT_GUIDE.md) - 贡献和扩展开发

### 实施与维护
1. [构建与运行](BUILD_AND_RUN.md) - 构建、测试、调试
2. [修复实施报告](FIXES_IMPLEMENTED.md) - 缺陷修复记录
3. [API参考](API_REFERENCE.md) - 公共API文档

---

## 🔍 快速查找

### 按关键字查找

| 关键字 | 推荐命令 | 页面 |
|--------|---------|------|
| 处理APK | `process-apk` | [详情](#1-process-apk) |
| 替换包名 | `process-apk` | [详情](#1-process-apk) |
| 替换类名 | `process-apk` | [详情](#1-process-apk) |
| 预览修改 | `scan` | [详情](#2-scan) |
| 扫描APK | `scan` | [详情](#2-scan) |
| 生成报告 | `scan` | [详情](#2-scan) |
| 验证APK | `validate` | [详情](#3-validate) |
| aapt2验证 | `validate` | [详情](#3-validate) |
| DEX验证 | `validate` | [详情](#3-validate) |

### 按功能查找

| 功能 | 命令 | 参数 |
|------|------|------|
| 修改APK | `process-apk` | `-c config.yaml` |
| 指定输出路径 | `process-apk` | `-o output.apk` |
| 保存报告 | `scan` | `-o report.txt` |
| DEX交叉验证 | `process-apk` / `validate` | `--dex-path classes.dex` |
| 详细日志 | 任何命令 | `-v` |
| 查看帮助 | 任何命令 | `--help` |

---

## ❓ 常见问题

### Q: 我应该先用哪个命令？

**A**: 推荐先用 `scan` 命令预览修改点：
```bash
java -jar rp.jar scan input/app.apk -c config.yaml
```

### Q: 如何查看命令帮助？

**A**: 使用 `--help` 选项：
```bash
java -jar rp.jar --help                    # 主命令帮助
java -jar rp.jar process-apk --help        # process-apk帮助
java -jar rp.jar scan --help               # scan帮助
java -jar rp.jar validate --help           # validate帮助
```

### Q: 处理后如何验证结果？

**A**: 使用 `validate` 命令：
```bash
java -jar rp.jar validate output/app.apk --dex-path classes.dex -v
```

### Q: 命令执行失败如何查看详细错误？

**A**: 添加 `-v` 参数启用详细输出：
```bash
java -jar rp.jar process-apk app.apk -c config.yaml -v
```

### Q: 如何处理大型APK？

**A**: 增加JVM内存：
```bash
java -Xmx4g -jar rp.jar process-apk large-app.apk -c config.yaml
```

---

## 📞 获取帮助

### 命令行帮助
```bash
java -jar resources-processor.jar --help
```

### 在线资源
- **项目主页**: https://github.com/frezrik/jiagu-resources
- **问题反馈**: https://github.com/frezrik/jiagu-resources/issues
- **文档Wiki**: https://github.com/frezrik/jiagu-resources/wiki

### 文档导航
- 📘 [用户手册](USER_MANUAL.md) - 完整使用指南
- 📗 [CLI完整参考](CLI_REFERENCE.md) - 详细命令说明
- 📙 [CLI快速参考](CLI_QUICK_REFERENCE.md) - 速查卡片
- 🏗️ [架构设计](ARCHITECTURE.md) - 系统架构
- 🔨 [构建指南](BUILD_AND_RUN.md) - 构建和测试

---

**命令索引** | v1.0.0 | 2025-10-20 | Resources Processor Team

