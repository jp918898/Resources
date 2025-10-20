from setuptools import setup, find_packages

setup(
    name="resources-processor",
    version="1.0.0",
    description="Industrial-grade resources.arsc and binary XML processing tool for Android APK package/class name randomization",
    author="jp918898",
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    python_requires=">=3.7",
    install_requires=[
        "lxml>=4.9.0",
    ],
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
    ],
)
