"""
DexValidator - Cross-validate class names with DEX files.

Ensures that class names referenced in resources actually exist in the compiled DEX files.
"""

import struct
from typing import Set, Dict, List, Optional


class DexValidator:
    """
    Validator for DEX files.
    
    Parses DEX files and validates that class names exist in the compiled code.
    Prevents references to non-existent classes.
    """
    
    DEX_MAGIC = b'dex\n'
    
    def __init__(self, dex_data: bytes):
        """
        Initialize DexValidator with DEX file data.
        
        Args:
            dex_data: Raw bytes of DEX file
        """
        self.dex_data = dex_data
        self.class_names: Set[str] = set()
        self.string_ids: List[str] = []
        self.type_ids: List[str] = []
    
    def parse(self) -> bool:
        """
        Parse the DEX file and extract class names.
        
        Returns:
            True if parsing succeeded, False otherwise
        """
        try:
            # Verify DEX magic number
            if not self.dex_data.startswith(self.DEX_MAGIC):
                return False
            
            # Parse header
            offset = 8  # Skip magic and version
            checksum = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Skip SHA-1 signature (20 bytes)
            offset += 20
            
            file_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            header_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            endian_tag = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Skip link section
            offset += 8
            
            # Map offset
            map_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # String IDs
            string_ids_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            string_ids_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Type IDs
            type_ids_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            type_ids_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Proto IDs
            proto_ids_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            proto_ids_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Field IDs
            field_ids_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            field_ids_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Method IDs
            method_ids_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            method_ids_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Class definitions
            class_defs_size = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            class_defs_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Parse string IDs
            self._parse_string_ids(string_ids_off, string_ids_size)
            
            # Parse type IDs
            self._parse_type_ids(type_ids_off, type_ids_size)
            
            # Parse class definitions
            self._parse_class_defs(class_defs_off, class_defs_size)
            
            return True
        except Exception as e:
            print(f"Error parsing DEX file: {e}")
            return False
    
    def _parse_string_ids(self, offset: int, count: int) -> None:
        """
        Parse string IDs from DEX file.
        
        Args:
            offset: Offset to string IDs section
            count: Number of string IDs
        """
        for i in range(count):
            string_data_off = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Read string from string data
            string = self._read_string(string_data_off)
            self.string_ids.append(string)
    
    def _read_string(self, offset: int) -> str:
        """
        Read a string from DEX file.
        
        Args:
            offset: Offset to string data
            
        Returns:
            Decoded string
        """
        # Read ULEB128 length
        length, bytes_read = self._read_uleb128(offset)
        offset += bytes_read
        
        # Read string bytes
        string_bytes = self.dex_data[offset:offset+length]
        return string_bytes.decode('utf-8', errors='replace')
    
    def _read_uleb128(self, offset: int) -> tuple:
        """
        Read an unsigned LEB128 encoded value.
        
        Args:
            offset: Offset to start reading
            
        Returns:
            Tuple of (value, bytes_read)
        """
        result = 0
        shift = 0
        bytes_read = 0
        
        while True:
            byte = self.dex_data[offset + bytes_read]
            bytes_read += 1
            result |= (byte & 0x7f) << shift
            if (byte & 0x80) == 0:
                break
            shift += 7
        
        return result, bytes_read
    
    def _parse_type_ids(self, offset: int, count: int) -> None:
        """
        Parse type IDs from DEX file.
        
        Args:
            offset: Offset to type IDs section
            count: Number of type IDs
        """
        for i in range(count):
            descriptor_idx = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            if descriptor_idx < len(self.string_ids):
                self.type_ids.append(self.string_ids[descriptor_idx])
    
    def _parse_class_defs(self, offset: int, count: int) -> None:
        """
        Parse class definitions from DEX file.
        
        Args:
            offset: Offset to class definitions section
            count: Number of class definitions
        """
        for i in range(count):
            class_idx = struct.unpack('<I', self.dex_data[offset:offset+4])[0]
            offset += 4
            
            # Skip access flags, superclass, interfaces, source file, annotations, class data, static values
            offset += 28
            
            if class_idx < len(self.type_ids):
                descriptor = self.type_ids[class_idx]
                # Convert descriptor format (Lcom/example/Class;) to dotted format (com.example.Class)
                class_name = self._descriptor_to_class_name(descriptor)
                if class_name:
                    self.class_names.add(class_name)
    
    def _descriptor_to_class_name(self, descriptor: str) -> Optional[str]:
        """
        Convert DEX type descriptor to class name.
        
        Args:
            descriptor: DEX type descriptor (e.g., "Lcom/example/Class;")
            
        Returns:
            Class name in dotted format (e.g., "com.example.Class") or None
        """
        if descriptor.startswith('L') and descriptor.endswith(';'):
            # Remove L prefix and ; suffix, replace / with .
            class_name = descriptor[1:-1].replace('/', '.')
            return class_name
        return None
    
    def class_exists(self, class_name: str) -> bool:
        """
        Check if a class exists in the DEX file.
        
        Args:
            class_name: Fully qualified class name to check
            
        Returns:
            True if class exists, False otherwise
        """
        return class_name in self.class_names
    
    def validate_class_names(self, class_names: Set[str]) -> Dict[str, bool]:
        """
        Validate multiple class names against DEX.
        
        Args:
            class_names: Set of class names to validate
            
        Returns:
            Dictionary mapping class names to existence status
        """
        return {name: self.class_exists(name) for name in class_names}
    
    def get_all_classes(self) -> Set[str]:
        """
        Get all class names found in the DEX file.
        
        Returns:
            Set of all class names
        """
        return self.class_names.copy()
    
    def filter_existing_classes(self, class_names: Set[str]) -> Set[str]:
        """
        Filter class names to only include those that exist in DEX.
        
        Args:
            class_names: Set of class names to filter
            
        Returns:
            Set of class names that exist in DEX
        """
        return {name for name in class_names if self.class_exists(name)}
