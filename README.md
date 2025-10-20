# Resources

工业生产级resources.arsc和二进制XML处理工具 - 用于Android APK包名/类名随机化场景

## ✨ 核心功能

- ✅ **resources.arsc处理**: 替换包名和全局字符串池中的类名/包名
- ✅ **二进制XML处理**: 支持layout、menu、navigation、xml配置
- ✅ **Data Binding支持**: 处理variable type、import type和T(FQCN)表达式
- ✅ **语义验证**: 区分类名/包名vs普通UI文案，避免误改
- ✅ **白名单过滤**: 仅替换自有包，保留系统/三方库
- ✅ **DEX交叉验证**: 确保新类名在DEX中存在
- ✅ **事务回滚**: 失败时自动恢复，零数据损坏风险
- ✅ **完整性验证**: aapt2静态验证 + 结构完整性检查

## 🏗️ 架构设计

```
resources_processor/
├── arsc_processor.py        # resources.arsc解析与处理
├── xml_processor.py          # 二进制XML解析与处理
├── databinding_processor.py  # Data Binding表达式处理
├── validator.py              # 语义验证器
├── whitelist.py              # 白名单过滤器
├── dex_validator.py          # DEX交叉验证
├── transaction.py            # 事务管理器
├── integrity.py              # 完整性验证器
├── processor.py              # 主处理器（编排）
└── cli.py                    # 命令行工具
```

## 📦 安装

```bash
# 从源码安装
git clone https://github.com/jp918898/Resources.git
cd Resources
pip install -e .

# 或安装依赖
pip install -r requirements.txt
```

## 🚀 快速开始

### Python API

```python
from resources_processor import ResourceProcessor

# 创建处理器，指定自有包名
processor = ResourceProcessor(own_packages=['com.myapp', 'org.myproject'])

# 添加文件
with open('resources.arsc', 'rb') as f:
    processor.add_arsc_file('resources.arsc', f.read())

with open('AndroidManifest.xml', 'rb') as f:
    processor.add_xml_file('AndroidManifest.xml', f.read())

with open('classes.dex', 'rb') as f:
    processor.add_dex_file('classes.dex', f.read())

# 替换包名
processor.replace_package_name('com.myapp', 'com.newapp')

# 替换类名
processor.replace_class_name('com.myapp.MainActivity', 'com.newapp.MainActivity')

# 验证完整性
is_valid, errors = processor.verify_integrity()
if is_valid:
    print("验证通过!")
else:
    print("验证失败:", errors)

# 获取修改后的数据
modified_data = processor.get_modified_data('resources.arsc')
```

### 命令行工具

```bash
# 替换包名
python -m resources_processor.cli replace-package \
    --old com.example.app \
    --new com.newpkg.app \
    --arsc path/to/resources.arsc \
    --xml path/to/AndroidManifest.xml \
    --own-packages com.example

# 替换类名
python -m resources_processor.cli replace-class \
    --old com.example.MainActivity \
    --new com.newpkg.MainActivity \
    --arsc path/to/resources.arsc \
    --xml path/to/AndroidManifest.xml \
    --dex path/to/classes.dex \
    --own-packages com.example

# 验证资源
python -m resources_processor.cli validate \
    --arsc path/to/resources.arsc \
    --xml path/to/AndroidManifest.xml \
    --dex path/to/classes.dex \
    --own-packages com.example

# 分析资源
python -m resources_processor.cli analyze \
    --arsc path/to/resources.arsc \
    --xml path/to/AndroidManifest.xml \
    --dex path/to/classes.dex \
    --output analysis.json
```

## 🔍 核心组件详解

### 1. ArscProcessor - resources.arsc处理器

解析和修改Android资源编译文件（resources.arsc）：

- 解析二进制格式的string pool
- 替换包名和类名
- 保持文件结构完整性

```python
from resources_processor import ArscProcessor

processor = ArscProcessor(arsc_data)
processor.parse()
processor.replace_package_name('com.old', 'com.new')
modified = processor.get_modified_data()
```

### 2. XmlProcessor - 二进制XML处理器

处理Android二进制XML文件（layout、menu、navigation等）：

- 支持多种XML类型
- 识别并替换类名引用
- 处理命名空间和属性

```python
from resources_processor import XmlProcessor

processor = XmlProcessor(xml_data, file_type='layout')
processor.parse()
processor.replace_class_name('com.old.MainActivity', 'com.new.MainActivity')
```

### 3. DataBindingProcessor - Data Binding处理器

专门处理Data Binding表达式：

- `<variable type="...">` 变量类型
- `<import type="...">` 导入类型
- `T(com.example.Class)` 表达式

```python
from resources_processor import DataBindingProcessor

processor = DataBindingProcessor(layout_xml_content)
processor.parse()
processor.replace_class_name('com.old.User', 'com.new.User')
```

### 4. SemanticValidator - 语义验证器

区分类名/包名和普通UI文案：

- 验证Java/Kotlin命名规范
- 检测UI文本模式
- 防止误改用户界面文案

```python
from resources_processor import SemanticValidator

validator = SemanticValidator()

# 验证类名
if validator.is_valid_class_name('com.example.MainActivity'):
    print("有效的类名")

# 验证包名
if validator.is_valid_package_name('com.example.app'):
    print("有效的包名")

# 检测UI文本
if validator.is_ui_text('Hello World'):
    print("这是UI文案，不应替换")
```

