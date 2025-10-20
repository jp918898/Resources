"""
IntegrityVerifier - Verify integrity of processed resources.

Uses aapt2 for static verification and performs structural integrity checks.
"""

import subprocess
import os
from typing import List, Dict, Optional, Tuple


class IntegrityVerifier:
    """
    Verifier for resource integrity.
    
    Features:
    - aapt2 static verification
    - Structural integrity checks
    - Format validation
    """
    
    def __init__(self, aapt2_path: Optional[str] = None):
        """
        Initialize IntegrityVerifier.
        
        Args:
            aapt2_path: Path to aapt2 binary (if None, will search in PATH)
        """
        self.aapt2_path = aapt2_path or self._find_aapt2()
    
    def _find_aapt2(self) -> Optional[str]:
        """
        Find aapt2 in system PATH or Android SDK.
        
        Returns:
            Path to aapt2 or None if not found
        """
        # Try to find in PATH
        try:
            result = subprocess.run(['which', 'aapt2'], 
                                    capture_output=True, 
                                    text=True, 
                                    timeout=5)
            if result.returncode == 0:
                return result.stdout.strip()
        except Exception:
            pass
        
        # Try common Android SDK locations
        sdk_paths = [
            os.path.expanduser('~/Android/Sdk/build-tools'),
            '/usr/local/android-sdk/build-tools',
            os.environ.get('ANDROID_HOME', '') + '/build-tools',
        ]
        
        for sdk_path in sdk_paths:
            if os.path.exists(sdk_path):
                # Find latest build-tools version
                try:
                    versions = [d for d in os.listdir(sdk_path) 
                               if os.path.isdir(os.path.join(sdk_path, d))]
                    if versions:
                        latest = sorted(versions)[-1]
                        aapt2 = os.path.join(sdk_path, latest, 'aapt2')
                        if os.path.exists(aapt2):
                            return aapt2
                except Exception:
                    pass
        
        return None
    
    def verify_resources_arsc(self, file_path: str) -> Tuple[bool, List[str]]:
        """
        Verify a resources.arsc file.
        
        Args:
            file_path: Path to resources.arsc file
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []
        
        # Check file exists
        if not os.path.exists(file_path):
            errors.append(f"File not found: {file_path}")
            return False, errors
        
        # Check file is not empty
        if os.path.getsize(file_path) == 0:
            errors.append("File is empty")
            return False, errors
        
        # Verify magic number
        try:
            with open(file_path, 'rb') as f:
                magic = f.read(2)
                # RES_TABLE_TYPE = 0x0001
                if magic != b'\x01\x00':
                    errors.append(f"Invalid magic number: {magic.hex()}")
                    return False, errors
        except Exception as e:
            errors.append(f"Error reading file: {e}")
            return False, errors
        
        # Additional structural checks could be added here
        
        return True, errors
    
    def verify_binary_xml(self, file_path: str) -> Tuple[bool, List[str]]:
        """
        Verify a binary XML file.
        
        Args:
            file_path: Path to binary XML file
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []
        
        # Check file exists
        if not os.path.exists(file_path):
            errors.append(f"File not found: {file_path}")
            return False, errors
        
        # Check file is not empty
        if os.path.getsize(file_path) == 0:
            errors.append("File is empty")
            return False, errors
        
        # Verify magic number
        try:
            with open(file_path, 'rb') as f:
                magic = f.read(2)
                # RES_XML_TYPE = 0x0003
                if magic != b'\x03\x00':
                    errors.append(f"Invalid magic number: {magic.hex()}")
                    return False, errors
        except Exception as e:
            errors.append(f"Error reading file: {e}")
            return False, errors
        
        return True, errors
    
    def verify_with_aapt2(self, apk_path: str) -> Tuple[bool, List[str]]:
        """
        Verify an APK using aapt2.
        
        Args:
            apk_path: Path to APK file
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []
        
        if not self.aapt2_path:
            errors.append("aapt2 not found")
            return False, errors
        
        if not os.path.exists(apk_path):
            errors.append(f"APK not found: {apk_path}")
            return False, errors
        
        try:
            # Run aapt2 dump
            result = subprocess.run(
                [self.aapt2_path, 'dump', 'badging', apk_path],
                capture_output=True,
                text=True,
                timeout=30
            )
            
            if result.returncode != 0:
                errors.append(f"aapt2 verification failed: {result.stderr}")
                return False, errors
            
            return True, errors
        except subprocess.TimeoutExpired:
            errors.append("aapt2 verification timed out")
            return False, errors
        except Exception as e:
            errors.append(f"Error running aapt2: {e}")
            return False, errors
    
    def verify_structure(self, file_path: str, file_type: str) -> Tuple[bool, List[str]]:
        """
        Verify structural integrity based on file type.
        
        Args:
            file_path: Path to file
            file_type: Type of file (arsc, xml, apk)
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        if file_type == 'arsc':
            return self.verify_resources_arsc(file_path)
        elif file_type == 'xml':
            return self.verify_binary_xml(file_path)
        elif file_type == 'apk':
            return self.verify_with_aapt2(file_path)
        else:
            return False, [f"Unknown file type: {file_type}"]
    
    def verify_multiple(self, files: Dict[str, str]) -> Dict[str, Tuple[bool, List[str]]]:
        """
        Verify multiple files.
        
        Args:
            files: Dictionary mapping file paths to file types
            
        Returns:
            Dictionary mapping file paths to (is_valid, errors) tuples
        """
        results = {}
        for file_path, file_type in files.items():
            results[file_path] = self.verify_structure(file_path, file_type)
        return results
    
    def has_aapt2(self) -> bool:
        """
        Check if aapt2 is available.
        
        Returns:
            True if aapt2 is available, False otherwise
        """
        return self.aapt2_path is not None and os.path.exists(self.aapt2_path)
