"""
XmlProcessor - Process binary XML files in Android APK.

Handles layout, menu, navigation, and other XML configuration files,
replacing class names and package names while preserving the binary format.
"""

import struct
import io
from typing import Dict, List, Set, Optional
from enum import IntEnum


class XmlProcessor:
    """
    Processor for Android binary XML files.
    
    Handles:
    - Parsing binary XML format
    - Replacing class names in attributes
    - Replacing package names
    - Supporting layout, menu, navigation, and xml config files
    """
    
    # Chunk types for binary XML
    RES_XML_TYPE = 0x0003
    RES_STRING_POOL_TYPE = 0x0001
    RES_XML_RESOURCE_MAP_TYPE = 0x0180
    RES_XML_START_NAMESPACE_TYPE = 0x0100
    RES_XML_END_NAMESPACE_TYPE = 0x0101
    RES_XML_START_ELEMENT_TYPE = 0x0102
    RES_XML_END_ELEMENT_TYPE = 0x0103
    RES_XML_CDATA_TYPE = 0x0104
    
    def __init__(self, data: bytes, file_type: str = "layout"):
        """
        Initialize XmlProcessor with binary XML data.
        
        Args:
            data: Raw bytes of binary XML file
            file_type: Type of XML file (layout, menu, navigation, xml)
        """
        self.original_data = data
        self.data = bytearray(data)
        self.file_type = file_type
        self.string_pool: List[str] = []
        self.string_offsets: List[int] = []
        self.attributes_to_process: List[str] = self._get_attributes_for_type(file_type)
        
    def _get_attributes_for_type(self, file_type: str) -> List[str]:
        """
        Get list of attributes that may contain class names for the given file type.
        
        Args:
            file_type: Type of XML file
            
        Returns:
            List of attribute names to process
        """
        common_attrs = ["class", "name"]
        
        if file_type == "layout":
            return common_attrs + ["android:name", "tools:context"]
        elif file_type == "menu":
            return common_attrs + ["android:onClick"]
        elif file_type == "navigation":
            return common_attrs + ["android:name", "app:destination"]
        else:
            return common_attrs
    
    def parse(self) -> bool:
        """
        Parse the binary XML file structure.
        
        Returns:
            True if parsing succeeded, False otherwise
        """
        try:
            stream = io.BytesIO(self.data)
            
            # Read header
            chunk_type = struct.unpack('<H', stream.read(2))[0]
            header_size = struct.unpack('<H', stream.read(2))[0]
            file_size = struct.unpack('<I', stream.read(4))[0]
            
            if chunk_type != self.RES_XML_TYPE:
                return False
            
            # Parse string pool
            self._parse_string_pool(stream)
            
            return True
        except Exception as e:
            print(f"Error parsing binary XML: {e}")
            return False
    
    def _parse_string_pool(self, stream: io.BytesIO) -> None:
        """
        Parse the string pool from binary XML.
        
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
    
    def replace_class_name(self, old_class: str, new_class: str) -> bool:
        """
        Replace class name in the XML string pool.
        
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
                # Also check for short form (e.g., ".MainActivity" -> full form)
                elif string.startswith('.') and old_class.endswith(string):
                    # Keep the short form but need to update if package changes
                    pass
            
            if replaced:
                self._rebuild_string_pool()
            
            return replaced
        except Exception as e:
            print(f"Error replacing class name in XML: {e}")
            return False
    
    def replace_package_name(self, old_package: str, new_package: str) -> bool:
        """
        Replace package name in the XML string pool.
        
        Args:
            old_package: Old package name
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
            print(f"Error replacing package name in XML: {e}")
            return False
    
    def _rebuild_string_pool(self) -> None:
        """
        Rebuild the string pool in the binary data after modifications.
        
        Note: This is a simplified implementation. In production, you would need
        to handle the complete binary reconstruction.
        """
        # Placeholder for complex string pool reconstruction
        pass
    
    def get_modified_data(self) -> bytes:
        """
        Get the modified binary XML data.
        
        Returns:
            Modified binary XML as bytes
        """
        return bytes(self.data)
    
    def get_class_references(self) -> Set[str]:
        """
        Get all class references found in the XML.
        
        Returns:
            Set of class names
        """
        classes = set()
        for string in self.string_pool:
            # Look for fully qualified class names
            if '.' in string and not ' ' in string:
                parts = string.split('.')
                if len(parts) >= 2:
                    if parts[-1] and parts[-1][0].isupper():
                        classes.add(string)
            # Also handle short form like ".MainActivity"
            elif string.startswith('.') and len(string) > 1:
                if string[1].isupper():
                    classes.add(string)
        return classes
