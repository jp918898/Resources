"""
DataBindingProcessor - Handle Data Binding expressions in XML files.

Processes variable types, import types, and T(FQCN) expressions in Data Binding layouts.
"""

import re
from typing import Dict, List, Set, Tuple, Optional


class DataBindingProcessor:
    """
    Processor for Data Binding expressions in Android layout files.
    
    Handles:
    - Variable type attributes (e.g., <variable type="com.example.User"/>)
    - Import type attributes (e.g., <import type="com.example.Utils"/>)
    - T(FQCN) expressions in binding expressions (e.g., T(com.example.Utils).method())
    """
    
    # Regex patterns for Data Binding
    VARIABLE_TYPE_PATTERN = re.compile(r'<variable\s+[^>]*type\s*=\s*["\']([^"\']+)["\']', re.IGNORECASE)
    IMPORT_TYPE_PATTERN = re.compile(r'<import\s+[^>]*type\s*=\s*["\']([^"\']+)["\']', re.IGNORECASE)
    T_EXPRESSION_PATTERN = re.compile(r'T\(([a-zA-Z0-9_.]+)\)')
    
    def __init__(self, xml_content: str):
        """
        Initialize DataBindingProcessor with XML content.
        
        Args:
            xml_content: XML content as string (for text-based processing)
        """
        self.original_content = xml_content
        self.content = xml_content
        self.variable_types: List[str] = []
        self.import_types: List[str] = []
        self.t_expressions: List[str] = []
    
    def parse(self) -> bool:
        """
        Parse Data Binding expressions in the XML content.
        
        Returns:
            True if parsing succeeded, False otherwise
        """
        try:
            # Find all variable types
            self.variable_types = self.VARIABLE_TYPE_PATTERN.findall(self.content)
            
            # Find all import types
            self.import_types = self.IMPORT_TYPE_PATTERN.findall(self.content)
            
            # Find all T() expressions
            self.t_expressions = self.T_EXPRESSION_PATTERN.findall(self.content)
            
            return True
        except Exception as e:
            print(f"Error parsing Data Binding content: {e}")
            return False
    
    def replace_variable_type(self, old_type: str, new_type: str) -> bool:
        """
        Replace a variable type in Data Binding expressions.
        
        Args:
            old_type: Old fully qualified class name
            new_type: New fully qualified class name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        try:
            pattern = re.compile(
                r'(<variable\s+[^>]*type\s*=\s*["\'])' + re.escape(old_type) + r'(["\'])',
                re.IGNORECASE
            )
            new_content = pattern.sub(r'\1' + new_type + r'\2', self.content)
            
            if new_content != self.content:
                self.content = new_content
                return True
            return False
        except Exception as e:
            print(f"Error replacing variable type: {e}")
            return False
    
    def replace_import_type(self, old_type: str, new_type: str) -> bool:
        """
        Replace an import type in Data Binding expressions.
        
        Args:
            old_type: Old fully qualified class name
            new_type: New fully qualified class name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        try:
            pattern = re.compile(
                r'(<import\s+[^>]*type\s*=\s*["\'])' + re.escape(old_type) + r'(["\'])',
                re.IGNORECASE
            )
            new_content = pattern.sub(r'\1' + new_type + r'\2', self.content)
            
            if new_content != self.content:
                self.content = new_content
                return True
            return False
        except Exception as e:
            print(f"Error replacing import type: {e}")
            return False
    
    def replace_t_expression(self, old_class: str, new_class: str) -> bool:
        """
        Replace a class name in T() expressions.
        
        Args:
            old_class: Old fully qualified class name
            new_class: New fully qualified class name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        try:
            pattern = re.compile(r'T\(' + re.escape(old_class) + r'\)')
            new_content = pattern.sub(f'T({new_class})', self.content)
            
            if new_content != self.content:
                self.content = new_content
                return True
            return False
        except Exception as e:
            print(f"Error replacing T() expression: {e}")
            return False
    
    def replace_class_name(self, old_class: str, new_class: str) -> bool:
        """
        Replace a class name in all Data Binding contexts.
        
        Args:
            old_class: Old fully qualified class name
            new_class: New fully qualified class name
            
        Returns:
            True if any replacement succeeded, False otherwise
        """
        replaced = False
        replaced |= self.replace_variable_type(old_class, new_class)
        replaced |= self.replace_import_type(old_class, new_class)
        replaced |= self.replace_t_expression(old_class, new_class)
        return replaced
    
    def replace_package_name(self, old_package: str, new_package: str) -> bool:
        """
        Replace a package name in all Data Binding contexts.
        
        Args:
            old_package: Old package name
            new_package: New package name
            
        Returns:
            True if any replacement succeeded, False otherwise
        """
        try:
            replaced = False
            
            # Replace in variable types
            for old_type in self.variable_types:
                if old_type.startswith(old_package + '.') or old_type == old_package:
                    new_type = new_package + old_type[len(old_package):]
                    if self.replace_variable_type(old_type, new_type):
                        replaced = True
            
            # Replace in import types
            for old_type in self.import_types:
                if old_type.startswith(old_package + '.') or old_type == old_package:
                    new_type = new_package + old_type[len(old_package):]
                    if self.replace_import_type(old_type, new_type):
                        replaced = True
            
            # Replace in T() expressions
            for old_expr in self.t_expressions:
                if old_expr.startswith(old_package + '.') or old_expr == old_package:
                    new_expr = new_package + old_expr[len(old_package):]
                    if self.replace_t_expression(old_expr, new_expr):
                        replaced = True
            
            return replaced
        except Exception as e:
            print(f"Error replacing package name: {e}")
            return False
    
    def get_modified_content(self) -> str:
        """
        Get the modified XML content.
        
        Returns:
            Modified XML content as string
        """
        return self.content
    
    def get_all_class_references(self) -> Set[str]:
        """
        Get all class references from Data Binding expressions.
        
        Returns:
            Set of fully qualified class names
        """
        classes = set()
        classes.update(self.variable_types)
        classes.update(self.import_types)
        classes.update(self.t_expressions)
        return classes
