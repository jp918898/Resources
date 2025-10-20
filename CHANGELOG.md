# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-10-20

### Added

#### Core Features
- **ArscProcessor**: Parse and modify resources.arsc files
  - Parse binary format string pools
  - Replace package names in global string pool
  - Replace class names in string pool
  - Maintain file structure integrity

- **XmlProcessor**: Process binary XML files
  - Support for layout, menu, navigation, and xml config files
  - Replace class names in attributes
  - Replace package names
  - Handle Android namespaces

- **DataBindingProcessor**: Handle Data Binding expressions
  - Process `<variable type="...">` attributes
  - Process `<import type="...">` attributes
  - Process `T(FQCN)` expressions in binding expressions
  - Package name replacement in all Data Binding contexts

- **SemanticValidator**: Validate and distinguish code from UI text
  - Validate Java/Kotlin package names
  - Validate fully qualified class names
  - Detect UI text patterns
  - Prevent accidental replacement of user-facing text
  - Extract package from class names

- **WhitelistFilter**: Filter packages for processing
  - Built-in system package list (android, java, kotlin, etc.)
  - Built-in common library package list (androidx, google, etc.)
  - Custom own package list
  - Custom exclusion list
  - Filter packages and class names

- **DexValidator**: Cross-validate with DEX files
  - Parse DEX file format
  - Extract all class definitions
  - Validate class name existence
  - Filter classes by DEX presence
  - Convert DEX descriptors to class names

- **TransactionManager**: Atomic operations with rollback
  - Automatic file backup before modification
  - Commit changes on success
  - Rollback on failure
  - Context manager support
  - Zero data corruption risk

- **IntegrityVerifier**: Verify processed resources
  - Verify resources.arsc format
  - Verify binary XML format
  - aapt2 integration for APK verification
  - Structural integrity checks
  - Auto-detect aapt2 in Android SDK

- **ResourceProcessor**: Main orchestrator
  - Coordinate all processing components
  - Manage multiple file types
  - Apply package/class replacements
  - Validate all operations
  - Get modified data

- **CLI**: Command-line interface
  - `replace-package`: Replace package names
  - `replace-class`: Replace class names
  - `validate`: Validate resources and cross-check with DEX
  - `analyze`: Analyze and list all classes/packages

#### Testing
- Comprehensive test suite with 28 tests
- Tests for SemanticValidator
- Tests for WhitelistFilter
- Tests for DataBindingProcessor
- Tests for TransactionManager
- 100% test pass rate

#### Documentation
- Comprehensive README with Chinese documentation
- Feature overview and checklist
- Architecture design documentation
- Installation instructions
- Quick start guide
- Detailed component documentation
- Usage examples (Python API and CLI)
- Use case scenarios
- Security features documentation
- Contributing guidelines
- Example scripts

#### Examples
- `basic_usage.py`: Basic usage demonstration
- `batch_processing.py`: Batch APK processing
- `validation_example.py`: Validation features demonstration

### Technical Details

- Python 3.7+ support
- Pure Python implementation
- Binary format parsing (resources.arsc, binary XML, DEX)
- Regex-based Data Binding expression parsing
- Transaction-based safety mechanism
- Modular architecture for extensibility

### Dependencies
- lxml >= 4.9.0 (for potential XML manipulation)

[1.0.0]: https://github.com/jp918898/Resources/releases/tag/v1.0.0
