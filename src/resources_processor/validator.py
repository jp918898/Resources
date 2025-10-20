"""
SemanticValidator - Validate and distinguish between class names/package names and UI text.

Prevents accidental replacement of ordinary UI text that happens to look like a class name.
"""

import re
from typing import Set, List, Optional


class SemanticValidator:
    """
    Validator for semantic context of strings.
    
    Distinguishes between:
    - Fully qualified class names (FQCN)
    - Package names
    - Ordinary UI text/strings
    
    Prevents mistaken replacement of UI text.
    """
    
    # Pattern for valid Java/Kotlin package names
    PACKAGE_PATTERN = re.compile(r'^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')
    
    # Pattern for valid Java/Kotlin class names
    CLASS_PATTERN = re.compile(r'^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*\.[A-Z][a-zA-Z0-9_]*(\$[A-Z][a-zA-Z0-9_]*)*$')
    
    # Pattern for inner class notation
    INNER_CLASS_PATTERN = re.compile(r'\$[A-Z][a-zA-Z0-9_]*')
    
    # Common UI text patterns that might be confused with class names
    UI_TEXT_INDICATORS = [
        r'\s',  # Contains whitespace
        r'[!@#%^&*()+=\[\]{};:\'",<>?/\\|`~]',  # Contains special characters
        r'^https?://',  # URL
        r'^file://',  # File path
        r'^\d+$',  # Pure numbers
    ]
    
    def __init__(self):
        """Initialize SemanticValidator."""
        self.ui_text_pattern = re.compile('|'.join(self.UI_TEXT_INDICATORS))
    
    def is_valid_package_name(self, name: str) -> bool:
        """
        Check if a string is a valid package name.
        
        Args:
            name: String to validate
            
        Returns:
            True if valid package name, False otherwise
        """
        if not name or len(name) < 3:
            return False
        
        # Check basic pattern
        if not self.PACKAGE_PATTERN.match(name):
            return False
        
        # Check for UI text indicators
        if self.ui_text_pattern.search(name):
            return False
        
        # Package names should have at least 2 segments
        parts = name.split('.')
        if len(parts) < 2:
            return False
        
        # Each part should be reasonable length (not too long for a package segment)
        for part in parts:
            if len(part) > 30:  # Arbitrary but reasonable limit
                return False
        
        return True
    
    def is_valid_class_name(self, name: str) -> bool:
        """
        Check if a string is a valid fully qualified class name.
        
        Args:
            name: String to validate
            
        Returns:
            True if valid class name, False otherwise
        """
        if not name or len(name) < 3:
            return False
        
        # Check basic pattern
        if not self.CLASS_PATTERN.match(name):
            return False
        
        # Check for UI text indicators
        if self.ui_text_pattern.search(name):
            return False
        
        # Split into package and class parts
        parts = name.split('.')
        if len(parts) < 2:
            return False
        
        # Last part should be class name (starts with uppercase)
        class_name = parts[-1]
        if not class_name or not class_name[0].isupper():
            return False
        
        # Package parts should start with lowercase
        for part in parts[:-1]:
            if not part or not part[0].islower():
                return False
        
        return True
    
    def is_ui_text(self, text: str) -> bool:
        """
        Check if a string is likely UI text rather than a class/package name.
        
        Args:
            text: String to check
            
        Returns:
            True if likely UI text, False otherwise
        """
        if not text:
            return False
        
        # Check for UI text indicators
        if self.ui_text_pattern.search(text):
            return True
        
        # Check if it's too long to be a class name
        if len(text) > 200:
            return True
        
        # Check if it contains multiple sentences
        if text.count('.') > 5:
            return True
        
        # If it looks like a class or package name, it's not UI text
        if self.is_valid_class_name(text) or self.is_valid_package_name(text):
            return False
        
        # Check for common UI text patterns
        # Contains multiple words without dots
        if ' ' in text and '.' not in text:
            return True
        
        return False
    
    def validate_replacement(self, original: str, replacement: str) -> bool:
        """
        Validate that a replacement is semantically appropriate.
        
        Args:
            original: Original string
            replacement: Replacement string
            
        Returns:
            True if replacement is valid, False otherwise
        """
        # Both should be same type (class or package)
        orig_is_class = self.is_valid_class_name(original)
        repl_is_class = self.is_valid_class_name(replacement)
        
        orig_is_package = self.is_valid_package_name(original)
        repl_is_package = self.is_valid_package_name(replacement)
        
        # If original is a class, replacement should also be a class
        if orig_is_class and not repl_is_class:
            return False
        
        # If original is a package, replacement should also be a package
        if orig_is_package and not repl_is_package:
            return False
        
        # Neither should be UI text
        if self.is_ui_text(original) or self.is_ui_text(replacement):
            return False
        
        return True
    
    def filter_class_names(self, candidates: Set[str]) -> Set[str]:
        """
        Filter a set of candidates to only include valid class names.
        
        Args:
            candidates: Set of candidate strings
            
        Returns:
            Filtered set containing only valid class names
        """
        return {name for name in candidates if self.is_valid_class_name(name)}
    
    def filter_package_names(self, candidates: Set[str]) -> Set[str]:
        """
        Filter a set of candidates to only include valid package names.
        
        Args:
            candidates: Set of candidate strings
            
        Returns:
            Filtered set containing only valid package names
        """
        return {name for name in candidates if self.is_valid_package_name(name)}
    
    def extract_package_from_class(self, class_name: str) -> Optional[str]:
        """
        Extract package name from a fully qualified class name.
        
        Args:
            class_name: Fully qualified class name
            
        Returns:
            Package name or None if not a valid class name
        """
        if not self.is_valid_class_name(class_name):
            return None
        
        parts = class_name.split('.')
        if len(parts) < 2:
            return None
        
        return '.'.join(parts[:-1])
