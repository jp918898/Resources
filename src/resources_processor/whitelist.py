"""
WhitelistFilter - Filter packages to only process own packages.

Prevents replacement of system and third-party library packages.
"""

from typing import Set, List, Optional


class WhitelistFilter:
    """
    Filter for managing package whitelists.
    
    Ensures that only own packages are processed, while system packages
    and third-party libraries are preserved.
    """
    
    # Common Android system packages
    SYSTEM_PACKAGES = {
        'android',
        'java',
        'javax',
        'kotlin',
        'kotlinx',
        'dalvik',
        'org.xml',
        'org.json',
        'org.w3c',
        'org.apache',
    }
    
    # Common third-party library packages
    COMMON_LIBRARY_PACKAGES = {
        'com.google.android',
        'com.google.firebase',
        'androidx',
        'com.squareup',
        'okhttp3',
        'retrofit2',
        'io.reactivex',
        'com.facebook',
        'com.twitter',
        'com.github',
        'org.jetbrains',
    }
    
    def __init__(self, own_packages: Optional[List[str]] = None):
        """
        Initialize WhitelistFilter.
        
        Args:
            own_packages: List of own package names to process
        """
        self.own_packages: Set[str] = set(own_packages) if own_packages else set()
        self.excluded_packages: Set[str] = set()
        self.excluded_packages.update(self.SYSTEM_PACKAGES)
        self.excluded_packages.update(self.COMMON_LIBRARY_PACKAGES)
    
    def add_own_package(self, package: str) -> None:
        """
        Add a package to the whitelist of own packages.
        
        Args:
            package: Package name to add
        """
        self.own_packages.add(package)
    
    def add_own_packages(self, packages: List[str]) -> None:
        """
        Add multiple packages to the whitelist.
        
        Args:
            packages: List of package names to add
        """
        self.own_packages.update(packages)
    
    def add_excluded_package(self, package: str) -> None:
        """
        Add a package to the exclusion list.
        
        Args:
            package: Package name to exclude
        """
        self.excluded_packages.add(package)
    
    def add_excluded_packages(self, packages: List[str]) -> None:
        """
        Add multiple packages to the exclusion list.
        
        Args:
            packages: List of package names to exclude
        """
        self.excluded_packages.update(packages)
    
    def is_own_package(self, package: str) -> bool:
        """
        Check if a package is in the own packages list.
        
        Args:
            package: Package name to check
            
        Returns:
            True if package is in own packages, False otherwise
        """
        # Exact match
        if package in self.own_packages:
            return True
        
        # Check if it's a sub-package of any own package
        for own_pkg in self.own_packages:
            if package.startswith(own_pkg + '.'):
                return True
        
        return False
    
    def is_system_package(self, package: str) -> bool:
        """
        Check if a package is a system package.
        
        Args:
            package: Package name to check
            
        Returns:
            True if package is a system package, False otherwise
        """
        # Check exact match
        if package in self.SYSTEM_PACKAGES:
            return True
        
        # Check if it starts with any system package
        for sys_pkg in self.SYSTEM_PACKAGES:
            if package.startswith(sys_pkg + '.') or package == sys_pkg:
                return True
        
        return False
    
    def is_library_package(self, package: str) -> bool:
        """
        Check if a package is a third-party library package.
        
        Args:
            package: Package name to check
            
        Returns:
            True if package is a library package, False otherwise
        """
        # Check exact match
        if package in self.COMMON_LIBRARY_PACKAGES:
            return True
        
        # Check if it starts with any library package
        for lib_pkg in self.COMMON_LIBRARY_PACKAGES:
            if package.startswith(lib_pkg + '.') or package == lib_pkg:
                return True
        
        return False
    
    def is_excluded(self, package: str) -> bool:
        """
        Check if a package should be excluded from processing.
        
        Args:
            package: Package name to check
            
        Returns:
            True if package should be excluded, False otherwise
        """
        # Check if in explicit exclusion list
        if package in self.excluded_packages:
            return True
        
        # Check if it's a sub-package of any excluded package
        for excluded_pkg in self.excluded_packages:
            if package.startswith(excluded_pkg + '.'):
                return True
        
        # System and library packages are excluded
        if self.is_system_package(package) or self.is_library_package(package):
            return True
        
        return False
    
    def should_process(self, package: str) -> bool:
        """
        Determine if a package should be processed.
        
        Args:
            package: Package name to check
            
        Returns:
            True if package should be processed, False otherwise
        """
        # Don't process if excluded
        if self.is_excluded(package):
            return False
        
        # Process if it's an own package
        if self.is_own_package(package):
            return True
        
        # If no own packages are specified, process everything that's not excluded
        if not self.own_packages:
            return True
        
        # Otherwise, don't process
        return False
    
    def filter_packages(self, packages: Set[str]) -> Set[str]:
        """
        Filter a set of packages to only include processable ones.
        
        Args:
            packages: Set of package names
            
        Returns:
            Filtered set of packages to process
        """
        return {pkg for pkg in packages if self.should_process(pkg)}
    
    def filter_class_names(self, class_names: Set[str]) -> Set[str]:
        """
        Filter class names based on their package.
        
        Args:
            class_names: Set of fully qualified class names
            
        Returns:
            Filtered set of class names to process
        """
        filtered = set()
        for class_name in class_names:
            # Extract package from class name
            parts = class_name.split('.')
            if len(parts) >= 2:
                package = '.'.join(parts[:-1])
                if self.should_process(package):
                    filtered.add(class_name)
        return filtered
