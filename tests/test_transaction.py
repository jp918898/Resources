"""
Tests for TransactionManager.
"""

import unittest
import os
import tempfile
from resources_processor.transaction import TransactionManager


class TestTransactionManager(unittest.TestCase):
    """Test cases for TransactionManager."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.test_file = os.path.join(self.temp_dir, 'test.txt')
        
        # Create test file
        with open(self.test_file, 'w') as f:
            f.write('original content')
    
    def tearDown(self):
        """Clean up test fixtures."""
        # Clean up temp files
        if os.path.exists(self.test_file):
            os.remove(self.test_file)
        if os.path.exists(self.temp_dir):
            try:
                os.rmdir(self.temp_dir)
            except:
                pass
    
    def test_backup_file(self):
        """Test file backup."""
        manager = TransactionManager()
        
        self.assertTrue(manager.backup_file(self.test_file))
        self.assertTrue(manager.has_backup(self.test_file))
        
        backup_path = manager.get_backup_path(self.test_file)
        self.assertIsNotNone(backup_path)
        self.assertTrue(os.path.exists(backup_path))
    
    def test_rollback(self):
        """Test transaction rollback."""
        manager = TransactionManager()
        
        # Backup original file
        manager.backup_file(self.test_file)
        
        # Modify file
        with open(self.test_file, 'w') as f:
            f.write('modified content')
        
        # Rollback
        self.assertTrue(manager.rollback())
        
        # Verify original content restored
        with open(self.test_file, 'r') as f:
            content = f.read()
        self.assertEqual(content, 'original content')
    
    def test_commit(self):
        """Test transaction commit."""
        manager = TransactionManager()
        
        # Backup and modify
        manager.backup_file(self.test_file)
        
        with open(self.test_file, 'w') as f:
            f.write('modified content')
        
        # Commit
        self.assertTrue(manager.commit())
        
        # Verify modified content preserved
        with open(self.test_file, 'r') as f:
            content = f.read()
        self.assertEqual(content, 'modified content')
    
    def test_context_manager_success(self):
        """Test context manager with successful operation."""
        with TransactionManager() as manager:
            manager.backup_file(self.test_file)
            
            with open(self.test_file, 'w') as f:
                f.write('modified content')
        
        # Should preserve changes on success
        with open(self.test_file, 'r') as f:
            content = f.read()
        self.assertEqual(content, 'modified content')
    
    def test_context_manager_failure(self):
        """Test context manager with exception."""
        try:
            with TransactionManager() as manager:
                manager.backup_file(self.test_file)
                
                with open(self.test_file, 'w') as f:
                    f.write('modified content')
                
                raise Exception("Test exception")
        except Exception:
            pass
        
        # Should rollback on exception
        with open(self.test_file, 'r') as f:
            content = f.read()
        self.assertEqual(content, 'original content')


if __name__ == '__main__':
    unittest.main()
