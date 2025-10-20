# Examples

This directory contains example scripts demonstrating various features of the Resources Processor.

## Files

### 1. `basic_usage.py`

Demonstrates basic usage of the Resources Processor:
- Loading resource files
- Replacing package names
- Replacing class names
- Verifying integrity

```bash
python examples/basic_usage.py
```

### 2. `batch_processing.py`

Shows how to process multiple APK files in batch:
- Generating random class names
- Using transaction management
- Error handling and rollback
- Processing multiple files

```bash
python examples/batch_processing.py
```

### 3. `validation_example.py`

Demonstrates validation features:
- Semantic validation (distinguishing code from UI text)
- Whitelist filtering (own vs system/library packages)
- Integrity verification

```bash
python examples/validation_example.py
```

## Running Examples

Make sure the package is installed first:

```bash
cd /home/runner/work/Resources/Resources
pip install -e .
```

Then run any example:

```bash
python examples/validation_example.py
```

## Customization

Feel free to modify these examples for your specific use case. The examples are designed to be simple and easy to understand, serving as a starting point for your own implementations.
