"""
ResourceProcessor - Main orchestrator for resource processing.

Coordinates all components to perform complete package/class name replacement.
"""

from typing import Dict, List, Set, Optional, Tuple
from .arsc_processor import ArscProcessor
from .xml_processor import XmlProcessor
from .databinding_processor import DataBindingProcessor
from .validator import SemanticValidator
from .whitelist import WhitelistFilter
from .dex_validator import DexValidator
from .transaction import TransactionManager
from .integrity import IntegrityVerifier


class ResourceProcessor:
    """
    Main processor that orchestrates all resource processing operations.
    
    Workflow:
    1. Parse resources and DEX files
    2. Validate class names semantically
    3. Filter using whitelist
    4. Cross-validate with DEX
    5. Perform replacements
    6. Verify integrity
    7. Commit or rollback
    """
    
    def __init__(self, own_packages: Optional[List[str]] = None):
        """
        Initialize ResourceProcessor.
        
        Args:
            own_packages: List of own package names to process
        """
        self.validator = SemanticValidator()
        self.whitelist = WhitelistFilter(own_packages)
        self.transaction = TransactionManager()
        self.integrity_verifier = IntegrityVerifier()
        
        self.arsc_processors: Dict[str, ArscProcessor] = {}
        self.xml_processors: Dict[str, XmlProcessor] = {}
        self.databinding_processors: Dict[str, DataBindingProcessor] = {}
        self.dex_validators: Dict[str, DexValidator] = {}
    
    def add_arsc_file(self, file_path: str, data: bytes) -> bool:
        """
        Add a resources.arsc file for processing.
        
        Args:
            file_path: Path to the file
            data: File data
            
        Returns:
            True if added successfully, False otherwise
        """
        try:
            processor = ArscProcessor(data)
            if processor.parse():
                self.arsc_processors[file_path] = processor
                return True
            return False
        except Exception as e:
            print(f"Error adding ARSC file {file_path}: {e}")
            return False
    
    def add_xml_file(self, file_path: str, data: bytes, file_type: str = "layout") -> bool:
        """
        Add a binary XML file for processing.
        
        Args:
            file_path: Path to the file
            data: File data
            file_type: Type of XML file (layout, menu, navigation, xml)
            
        Returns:
            True if added successfully, False otherwise
        """
        try:
            processor = XmlProcessor(data, file_type)
            if processor.parse():
                self.xml_processors[file_path] = processor
                return True
            return False
        except Exception as e:
            print(f"Error adding XML file {file_path}: {e}")
            return False
    
    def add_databinding_file(self, file_path: str, content: str) -> bool:
        """
        Add a Data Binding layout file for processing.
        
        Args:
            file_path: Path to the file
            content: XML content as string
            
        Returns:
            True if added successfully, False otherwise
        """
        try:
            processor = DataBindingProcessor(content)
            if processor.parse():
                self.databinding_processors[file_path] = processor
                return True
            return False
        except Exception as e:
            print(f"Error adding Data Binding file {file_path}: {e}")
            return False
    
    def add_dex_file(self, file_path: str, data: bytes) -> bool:
        """
        Add a DEX file for validation.
        
        Args:
            file_path: Path to the file
            data: File data
            
        Returns:
            True if added successfully, False otherwise
        """
        try:
            validator = DexValidator(data)
            if validator.parse():
                self.dex_validators[file_path] = validator
                return True
            return False
        except Exception as e:
            print(f"Error adding DEX file {file_path}: {e}")
            return False
    
    def get_all_class_references(self) -> Set[str]:
        """
        Get all class references from all processed files.
        
        Returns:
            Set of fully qualified class names
        """
        classes = set()
        
        # From ARSC
        for processor in self.arsc_processors.values():
            classes.update(processor.get_class_names())
        
        # From XML
        for processor in self.xml_processors.values():
            classes.update(processor.get_class_references())
        
        # From Data Binding
        for processor in self.databinding_processors.values():
            classes.update(processor.get_all_class_references())
        
        return classes
    
    def get_all_dex_classes(self) -> Set[str]:
        """
        Get all classes from DEX files.
        
        Returns:
            Set of class names from DEX
        """
        classes = set()
        for validator in self.dex_validators.values():
            classes.update(validator.get_all_classes())
        return classes
    
    def validate_class_references(self) -> Tuple[Set[str], Set[str]]:
        """
        Validate class references against DEX files.
        
        Returns:
            Tuple of (valid_classes, invalid_classes)
        """
        references = self.get_all_class_references()
        dex_classes = self.get_all_dex_classes()
        
        # Filter by semantic validation
        valid_classes = self.validator.filter_class_names(references)
        
        # Filter by whitelist
        valid_classes = self.whitelist.filter_class_names(valid_classes)
        
        # Cross-validate with DEX
        if dex_classes:
            invalid_classes = valid_classes - dex_classes
            valid_classes = valid_classes & dex_classes
        else:
            invalid_classes = set()
        
        return valid_classes, invalid_classes
    
    def replace_package_name(self, old_package: str, new_package: str) -> bool:
        """
        Replace a package name in all resources.
        
        Args:
            old_package: Old package name
            new_package: New package name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        # Validate replacement
        if not self.validator.validate_replacement(old_package, new_package):
            print(f"Invalid replacement: {old_package} -> {new_package}")
            return False
        
        # Check whitelist
        if not self.whitelist.should_process(old_package):
            print(f"Package not in whitelist: {old_package}")
            return False
        
        try:
            # Replace in ARSC
            for processor in self.arsc_processors.values():
                processor.replace_package_name(old_package, new_package)
            
            # Replace in XML
            for processor in self.xml_processors.values():
                processor.replace_package_name(old_package, new_package)
            
            # Replace in Data Binding
            for processor in self.databinding_processors.values():
                processor.replace_package_name(old_package, new_package)
            
            return True
        except Exception as e:
            print(f"Error replacing package name: {e}")
            return False
    
    def replace_class_name(self, old_class: str, new_class: str) -> bool:
        """
        Replace a class name in all resources.
        
        Args:
            old_class: Old fully qualified class name
            new_class: New fully qualified class name
            
        Returns:
            True if replacement succeeded, False otherwise
        """
        # Validate replacement
        if not self.validator.validate_replacement(old_class, new_class):
            print(f"Invalid replacement: {old_class} -> {new_class}")
            return False
        
        # Check whitelist
        package = self.validator.extract_package_from_class(old_class)
        if package and not self.whitelist.should_process(package):
            print(f"Class package not in whitelist: {old_class}")
            return False
        
        # Validate against DEX
        dex_classes = self.get_all_dex_classes()
        if dex_classes and new_class not in dex_classes:
            print(f"New class not found in DEX: {new_class}")
            return False
        
        try:
            # Replace in ARSC
            for processor in self.arsc_processors.values():
                processor.replace_class_name(old_class, new_class)
            
            # Replace in XML
            for processor in self.xml_processors.values():
                processor.replace_class_name(old_class, new_class)
            
            # Replace in Data Binding
            for processor in self.databinding_processors.values():
                processor.replace_class_name(old_class, new_class)
            
            return True
        except Exception as e:
            print(f"Error replacing class name: {e}")
            return False
    
    def verify_integrity(self) -> Tuple[bool, List[str]]:
        """
        Verify integrity of processed resources.
        
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        all_errors = []
        
        # Verify ARSC files
        for file_path in self.arsc_processors:
            is_valid, errors = self.integrity_verifier.verify_resources_arsc(file_path)
            if not is_valid:
                all_errors.extend([f"{file_path}: {err}" for err in errors])
        
        # Verify XML files
        for file_path in self.xml_processors:
            is_valid, errors = self.integrity_verifier.verify_binary_xml(file_path)
            if not is_valid:
                all_errors.extend([f"{file_path}: {err}" for err in errors])
        
        return len(all_errors) == 0, all_errors
    
    def get_modified_data(self, file_path: str) -> Optional[bytes]:
        """
        Get modified data for a file.
        
        Args:
            file_path: Path to the file
            
        Returns:
            Modified data or None
        """
        if file_path in self.arsc_processors:
            return self.arsc_processors[file_path].get_modified_data()
        elif file_path in self.xml_processors:
            return self.xml_processors[file_path].get_modified_data()
        elif file_path in self.databinding_processors:
            return self.databinding_processors[file_path].get_modified_content().encode('utf-8')
        return None
