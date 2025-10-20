#!/usr/bin/env python
"""
Batch processing example for Resources Processor.

This example demonstrates how to:
1. Process multiple APK files
2. Generate randomized class names
3. Use transaction management
4. Handle errors gracefully
"""

import random
import string
from resources_processor import ResourceProcessor, TransactionManager
from pathlib import Path


def generate_random_class_name():
    """Generate a random class name."""
    return ''.join(random.choices(string.ascii_uppercase, k=8))


def generate_random_package():
    """Generate a random package name."""
    segments = random.randint(2, 4)
    parts = []
    for _ in range(segments):
        length = random.randint(4, 8)
        part = ''.join(random.choices(string.ascii_lowercase, k=length))
        parts.append(part)
    return '.'.join(parts)


def process_single_apk(apk_path, output_path, own_packages):
    """
    Process a single APK file.
    
    Args:
        apk_path: Path to input APK
        output_path: Path to output APK
        own_packages: List of own package names
    """
    print(f"\nProcessing: {apk_path}")
    print(f"Output: {output_path}")
    
    # Create processor
    processor = ResourceProcessor(own_packages)
    
    # Use transaction manager for safety
    with TransactionManager() as tm:
        try:
            # In real usage, extract and process APK contents
            # This is a simplified example
            
            # Get all class references
            classes = processor.get_all_class_references()
            print(f"Found {len(classes)} class references")
            
            # Generate mapping
            class_mapping = {}
            for old_class in classes:
                # Extract package
                parts = old_class.split('.')
                if len(parts) >= 2:
                    package = '.'.join(parts[:-1])
                    new_name = generate_random_class_name()
                    new_class = f"{package}.{new_name}"
                    class_mapping[old_class] = new_class
            
            print(f"Generated {len(class_mapping)} class mappings")
            
            # Apply mappings
            for old_class, new_class in class_mapping.items():
                processor.replace_class_name(old_class, new_class)
                print(f"  {old_class} -> {new_class}")
            
            # Verify integrity
            is_valid, errors = processor.verify_integrity()
            if not is_valid:
                print("Integrity check failed:")
                for error in errors:
                    print(f"  - {error}")
                raise Exception("Integrity check failed")
            
            print("✓ Processing completed successfully")
            
        except Exception as e:
            print(f"✗ Error processing APK: {e}")
            print("Rolling back changes...")
            # Transaction manager will auto-rollback
            raise


def batch_process_apks(apk_list, output_dir, own_packages):
    """
    Process multiple APK files in batch.
    
    Args:
        apk_list: List of APK file paths
        output_dir: Output directory
        own_packages: List of own package names
    """
    print("="*60)
    print("Batch APK Processing")
    print("="*60)
    
    success_count = 0
    fail_count = 0
    
    for apk_path in apk_list:
        apk_name = Path(apk_path).name
        output_path = Path(output_dir) / f"processed_{apk_name}"
        
        try:
            process_single_apk(apk_path, output_path, own_packages)
            success_count += 1
        except Exception as e:
            print(f"Failed to process {apk_path}: {e}")
            fail_count += 1
    
    print("\n" + "="*60)
    print("Batch Processing Summary")
    print("="*60)
    print(f"Successful: {success_count}")
    print(f"Failed: {fail_count}")
    print(f"Total: {len(apk_list)}")


def main():
    # Example configuration
    apk_list = [
        # 'path/to/app1.apk',
        # 'path/to/app2.apk',
        # 'path/to/app3.apk',
    ]
    
    output_dir = 'output'
    own_packages = ['com.example', 'org.myapp']
    
    if not apk_list:
        print("No APK files to process.")
        print("Add APK paths to apk_list in the script.")
        return
    
    # Process APKs
    batch_process_apks(apk_list, output_dir, own_packages)


if __name__ == '__main__':
    main()
