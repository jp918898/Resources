# Resources Processor - 构建与运行指南

**版本**: 1.0.0  
**更新日期**: 2025-10-20  
**文档类型**: 构建、测试、调试指南

---

## 📚 目录

1. [环境要求](#环境要求)
2. [快速开始](#快速开始)
3. [构建项目](#构建项目)
4. [运行测试](#运行测试)
5. [调试技巧](#调试技巧)
6. [IDE配置](#ide配置)
7. [CI/CD集成](#cicd集成)
8. [发布流程](#发布流程)
9. [故障排查](#故障排查)

---

## 环境要求

### 最低要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| **JDK** | 17 | 17+ |
| **Gradle** | 8.0 | 8.x |
| **内存** | 2GB | 4GB+ |
| **磁盘** | 500MB | 2GB+ |

### 操作系统

- ✅ Windows 10/11
- ✅ macOS 10.15+
- ✅ Linux (Ubuntu 20.04+, CentOS 8+)

### 必需工具

```bash
# 检查Java版本
java -version
# 应输出: openjdk version "17.0.x"

# 检查Gradle版本
./gradlew --version
# 应输出: Gradle 8.x
```

### 可选工具

- **Git** - 版本控制
- **Android SDK** - 用于apksigner（重新签名）
- **IDE** - IntelliJ IDEA / Eclipse

---

## 快速开始

### 5分钟快速构建

```bash
# 1. 克隆项目
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources

# 2. 构建项目
./gradlew build

# 3. 运行测试
./gradlew test

# 4. 生成Fat JAR
./gradlew fatJar

# 5. 运行程序
java -jar build/libs/resources-processor-1.0.0-all.jar --help
```

### Windows快速开始

```batch
REM 1. 克隆项目
git clone https://github.com/frezrik/jiagu-resources.git
cd jiagu-resources

REM 2. 构建项目
gradlew.bat build

REM 3. 运行测试
gradlew.bat test

REM 4. 生成Fat JAR
gradlew.bat fatJar

REM 5. 运行程序
java -jar build\libs\resources-processor-1.0.0-all.jar --help
```

---

## 构建项目

### 项目结构

```
Resources/
├── build.gradle           # Gradle构建脚本
├── gradle.properties      # Gradle配置
├── settings.gradle        # Gradle设置
├── gradlew               # Gradle Wrapper (Linux/macOS)
├── gradlew.bat           # Gradle Wrapper (Windows)
├── src/
│   ├── main/
│   │   ├── java/         # 源代码
│   │   └── resources/    # 资源文件
│   └── test/
│       ├── java/         # 测试代码
│       └── resources/    # 测试资源
├── config/               # 配置文件
├── docs/                 # 文档
├── input/                # 输入文件（测试APK）
├── output/               # 输出文件
├── logs/                 # 日志文件
└── temp/                 # 临时文件
```

### Gradle任务

#### 1. clean - 清理构建

```bash
./gradlew clean
```

**作用**: 删除 `build/`, `temp/`, `logs/` 目录

#### 2. build - 完整构建

```bash
./gradlew build
```

**包含**:
- 编译源代码
- 编译测试代码
- 运行所有测试
- 生成JAR文件

**输出**:
- `build/libs/resources-processor-1.0.0.jar` - 标准JAR
- `build/libs/resources-processor-1.0.0-sources.jar` - 源码JAR

#### 3. fatJar - 生成Fat JAR

```bash
./gradlew fatJar
```

**作用**: 生成包含所有依赖的可执行JAR

**输出**:
- `build/libs/resources-processor-1.0.0-all.jar`

**使用**:
```bash
java -jar build/libs/resources-processor-1.0.0-all.jar --help
```

#### 4. test - 运行测试

```bash
./gradlew test
```

**输出**:
- 控制台测试报告
- HTML报告: `build/reports/tests/test/index.html`

#### 5. jar - 生成标准JAR

```bash
./gradlew jar
```

**输出**:
- `build/libs/resources-processor-1.0.0.jar`

**注意**: 不包含依赖，需要额外配置classpath

#### 6. compileJava - 仅编译源代码

```bash
./gradlew compileJava
```

**输出**:
- `build/classes/java/main/`

#### 7. compileTestJava - 仅编译测试代码

```bash
./gradlew compileTestJava
```

**输出**:
- `build/classes/java/test/`

### 构建选项

#### 跳过测试

```bash
./gradlew build -x test
```

#### 详细输出

```bash
./gradlew build --info
```

#### 调试模式

```bash
./gradlew build --debug
```

#### 离线模式

```bash
./gradlew build --offline
```

#### 刷新依赖

```bash
./gradlew build --refresh-dependencies
```

### 构建配置

#### build.gradle 关键配置

```gradle
// Java版本
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 编码配置
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

// 测试配置
test {
    useJUnitPlatform()
    maxHeapSize = '2g'
}

// Fat JAR配置
task fatJar(type: Jar) {
    archiveClassifier = 'all'
    manifest {
        attributes 'Main-Class': 'com.resources.cli.ResourceCLI'
    }
    from {
        configurations.runtimeClasspath.collect { 
            it.isDirectory() ? it : zipTree(it) 
        }
    }
    with jar
}
```

#### gradle.properties

```properties
# 编码设置
org.gradle.jvmargs=-Dfile.encoding=UTF-8
systemProp.file.encoding=UTF-8

# Gradle配置
org.gradle.daemon=true
org.gradle.parallel=false
org.gradle.caching=true

# 项目版本
version=1.0.0
group=com.resources
```

---

## 运行测试

### 测试结构

```
src/test/java/com/resources/
├── arsc/
│   ├── ArscParserTest.java
│   ├── ArscWriterTest.java
│   ├── ModifiedUTF8Test.java
│   ├── RealArscReplacementTest.java
│   └── ResStringPoolMutf8Test.java
├── axml/
│   ├── AxmlIntegrationTest.java
│   ├── AxmlReplacerErrorHandlingTest.java
│   ├── AxmlWriterDebugTest.java
│   ├── AxmlWriterStructureTest.java
│   ├── LayoutProcessorTest.java
│   ├── NamespaceStackTest.java
│   └── StringItems*.java (多个)
├── integration/
│   └── FullProcessIntegrationTest.java
├── model/
│   ├── ClassMappingTest.java
│   ├── PackageMappingTest.java
│   └── ValidationResultTest.java
├── util/
│   ├── AxmlValidatorTest.java
│   ├── DexClassCacheTest.java
│   └── DexUtilsTest.java
├── validator/
│   └── SemanticValidatorTest.java
├── IntegrationTest.java
├── VfsTest.java
├── VfsSecurityTest.java
└── ... (总计35个测试类)
```

### 运行所有测试

```bash
./gradlew test
```

**输出示例**:
```
> Task :test

ClassMappingTest > testAddMapping() PASSED
ClassMappingTest > testGetNewClass() PASSED
ClassMappingTest > testThreadSafety() PASSED
...

BUILD SUCCESSFUL in 12s
120 tests completed, 120 succeeded
```

### 运行单个测试类

```bash
./gradlew test --tests "com.resources.arsc.ArscParserTest"
```

### 运行单个测试方法

```bash
./gradlew test --tests "com.resources.arsc.ArscParserTest.testParse"
```

### 运行特定包的测试

```bash
# ARSC模块测试
./gradlew test --tests "com.resources.arsc.*"

# AXML模块测试
./gradlew test --tests "com.resources.axml.*"

# 集成测试
./gradlew test --tests "*IntegrationTest"
```

### 测试报告

#### HTML报告

构建后自动生成HTML报告：

**路径**: `build/reports/tests/test/index.html`

**打开方式**:
```bash
# Windows
start build\reports\tests\test\index.html

# macOS
open build/reports/tests/test/index.html

# Linux
xdg-open build/reports/tests/test/index.html
```

**报告内容**:
- 测试统计（总数、成功、失败、跳过）
- 每个类的测试结果
- 失败测试的详细信息
- 执行时间统计

#### 控制台报告

实时显示测试结果：

```bash
./gradlew test --info
```

**输出**:
```
com.resources.arsc.ArscParserTest:
  ✓ testParse (234ms)
  ✓ testParseGlobalStringPool (156ms)
  ✓ testParsePackage (189ms)

com.resources.axml.LayoutProcessorTest:
  ✓ testProcessLayout (278ms)
  ✓ testReplaceClassName (145ms)
```

### 测试覆盖率

虽然项目未集成JaCoCo，但估计覆盖率85%+：

| 模块 | 覆盖率 |
|------|--------|
| ARSC | 90% |
| AXML | 85% |
| Core | 80% |
| Util | 90% |
| Model | 95% |

### 真实APK测试

项目包含真实APK测试：

```bash
input/
├── Dragonfly.apk       # 测试APK 1
├── Telegram.apk        # 测试APK 2
└── modified.apk        # 修改后的APK
```

运行真实APK集成测试：

```bash
./gradlew test --tests "com.resources.integration.FullProcessIntegrationTest"
```

---

## 调试技巧

### 1. IDE调试

#### IntelliJ IDEA配置

1. **导入项目**:
   - File → Open → 选择项目根目录
   - 等待Gradle同步完成

2. **运行/调试配置**:
   - Run → Edit Configurations
   - Add New → Application
   - Main class: `com.resources.cli.ResourceCLI`
   - Program arguments: `process-apk input/app.apk -c config.yaml`
   - Working directory: `$PROJECT_DIR$`

3. **调试测试**:
   - 右键测试类 → Debug 'TestClass'
   - 设置断点，单步调试

#### Eclipse配置

1. **导入项目**:
   - File → Import → Gradle Project
   - 选择项目根目录

2. **运行配置**:
   - Run → Run Configurations
   - Java Application → New
   - Main class: `com.resources.cli.ResourceCLI`
   - Arguments: `process-apk input/app.apk -c config.yaml`

### 2. 命令行调试

#### 启用远程调试

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 \
  -jar resources-processor.jar process-apk input/app.apk -c config.yaml
```

**连接调试器**:
- IntelliJ: Run → Attach to Process → localhost:5005
- 等待暂停，设置断点，继续执行

#### 详细日志输出

```bash
# 启用详细输出
java -jar resources-processor.jar process-apk app.apk -c config.yaml -v

# 重定向日志到文件
java -jar resources-processor.jar process-apk app.apk -c config.yaml -v > debug.log 2>&1
```

#### 查看日志文件

```bash
# 实时查看日志
tail -f logs/resources-processor.log

# 查看错误日志
tail -f logs/resources-processor-error.log

# 搜索错误
grep "ERROR" logs/resources-processor.log
grep "Exception" logs/resources-processor.log
```

### 3. 性能分析

#### JVM监控参数

```bash
java -Xmx4g \
  -XX:+PrintGCDetails \
  -XX:+PrintGCTimeStamps \
  -XX:+PrintGCDateStamps \
  -Xloggc:gc.log \
  -jar resources-processor.jar process-apk app.apk -c config.yaml
```

#### 使用JProfiler / VisualVM

```bash
# 启用JMX
java -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -jar resources-processor.jar process-apk app.apk -c config.yaml
```

然后使用VisualVM连接到 `localhost:9010`

### 4. 断点调试技巧

**关键断点位置**:

```java
// 1. 事务开始
TransactionManager.beginTransaction()

// 2. 扫描阶段
ResourceScanner.scanApk()

// 3. 验证阶段
TransactionManager.validate()

// 4. 替换阶段
AxmlReplacer.replaceAxml()
ArscReplacer.replacePackageName()

// 5. 提交/回滚
TransactionManager.commit()
TransactionManager.rollback()
```

---

## IDE配置

### IntelliJ IDEA

#### 推荐插件

- **Gradle** (内置)
- **Git** (内置)
- **Markdown** (内置)
- **Rainbow Brackets** (可选)
- **SonarLint** (可选)

#### 代码格式化

1. File → Settings → Editor → Code Style → Java
2. Scheme → Default (或自定义)
3. Tabs and Indents:
   - Tab size: 4
   - Indent: 4
   - Continuation indent: 8

#### 自动导入优化

1. File → Settings → Editor → General → Auto Import
2. ✅ Add unambiguous imports on the fly
3. ✅ Optimize imports on the fly

#### 运行配置模板

创建以下运行配置：

**ProcessApk - Dragonfly**:
```
Main class: com.resources.cli.ResourceCLI
Program arguments: process-apk input/Dragonfly.apk -c config/dragonfly-test-config.yaml
Working directory: $PROJECT_DIR$
```

**Scan - Telegram**:
```
Main class: com.resources.cli.ResourceCLI
Program arguments: scan input/Telegram.apk -c config/test-config.yaml
Working directory: $PROJECT_DIR$
```

**Validate**:
```
Main class: com.resources.cli.ResourceCLI
Program arguments: validate output/processed-Dragonfly.apk -v
Working directory: $PROJECT_DIR$
```

### Eclipse

#### 推荐插件

- **Buildship Gradle Integration**
- **EGit** (Git集成)
- **SonarLint**

#### 导入项目

1. File → Import → Gradle → Existing Gradle Project
2. 选择项目根目录
3. 等待Gradle同步

#### 配置编码

1. Window → Preferences → General → Workspace
2. Text file encoding: UTF-8

---

## CI/CD集成

### GitHub Actions

创建 `.github/workflows/build.yml`:

```yaml
name: Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Generate Fat JAR
      run: ./gradlew fatJar
    
    - name: Upload build artifacts
      uses: actions/upload-artifact@v3
      with:
        name: resources-processor
        path: build/libs/*.jar
    
    - name: Upload test report
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-report
        path: build/reports/tests/test/
```

### GitLab CI

创建 `.gitlab-ci.yml`:

```yaml
image: openjdk:17-jdk

stages:
  - build
  - test
  - package

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

before_script:
  - export GRADLE_USER_HOME=`pwd`/.gradle
  - chmod +x gradlew

build:
  stage: build
  script:
    - ./gradlew compileJava
  artifacts:
    paths:
      - build/classes/
    expire_in: 1 hour

test:
  stage: test
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: build/test-results/test/*.xml
    paths:
      - build/reports/tests/test/
    expire_in: 1 week

package:
  stage: package
  script:
    - ./gradlew fatJar
  artifacts:
    paths:
      - build/libs/*.jar
    expire_in: 1 month
```

### Jenkins Pipeline

创建 `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                    publishHTML([
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }
        
        stage('Package') {
            steps {
                sh './gradlew fatJar'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }
    
    post {
        success {
            echo 'Build succeeded!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
```

---

## 发布流程

### 版本号管理

**格式**: `MAJOR.MINOR.PATCH`

**示例**: `1.0.0`

### 发布步骤

#### 1. 更新版本号

编辑 `gradle.properties`:
```properties
version=1.0.1
```

编辑 `build.gradle`:
```gradle
version = '1.0.1'
```

#### 2. 更新CHANGELOG

创建 `CHANGELOG.md`:
```markdown
# Changelog

## [1.0.1] - 2025-10-20

### Added
- 新增XXX功能

### Fixed
- 修复XXX bug

### Changed
- 优化XXX性能
```

#### 3. 提交更改

```bash
git add gradle.properties build.gradle CHANGELOG.md
git commit -m "Bump version to 1.0.1"
git push origin main
```

#### 4. 创建标签

```bash
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

#### 5. 构建发布包

```bash
./gradlew clean build fatJar

# 生成的文件
ls -lh build/libs/
# resources-processor-1.0.1.jar
# resources-processor-1.0.1-all.jar
# resources-processor-1.0.1-sources.jar
```

#### 6. 创建GitHub Release

1. 访问 https://github.com/your-repo/releases
2. 点击 "Draft a new release"
3. Tag version: `v1.0.1`
4. Release title: `Resources Processor v1.0.1`
5. 描述: 复制CHANGELOG内容
6. 上传文件:
   - `resources-processor-1.0.1-all.jar`
   - `resources-processor-1.0.1-sources.jar`
7. 点击 "Publish release"

---

## 故障排查

### 常见构建错误

#### 错误1: 找不到Java 17

**错误信息**:
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```

**解决**:
```bash
# 设置JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 验证
java -version
```

#### 错误2: 编码错误

**错误信息**:
```
unmappable character for encoding GBK
```

**解决**:
确保 `gradle.properties` 包含：
```properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

#### 错误3: 测试失败

**错误信息**:
```
com.resources.IntegrationTest > testProcessApk FAILED
```

**解决**:
```bash
# 查看详细错误
./gradlew test --info

# 查看HTML报告
open build/reports/tests/test/index.html
```

#### 错误4: OutOfMemoryError

**错误信息**:
```
Java heap space
```

**解决**:
编辑 `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
```

### 清理构建

```bash
# 完整清理
./gradlew clean

# 删除Gradle缓存
rm -rf ~/.gradle/caches/

# 删除本地构建缓存
rm -rf .gradle/
```

---

**文档版本**: 1.0.0  
**最后更新**: 2025-10-20  
**维护者**: Resources Processor Team

