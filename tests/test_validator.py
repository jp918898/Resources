"""
Tests for SemanticValidator.
"""

import unittest
from resources_processor.validator import SemanticValidator


class TestSemanticValidator(unittest.TestCase):
    """Test cases for SemanticValidator."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.validator = SemanticValidator()
    
    def test_valid_package_name(self):
        """Test valid package name detection."""
        self.assertTrue(self.validator.is_valid_package_name('com.example.app'))
        self.assertTrue(self.validator.is_valid_package_name('org.test.package'))
        self.assertTrue(self.validator.is_valid_package_name('com.example.very.long.package'))
    
    def test_invalid_package_name(self):
        """Test invalid package name detection."""
        self.assertFalse(self.validator.is_valid_package_name(''))
        self.assertFalse(self.validator.is_valid_package_name('com'))
        self.assertFalse(self.validator.is_valid_package_name('Com.Example'))
        self.assertFalse(self.validator.is_valid_package_name('com.example app'))
        self.assertFalse(self.validator.is_valid_package_name('123.example'))
    
    def test_valid_class_name(self):
        """Test valid class name detection."""
        self.assertTrue(self.validator.is_valid_class_name('com.example.MainActivity'))
        self.assertTrue(self.validator.is_valid_class_name('org.test.MyClass'))
        self.assertTrue(self.validator.is_valid_class_name('com.example.app.MyActivity'))
    
    def test_invalid_class_name(self):
        """Test invalid class name detection."""
        self.assertFalse(self.validator.is_valid_class_name(''))
        self.assertFalse(self.validator.is_valid_class_name('MainActivity'))
        self.assertFalse(self.validator.is_valid_class_name('com.example'))
        self.assertFalse(self.validator.is_valid_class_name('com.example.mainactivity'))
        self.assertFalse(self.validator.is_valid_class_name('Com.Example.MainActivity'))
    
    def test_ui_text_detection(self):
        """Test UI text detection."""
        self.assertTrue(self.validator.is_ui_text('Hello World'))
        self.assertTrue(self.validator.is_ui_text('Click here!'))
        self.assertTrue(self.validator.is_ui_text('https://example.com'))
        self.assertFalse(self.validator.is_ui_text('com.example.MainActivity'))
    
    def test_validate_replacement(self):
        """Test replacement validation."""
        # Valid replacements
        self.assertTrue(
            self.validator.validate_replacement(
                'com.example.MainActivity',
                'com.newpkg.MainActivity'
            )
        )
        self.assertTrue(
            self.validator.validate_replacement(
                'com.example.app',
                'com.newpkg.app'
            )
        )
        
        # Invalid replacements
        self.assertFalse(
            self.validator.validate_replacement(
                'com.example.MainActivity',
                'Hello World'
            )
        )
    
    def test_filter_class_names(self):
        """Test class name filtering."""
        candidates = {
            'com.example.MainActivity',
            'Hello World',
            'org.test.MyClass',
            'invalid',
        }
        
        filtered = self.validator.filter_class_names(candidates)
        
        self.assertIn('com.example.MainActivity', filtered)
        self.assertIn('org.test.MyClass', filtered)
        self.assertNotIn('Hello World', filtered)
        self.assertNotIn('invalid', filtered)
    
    def test_extract_package_from_class(self):
        """Test package extraction from class name."""
        self.assertEqual(
            self.validator.extract_package_from_class('com.example.MainActivity'),
            'com.example'
        )
        self.assertEqual(
            self.validator.extract_package_from_class('org.test.app.MyClass'),
            'org.test.app'
        )
        self.assertIsNone(
            self.validator.extract_package_from_class('InvalidClass')
        )


if __name__ == '__main__':
    unittest.main()