### 5. WhitelistFilter - 白名单过滤器

只处理自有包，保护系统和第三方库：

- 内置Android系统包列表
- 内置常见第三方库列表
- 支持自定义白名单

```python
from resources_processor import WhitelistFilter

filter = WhitelistFilter(own_packages=['com.myapp'])

# 检查是否应该处理
if filter.should_process('com.myapp.MainActivity'):
    print("处理此包")

# 过滤类名列表
filtered = filter.filter_class_names(all_classes)
```

### 6. DexValidator - DEX交叉验证器

验证类名在DEX中存在：

- 解析DEX文件格式
- 提取所有类定义
- 交叉验证类名引用

```python
from resources_processor import DexValidator

validator = DexValidator(dex_data)
validator.parse()

# 检查类是否存在
if validator.class_exists('com.example.MainActivity'):
    print("类在DEX中存在")

# 批量验证
results = validator.validate_class_names(class_set)
```

### 7. TransactionManager - 事务管理器

提供原子操作和回滚能力：

- 自动备份原始文件
- 失败时自动回滚
- 零数据损坏风险

```python
from resources_processor import TransactionManager

with TransactionManager() as tm:
    tm.backup_file('resources.arsc')
    
    # 执行操作...
    process_file('resources.arsc')
    
    # 成功则自动提交，失败则自动回滚
```

### 8. IntegrityVerifier - 完整性验证器

验证处理后的资源完整性：

- aapt2静态验证
- 文件格式验证
- 结构完整性检查

```python
from resources_processor import IntegrityVerifier

verifier = IntegrityVerifier()

# 验证resources.arsc
is_valid, errors = verifier.verify_resources_arsc('resources.arsc')

# 验证二进制XML
is_valid, errors = verifier.verify_binary_xml('AndroidManifest.xml')

# 使用aapt2验证APK
is_valid, errors = verifier.verify_with_aapt2('app.apk')
```

## 🧪 测试

```bash
# 运行所有测试
python -m pytest tests/

# 运行特定测试
python -m pytest tests/test_validator.py

# 运行单元测试
python -m unittest discover tests/
```

## 📝 使用场景

### 场景1: APK包名混淆

```python
processor = ResourceProcessor(own_packages=['com.original'])

# 加载APK中的资源文件
processor.add_arsc_file('resources.arsc', arsc_data)
processor.add_xml_file('AndroidManifest.xml', manifest_data)
processor.add_dex_file('classes.dex', dex_data)

# 替换包名
processor.replace_package_name('com.original', 'com.obfuscated.xyz')

# 验证并保存
if processor.verify_integrity()[0]:
    save_modified_files(processor)
```

### 场景2: 类名随机化

```python
import random
import string

def generate_random_name():
    return ''.join(random.choices(string.ascii_uppercase, k=8))

# 获取所有类引用
classes = processor.get_all_class_references()

# 为每个类生成新名称
for old_class in classes:
    package = validator.extract_package_from_class(old_class)
    new_name = generate_random_name()
    new_class = f"{package}.{new_name}"
    
    processor.replace_class_name(old_class, new_class)
```

### 场景3: 批量处理APK

```python
import zipfile
import os

def process_apk(apk_path, output_path, mappings):
    """处理APK文件，替换包名和类名"""
    
    with zipfile.ZipFile(apk_path, 'r') as zin:
        with zipfile.ZipFile(output_path, 'w') as zout:
            processor = ResourceProcessor()
            
            for item in zin.infolist():
                data = zin.read(item.filename)
                
                # 处理resources.arsc
                if item.filename == 'resources.arsc':
                    processor.add_arsc_file(item.filename, data)
                    # 应用映射...
                    data = processor.get_modified_data(item.filename)
                
                # 处理XML文件
                elif item.filename.endswith('.xml'):
                    processor.add_xml_file(item.filename, data)
                    # 应用映射...
                    data = processor.get_modified_data(item.filename)
                
                zout.writestr(item, data)
```

## 🛡️ 安全特性

1. **语义验证**: 自动识别并跳过UI文案，避免误改
2. **白名单机制**: 只处理指定的自有包，保护系统和第三方库
3. **DEX验证**: 确保所有类名引用都在代码中存在
4. **事务回滚**: 任何失败都能自动恢复原始文件
5. **完整性检查**: 使用aapt2和结构验证确保文件正确性

## ⚠️ 注意事项

1. **备份原始文件**: 虽然有事务回滚，仍建议手动备份重要文件
2. **DEX同步**: 修改资源后需同步修改DEX中的类定义
3. **签名更新**: 修改APK后需重新签名
4. **测试验证**: 修改后务必进行完整的功能测试
5. **aapt2依赖**: 完整性验证需要安装Android SDK的aapt2工具

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

## 📧 联系方式

- GitHub: [@jp918898](https://github.com/jp918898)
- Issues: [GitHub Issues](https://github.com/jp918898/Resources/issues)

---

**注意**: 本工具仅用于合法的APK分析和处理场景。使用时请遵守相关法律法规。
