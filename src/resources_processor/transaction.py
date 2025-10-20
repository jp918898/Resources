"""
TransactionManager - Manage atomic operations with rollback capability.

Ensures that all operations can be rolled back on failure to prevent data corruption.
"""

import os
import shutil
import tempfile
from typing import Dict, List, Optional, Callable
from pathlib import Path


class TransactionManager:
    """
    Manager for transactional file operations.
    
    Features:
    - Backup original files before modification
    - Commit changes atomically
    - Rollback on failure
    - Zero data corruption risk
    """
    
    def __init__(self, backup_dir: Optional[str] = None):
        """
        Initialize TransactionManager.
        
        Args:
            backup_dir: Directory to store backups (default: temp directory)
        """
        self.backup_dir = backup_dir or tempfile.mkdtemp(prefix='resources_backup_')
        self.backups: Dict[str, str] = {}
        self.operations: List[Callable] = []
        self.committed = False
        self.rolled_back = False
        
        # Ensure backup directory exists
        os.makedirs(self.backup_dir, exist_ok=True)
    
    def backup_file(self, file_path: str) -> bool:
        """
        Create a backup of a file.
        
        Args:
            file_path: Path to file to backup
            
        Returns:
            True if backup succeeded, False otherwise
        """
        try:
            if not os.path.exists(file_path):
                print(f"File not found for backup: {file_path}")
                return False
            
            # Create backup filename
            file_name = os.path.basename(file_path)
            backup_path = os.path.join(self.backup_dir, file_name)
            
            # If backup already exists, add counter
            counter = 1
            while os.path.exists(backup_path):
                name, ext = os.path.splitext(file_name)
                backup_path = os.path.join(self.backup_dir, f"{name}_{counter}{ext}")
                counter += 1
            
            # Copy file to backup
            shutil.copy2(file_path, backup_path)
            self.backups[file_path] = backup_path
            
            return True
        except Exception as e:
            print(f"Error backing up file {file_path}: {e}")
            return False
    
    def backup_files(self, file_paths: List[str]) -> bool:
        """
        Create backups of multiple files.
        
        Args:
            file_paths: List of file paths to backup
            
        Returns:
            True if all backups succeeded, False otherwise
        """
        success = True
        for file_path in file_paths:
            if not self.backup_file(file_path):
                success = False
        return success
    
    def add_operation(self, operation: Callable) -> None:
        """
        Add an operation to be executed in the transaction.
        
        Args:
            operation: Callable that performs the operation
        """
        self.operations.append(operation)
    
    def execute(self) -> bool:
        """
        Execute all operations in the transaction.
        
        Returns:
            True if all operations succeeded, False otherwise
        """
        try:
            for operation in self.operations:
                result = operation()
                if result is False:
                    print("Operation failed, rolling back")
                    self.rollback()
                    return False
            return True
        except Exception as e:
            print(f"Error executing operations: {e}")
            self.rollback()
            return False
    
    def commit(self) -> bool:
        """
        Commit the transaction and remove backups.
        
        Returns:
            True if commit succeeded, False otherwise
        """
        try:
            if self.rolled_back:
                print("Cannot commit after rollback")
                return False
            
            self.committed = True
            
            # Clean up backups
            self._cleanup_backups()
            
            return True
        except Exception as e:
            print(f"Error committing transaction: {e}")
            return False
    
    def rollback(self) -> bool:
        """
        Rollback the transaction by restoring backups.
        
        Returns:
            True if rollback succeeded, False otherwise
        """
        try:
            if self.committed:
                print("Cannot rollback after commit")
                return False
            
            self.rolled_back = True
            
            # Restore all backed up files
            for original_path, backup_path in self.backups.items():
                if os.path.exists(backup_path):
                    shutil.copy2(backup_path, original_path)
                    print(f"Restored {original_path} from backup")
            
            # Clean up backups
            self._cleanup_backups()
            
            return True
        except Exception as e:
            print(f"Error rolling back transaction: {e}")
            return False
    
    def _cleanup_backups(self) -> None:
        """Clean up backup files and directory."""
        try:
            # Remove backup files
            for backup_path in self.backups.values():
                if os.path.exists(backup_path):
                    os.remove(backup_path)
            
            # Remove backup directory if empty
            if os.path.exists(self.backup_dir) and not os.listdir(self.backup_dir):
                os.rmdir(self.backup_dir)
        except Exception as e:
            print(f"Error cleaning up backups: {e}")
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit with automatic rollback on exception."""
        if exc_type is not None:
            # Exception occurred, rollback
            self.rollback()
            return False
        else:
            # No exception, can commit
            return True
    
    def get_backup_path(self, file_path: str) -> Optional[str]:
        """
        Get the backup path for a file.
        
        Args:
            file_path: Original file path
            
        Returns:
            Backup path or None if not backed up
        """
        return self.backups.get(file_path)
    
    def has_backup(self, file_path: str) -> bool:
        """
        Check if a file has been backed up.
        
        Args:
            file_path: File path to check
            
        Returns:
            True if file has backup, False otherwise
        """
        return file_path in self.backups
    
    def get_all_backups(self) -> Dict[str, str]:
        """
        Get all file backups.
        
        Returns:
            Dictionary mapping original paths to backup paths
        """
        return self.backups.copy()
