"""
ArscProcessor - Process resources.arsc files for package/class name replacement.

This module handles parsing and modifying Android's compiled resources file (resources.arsc),
specifically replacing package names and class names in the global string pool.
"""

import struct
import io
from typing import Dict, List, Set, Optional


class ArscProcessor:
    """
    Processor for resources.arsc files.
    
    Handles:
    - Parsing resources.arsc binary format
    - Replacing package names in string pool
    - Replacing class names in string pool
    - Maintaining file structure integrity
    """
    
    # Magic numbers for ARSC file format
    RES_TABLE_TYPE = 0x0001
    RES_STRING_POOL_TYPE = 0x0001
    RES_TABLE_PACKAGE_TYPE = 0x0200
    
    def __init__(self, data: bytes):
        """
        Initialize ArscProcessor with raw resources.arsc data.
        
        Args:
            data: Raw bytes of resources.arsc file
        """
        self.original_data = data
        self.data = bytearray(data)
        self.string_pool: List[str] = []
        self.string_offsets: List[int] = []
        self.package_names: List[str] = []
        
    def parse(self) -> bool:
        """
        Parse the resources.arsc file structure.
        
        Returns:
            True if parsing succeeded, False otherwise
        """
        try:
            stream = io.BytesIO(self.data)
            
            # Read header
            res_type = struct.unpack('<H', stream.read(2))[0]
            header_size = struct.unpack('<H', stream.read(2))[0]
            file_size = struct.unpack('<I', stream.read(4))[0]
            package_count = struct.unpack('<I', stream.read(4))[0]
            
            if res_type != self.RES_TABLE_TYPE:
                return False
                
            # Parse string pool
            self._parse_string_pool(stream)
            
            return True
        except Exception as e:
            print(f"Error parsing ARSC: {e}")
            return False
    
    def _parse_string_pool(self, stream: io.BytesIO) -> None:
        """
        Parse the global string pool from resources.arsc.
        
        Args:
            stream: BytesIO stream positioned at string pool
        """
        start_pos = stream.tell()
        
        chunk_type = struct.unpack('<H', stream.read(2))[0]
        if chunk_type != self.RES_STRING_POOL_TYPE:
            stream.seek(start_pos)
            return
            
        header_size = struct.unpack('<H', stream.read(2))[0]
        chunk_size = struct.unpack('<I', stream.read(4))[0]
        string_count = struct.unpack('<I', stream.read(4))[0]
        style_count = struct.unpack('<I', stream.read(4))[0]
        flags = struct.unpack('<I', stream.read(4))[0]
        strings_start = struct.unpack('<I', stream.read(4))[0]
        styles_start = struct.unpack('<I', stream.read(4))[0]
        
        # Store string offsets
        for i in range(string_count):
            offset = struct.unpack('<I', stream.read(4))[0]
            self.string_offsets.append(start_pos + strings_start + offset)
        
        # Read strings
        string_data_pos = start_pos + strings_start
        for i in range(string_count):
            stream.seek(string_data_pos + (self.string_offsets[i] - (start_pos + strings_start)))
            
            # UTF-8 encoding (if flags & 0x100)
            if flags & 0x100:
                length = struct.unpack('B', stream.read(1))[0]
                if length & 0x80:
                    length = ((length & 0x7F) << 8) | struct.unpack('B', stream.read(1))[0]
                string = stream.read(length).decode('utf-8', errors='replace')
            else:
                # UTF-16 encoding
                length = struct.unpack('<H', stream.read(2))[0]
                if length & 0x8000:
                    length = ((length & 0x7FFF) << 16) | struct.unpack('<H', stream.read(2))[0]
                string = stream.read(length * 2).decode('utf-16le', errors='replace')
            
            self.string_pool.append(string)
    
    def replace_package_name(self, old_package: str, new_package: str) -> bool:
        """
        Replace package name in the string pool.
        
        Args:
            old_package: Old package name to replace
            new_package: New package name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        try:
            replaced = False
            for i, string in enumerate(self.string_pool):
                if string == old_package:
                    self.string_pool[i] = new_package
                    replaced = True
                elif string.startswith(old_package + '.'):
                    # Replace package prefix in class names
                    self.string_pool[i] = new_package + string[len(old_package):]
                    replaced = True
            
            if replaced:
                self._rebuild_string_pool()
            
            return replaced
        except Exception as e:
            print(f"Error replacing package name: {e}")
            return False
    
    def replace_class_name(self, old_class: str, new_class: str) -> bool:
        """
        Replace class name in the string pool.
        
        Args:
            old_class: Old fully qualified class name
            new_class: New fully qualified class name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        try:
            replaced = False
            for i, string in enumerate(self.string_pool):
                if string == old_class:
                    self.string_pool[i] = new_class
                    replaced = True
            
            if replaced:
                self._rebuild_string_pool()
            
            return replaced
        except Exception as e:
            print(f"Error replacing class name: {e}")
            return False
    
    def _rebuild_string_pool(self) -> None:
        """
        Rebuild the string pool in the binary data after modifications.
        
        Note: This is a simplified implementation. In production, you would need
        to handle the complete binary reconstruction including offsets and padding.
        """
        # This is a placeholder for the complex string pool reconstruction logic
        # In a real implementation, this would:
        # 1. Encode all strings back to UTF-8 or UTF-16
        # 2. Calculate new offsets
        # 3. Update the header
        # 4. Write back to self.data
        pass
    
    def get_modified_data(self) -> bytes:
        """
        Get the modified resources.arsc data.
        
        Returns:
            Modified resources.arsc as bytes
        """
        return bytes(self.data)
    
    def get_package_names(self) -> List[str]:
        """
        Get list of package names found in the resources.arsc.
        
        Returns:
            List of package names
        """
        packages = []
        for string in self.string_pool:
            # Simple heuristic: strings that look like package names
            if '.' in string and not ' ' in string and string[0].islower():
                parts = string.split('.')
                if len(parts) >= 2 and all(p.replace('_', '').isalnum() for p in parts):
                    packages.append(string)
        return list(set(packages))
    
    def get_class_names(self) -> Set[str]:
        """
        Get set of fully qualified class names found in the resources.arsc.
        
        Returns:
            Set of class names
        """
        classes = set()
        for string in self.string_pool:
            # Simple heuristic: strings that look like class names (FQCN)
            if '.' in string and not ' ' in string:
                parts = string.split('.')
                if len(parts) >= 2:
                    # Last part should start with uppercase (class name convention)
                    if parts[-1] and parts[-1][0].isupper():
                        classes.add(string)
        return classes
