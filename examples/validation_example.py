#!/usr/bin/env python
"""
Validation example for Resources Processor.

This example demonstrates:
1. Semantic validation
2. Whitelist filtering
3. DEX cross-validation
4. Integrity checking
"""

from resources_processor import (
    SemanticValidator,
    WhitelistFilter,
    DexValidator,
    IntegrityVerifier
)


def demonstrate_semantic_validation():
    """Demonstrate semantic validation features."""
    print("\n" + "="*60)
    print("Semantic Validation")
    print("="*60)
    
    validator = SemanticValidator()
    
    # Test package names
    print("\n--- Package Name Validation ---")
    test_packages = [
        'com.example.app',
        'org.test.project',
        'invalid',
        'Com.Example',
        'com example',
    ]
    
    for package in test_packages:
        is_valid = validator.is_valid_package_name(package)
        status = "✓" if is_valid else "✗"
        print(f"{status} {package:30s} {'VALID' if is_valid else 'INVALID'}")
    
    # Test class names
    print("\n--- Class Name Validation ---")
    test_classes = [
        'com.example.MainActivity',
        'org.test.MyClass',
        'MainActivity',
        'com.example.mainactivity',
        'Hello World',
    ]
    
    for class_name in test_classes:
        is_valid = validator.is_valid_class_name(class_name)
        status = "✓" if is_valid else "✗"
        print(f"{status} {class_name:35s} {'VALID' if is_valid else 'INVALID'}")
    
    # Test UI text detection
    print("\n--- UI Text Detection ---")
    test_strings = [
        'com.example.MainActivity',
        'Hello World',
        'Click here!',
        'https://example.com',
        '123456',
    ]
    
    for string in test_strings:
        is_ui = validator.is_ui_text(string)
        status = "UI" if is_ui else "CODE"
        print(f"[{status:4s}] {string}")


def demonstrate_whitelist_filtering():
    """Demonstrate whitelist filtering features."""
    print("\n" + "="*60)
    print("Whitelist Filtering")
    print("="*60)
    
    # Create filter with own packages
    whitelist = WhitelistFilter(own_packages=['com.myapp', 'org.myproject'])
    
    print("\n--- Package Processing Decisions ---")
    test_packages = [
        'com.myapp',
        'com.myapp.activity',
        'org.myproject.utils',
        'android.app',
        'androidx.core',
        'com.google.android.material',
        'com.other.app',
    ]
    
    for package in test_packages:
        should_process = whitelist.should_process(package)
        status = "✓ PROCESS" if should_process else "✗ SKIP"
        
        reason = ""
        if whitelist.is_system_package(package):
            reason = "(system)"
        elif whitelist.is_library_package(package):
            reason = "(library)"
        elif not whitelist.is_own_package(package):
            reason = "(not own)"
        
        print(f"{status:12s} {package:35s} {reason}")
    
    # Test class filtering
    print("\n--- Class Name Filtering ---")
    test_classes = {
        'com.myapp.MainActivity',
        'android.app.Activity',
        'androidx.core.app.ActivityCompat',
        'com.other.SomeClass',
    }
    
    filtered = whitelist.filter_class_names(test_classes)
    
    print(f"Original classes: {len(test_classes)}")
    print(f"Filtered classes: {len(filtered)}")
    print("\nFiltered classes:")
    for cls in sorted(filtered):
        print(f"  ✓ {cls}")


def demonstrate_integrity_verification():
    """Demonstrate integrity verification features."""
    print("\n" + "="*60)
    print("Integrity Verification")
    print("="*60)
    
    verifier = IntegrityVerifier()
    
    print(f"\naapt2 available: {verifier.has_aapt2()}")
    
    if verifier.has_aapt2():
        print(f"aapt2 path: {verifier.aapt2_path}")
    else:
        print("Note: aapt2 not found. Install Android SDK for full verification.")
    
    # Test file format verification (with mock data)
    print("\n--- File Format Verification ---")
    print("To verify actual files, use:")
    print("  verifier.verify_resources_arsc('path/to/resources.arsc')")
    print("  verifier.verify_binary_xml('path/to/AndroidManifest.xml')")
    print("  verifier.verify_with_aapt2('path/to/app.apk')")


def main():
    """Run all validation examples."""
    print("\n" + "="*60)
    print("Resources Processor - Validation Examples")
    print("="*60)
    
    demonstrate_semantic_validation()
    demonstrate_whitelist_filtering()
    demonstrate_integrity_verification()
    
    print("\n" + "="*60)
    print("Examples completed!")
    print("="*60 + "\n")


if __name__ == '__main__':
    main()
