"""
CLI - Command-line interface for resources processor.

Provides command-line tools for processing Android resources.
"""

import argparse
import sys
import json
from pathlib import Path
from typing import Dict, List, Optional

from .processor import ResourceProcessor


def main():
    """Main entry point for CLI."""
    parser = argparse.ArgumentParser(
        description='Industrial-grade resources.arsc and binary XML processing tool'
    )
    
    subparsers = parser.add_subparsers(dest='command', help='Command to execute')
    
    # Replace package command
    replace_pkg_parser = subparsers.add_parser('replace-package', 
                                                help='Replace package name')
    replace_pkg_parser.add_argument('--old', required=True, help='Old package name')
    replace_pkg_parser.add_argument('--new', required=True, help='New package name')
    replace_pkg_parser.add_argument('--arsc', action='append', help='Path to resources.arsc file')
    replace_pkg_parser.add_argument('--xml', action='append', help='Path to binary XML file')
    replace_pkg_parser.add_argument('--own-packages', action='append', 
                                    help='Own package names to process')
    
    # Replace class command
    replace_class_parser = subparsers.add_parser('replace-class', 
                                                  help='Replace class name')
    replace_class_parser.add_argument('--old', required=True, help='Old class name')
    replace_class_parser.add_argument('--new', required=True, help='New class name')
    replace_class_parser.add_argument('--arsc', action='append', help='Path to resources.arsc file')
    replace_class_parser.add_argument('--xml', action='append', help='Path to binary XML file')
    replace_class_parser.add_argument('--dex', action='append', help='Path to DEX file')
    replace_class_parser.add_argument('--own-packages', action='append', 
                                      help='Own package names to process')
    
    # Validate command
    validate_parser = subparsers.add_parser('validate', 
                                           help='Validate resources')
    validate_parser.add_argument('--arsc', action='append', help='Path to resources.arsc file')
    validate_parser.add_argument('--xml', action='append', help='Path to binary XML file')
    validate_parser.add_argument('--dex', action='append', help='Path to DEX file')
    validate_parser.add_argument('--own-packages', action='append', 
                                help='Own package names to process')
    
    # Analyze command
    analyze_parser = subparsers.add_parser('analyze', 
                                          help='Analyze resources and list classes/packages')
    analyze_parser.add_argument('--arsc', action='append', help='Path to resources.arsc file')
    analyze_parser.add_argument('--xml', action='append', help='Path to binary XML file')
    analyze_parser.add_argument('--dex', action='append', help='Path to DEX file')
    analyze_parser.add_argument('--output', help='Output JSON file for results')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        sys.exit(1)
    
    # Create processor
    own_packages = getattr(args, 'own_packages', None)
    processor = ResourceProcessor(own_packages)
    
    # Load files
    try:
        if hasattr(args, 'arsc') and args.arsc:
            for arsc_path in args.arsc:
                with open(arsc_path, 'rb') as f:
                    data = f.read()
                if not processor.add_arsc_file(arsc_path, data):
                    print(f"Failed to load ARSC file: {arsc_path}")
                    sys.exit(1)
        
        if hasattr(args, 'xml') and args.xml:
            for xml_path in args.xml:
                with open(xml_path, 'rb') as f:
                    data = f.read()
                if not processor.add_xml_file(xml_path, data):
                    print(f"Failed to load XML file: {xml_path}")
                    sys.exit(1)
        
        if hasattr(args, 'dex') and args.dex:
            for dex_path in args.dex:
                with open(dex_path, 'rb') as f:
                    data = f.read()
                if not processor.add_dex_file(dex_path, data):
                    print(f"Failed to load DEX file: {dex_path}")
                    sys.exit(1)
    except Exception as e:
        print(f"Error loading files: {e}")
        sys.exit(1)
    
    # Execute command
    if args.command == 'replace-package':
        if processor.replace_package_name(args.old, args.new):
            print(f"Successfully replaced package: {args.old} -> {args.new}")
            
            # Write modified files
            if args.arsc:
                for arsc_path in args.arsc:
                    modified_data = processor.get_modified_data(arsc_path)
                    if modified_data:
                        with open(arsc_path, 'wb') as f:
                            f.write(modified_data)
                        print(f"Updated: {arsc_path}")
            
            if args.xml:
                for xml_path in args.xml:
                    modified_data = processor.get_modified_data(xml_path)
                    if modified_data:
                        with open(xml_path, 'wb') as f:
                            f.write(modified_data)
                        print(f"Updated: {xml_path}")
        else:
            print(f"Failed to replace package: {args.old} -> {args.new}")
            sys.exit(1)
    
    elif args.command == 'replace-class':
        if processor.replace_class_name(args.old, args.new):
            print(f"Successfully replaced class: {args.old} -> {args.new}")
            
            # Write modified files
            if args.arsc:
                for arsc_path in args.arsc:
                    modified_data = processor.get_modified_data(arsc_path)
                    if modified_data:
                        with open(arsc_path, 'wb') as f:
                            f.write(modified_data)
                        print(f"Updated: {arsc_path}")
            
            if args.xml:
                for xml_path in args.xml:
                    modified_data = processor.get_modified_data(xml_path)
                    if modified_data:
                        with open(xml_path, 'wb') as f:
                            f.write(modified_data)
                        print(f"Updated: {xml_path}")
        else:
            print(f"Failed to replace class: {args.old} -> {args.new}")
            sys.exit(1)
    
    elif args.command == 'validate':
        valid_classes, invalid_classes = processor.validate_class_references()
        
        print(f"\nValidation Results:")
        print(f"Valid classes: {len(valid_classes)}")
        print(f"Invalid classes: {len(invalid_classes)}")
        
        if invalid_classes:
            print("\nInvalid classes (not found in DEX):")
            for cls in sorted(invalid_classes):
                print(f"  - {cls}")
        
        is_valid, errors = processor.verify_integrity()
        if is_valid:
            print("\nIntegrity check: PASSED")
        else:
            print("\nIntegrity check: FAILED")
            for error in errors:
                print(f"  - {error}")
            sys.exit(1)
    
    elif args.command == 'analyze':
        results = {
            'class_references': sorted(processor.get_all_class_references()),
            'dex_classes': sorted(processor.get_all_dex_classes()),
        }
        
        if args.output:
            with open(args.output, 'w') as f:
                json.dump(results, f, indent=2)
            print(f"Analysis results written to: {args.output}")
        else:
            print(json.dumps(results, indent=2))


if __name__ == '__main__':
    main()
