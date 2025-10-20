"""
Tests for DataBindingProcessor.
"""

import unittest
from resources_processor.databinding_processor import DataBindingProcessor


class TestDataBindingProcessor(unittest.TestCase):
    """Test cases for DataBindingProcessor."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.sample_xml = '''<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable
            name="user"
            type="com.example.User" />
        <import type="com.example.Utils" />
        <import type="com.example.Constants" />
    </data>
    <TextView
        android:text="@{T(com.example.Utils).formatName(user.name)}" />
</layout>'''
    
    def test_parse(self):
        """Test parsing Data Binding expressions."""
        processor = DataBindingProcessor(self.sample_xml)
        self.assertTrue(processor.parse())
        
        self.assertIn('com.example.User', processor.variable_types)
        self.assertIn('com.example.Utils', processor.import_types)
        self.assertIn('com.example.Constants', processor.import_types)
        self.assertIn('com.example.Utils', processor.t_expressions)
    
    def test_replace_variable_type(self):
        """Test replacing variable type."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        self.assertTrue(processor.replace_variable_type('com.example.User', 'com.newpkg.User'))
        self.assertIn('com.newpkg.User', processor.get_modified_content())
        self.assertNotIn('type="com.example.User"', processor.get_modified_content())
    
    def test_replace_import_type(self):
        """Test replacing import type."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        self.assertTrue(processor.replace_import_type('com.example.Utils', 'com.newpkg.Utils'))
        self.assertIn('com.newpkg.Utils', processor.get_modified_content())
    
    def test_replace_t_expression(self):
        """Test replacing T() expression."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        self.assertTrue(processor.replace_t_expression('com.example.Utils', 'com.newpkg.Utils'))
        self.assertIn('T(com.newpkg.Utils)', processor.get_modified_content())
        self.assertNotIn('T(com.example.Utils)', processor.get_modified_content())
    
    def test_replace_class_name(self):
        """Test replacing class name in all contexts."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        self.assertTrue(processor.replace_class_name('com.example.Utils', 'com.newpkg.Utils'))
        
        content = processor.get_modified_content()
        self.assertIn('com.newpkg.Utils', content)
        self.assertIn('T(com.newpkg.Utils)', content)
    
    def test_replace_package_name(self):
        """Test replacing package name."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        self.assertTrue(processor.replace_package_name('com.example', 'com.newpkg'))
        
        content = processor.get_modified_content()
        self.assertIn('com.newpkg.User', content)
        self.assertIn('com.newpkg.Utils', content)
        self.assertIn('com.newpkg.Constants', content)
        self.assertNotIn('com.example', content)
    
    def test_get_all_class_references(self):
        """Test getting all class references."""
        processor = DataBindingProcessor(self.sample_xml)
        processor.parse()
        
        classes = processor.get_all_class_references()
        
        self.assertIn('com.example.User', classes)
        self.assertIn('com.example.Utils', classes)
        self.assertIn('com.example.Constants', classes)


if __name__ == '__main__':
    unittest.main()
