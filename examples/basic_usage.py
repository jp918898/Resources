#!/usr/bin/env python
"""
Basic usage example for Resources Processor.

This example demonstrates how to:
1. Load resources files
2. Replace package names
3. Replace class names
4. Verify integrity
"""

from resources_processor import ResourceProcessor
import sys


def main():
    # Create processor with own packages
    print("Creating ResourceProcessor...")
    processor = ResourceProcessor(own_packages=['com.example.app'])
    
    # Example: Load files (in real usage, read from actual files)
    print("\nLoading files...")
    
    # Normally you would do:
    # with open('resources.arsc', 'rb') as f:
    #     processor.add_arsc_file('resources.arsc', f.read())
    
    print("  - resources.arsc (skipped - no file)")
    print("  - AndroidManifest.xml (skipped - no file)")
    print("  - classes.dex (skipped - no file)")
    
    # Replace package name
    print("\n=== Package Name Replacement ===")
    old_package = "com.example.app"
    new_package = "com.newapp.xyz"
    print(f"Replacing: {old_package} -> {new_package}")
    
    # In real usage:
    # success = processor.replace_package_name(old_package, new_package)
    # if success:
    #     print("✓ Package name replaced successfully")
    # else:
    #     print("✗ Failed to replace package name")
    
    # Replace class name
    print("\n=== Class Name Replacement ===")
    old_class = "com.example.app.MainActivity"
    new_class = "com.newapp.xyz.MainActivity"
    print(f"Replacing: {old_class} -> {new_class}")
    
    # In real usage:
    # success = processor.replace_class_name(old_class, new_class)
    # if success:
    #     print("✓ Class name replaced successfully")
    # else:
    #     print("✗ Failed to replace class name")
    
    # Verify integrity
    print("\n=== Integrity Verification ===")
    # is_valid, errors = processor.verify_integrity()
    # if is_valid:
    #     print("✓ All integrity checks passed")
    # else:
    #     print("✗ Integrity check failed:")
    #     for error in errors:
    #         print(f"  - {error}")
    
    # Get modified data
    print("\n=== Saving Modified Files ===")
    # modified_arsc = processor.get_modified_data('resources.arsc')
    # if modified_arsc:
    #     with open('resources_modified.arsc', 'wb') as f:
    #         f.write(modified_arsc)
    #     print("✓ Saved resources_modified.arsc")
    
    print("\n" + "="*50)
    print("Example completed successfully!")
    print("="*50)


if __name__ == '__main__':
    main()
