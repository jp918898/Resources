"""
Tests for WhitelistFilter.
"""

import unittest
from resources_processor.whitelist import WhitelistFilter


class TestWhitelistFilter(unittest.TestCase):
    """Test cases for WhitelistFilter."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.filter = WhitelistFilter(['com.myapp', 'org.myproject'])
    
    def test_is_own_package(self):
        """Test own package detection."""
        self.assertTrue(self.filter.is_own_package('com.myapp'))
        self.assertTrue(self.filter.is_own_package('com.myapp.activity'))
        self.assertTrue(self.filter.is_own_package('org.myproject.utils'))
        self.assertFalse(self.filter.is_own_package('com.other'))
    
    def test_is_system_package(self):
        """Test system package detection."""
        self.assertTrue(self.filter.is_system_package('android'))
        self.assertTrue(self.filter.is_system_package('android.app'))
        self.assertTrue(self.filter.is_system_package('java.util'))
        self.assertTrue(self.filter.is_system_package('kotlin.collections'))
        self.assertFalse(self.filter.is_system_package('com.example'))
    
    def test_is_library_package(self):
        """Test library package detection."""
        self.assertTrue(self.filter.is_library_package('androidx'))
        self.assertTrue(self.filter.is_library_package('androidx.core'))
        self.assertTrue(self.filter.is_library_package('com.google.android.material'))
        self.assertFalse(self.filter.is_library_package('com.myapp'))
    
    def test_should_process(self):
        """Test processing decision."""
        # Should process own packages
        self.assertTrue(self.filter.should_process('com.myapp'))
        self.assertTrue(self.filter.should_process('org.myproject.test'))
        
        # Should not process system packages
        self.assertFalse(self.filter.should_process('android.app'))
        self.assertFalse(self.filter.should_process('java.util'))
        
        # Should not process library packages
        self.assertFalse(self.filter.should_process('androidx.core'))
        
        # Should not process other packages
        self.assertFalse(self.filter.should_process('com.other'))
    
    def test_filter_packages(self):
        """Test package filtering."""
        packages = {
            'com.myapp',
            'com.myapp.activity',
            'android.app',
            'androidx.core',
            'com.other',
        }
        
        filtered = self.filter.filter_packages(packages)
        
        self.assertIn('com.myapp', filtered)
        self.assertIn('com.myapp.activity', filtered)
        self.assertNotIn('android.app', filtered)
        self.assertNotIn('androidx.core', filtered)
        self.assertNotIn('com.other', filtered)
    
    def test_filter_class_names(self):
        """Test class name filtering."""
        classes = {
            'com.myapp.MainActivity',
            'android.app.Activity',
            'androidx.core.app.ActivityCompat',
            'com.other.SomeClass',
        }
        
        filtered = self.filter.filter_class_names(classes)
        
        self.assertIn('com.myapp.MainActivity', filtered)
        self.assertNotIn('android.app.Activity', filtered)
        self.assertNotIn('androidx.core.app.ActivityCompat', filtered)
        self.assertNotIn('com.other.SomeClass', filtered)
    
    def test_add_own_package(self):
        """Test adding own packages."""
        self.filter.add_own_package('com.newpkg')
        self.assertTrue(self.filter.should_process('com.newpkg'))
        self.assertTrue(self.filter.should_process('com.newpkg.test'))
    
    def test_add_excluded_package(self):
        """Test adding excluded packages."""
        self.filter.add_excluded_package('com.excluded')
        self.assertFalse(self.filter.should_process('com.excluded'))
        self.assertFalse(self.filter.should_process('com.excluded.test'))


if __name__ == '__main__':
    unittest.main()
