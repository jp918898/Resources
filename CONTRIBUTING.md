# Contributing to Resources Processor

Thank you for your interest in contributing to the Resources Processor! This document provides guidelines and instructions for contributing.

## 🚀 Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/Resources.git`
3. Create a new branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test your changes
6. Commit and push
7. Create a Pull Request

## 🔧 Development Setup

### Prerequisites

- Python 3.7 or higher
- pip

### Installation

```bash
# Clone the repository
git clone https://github.com/jp918898/Resources.git
cd Resources

# Install in development mode
pip install -e .

# Install development dependencies
pip install pytest
```

## 🧪 Testing

We use pytest for testing. All new features should include tests.

### Running Tests

```bash
# Run all tests
python -m pytest tests/

# Run specific test file
python -m pytest tests/test_validator.py

# Run with verbose output
python -m pytest tests/ -v

# Run with coverage
python -m pytest tests/ --cov=resources_processor
```

### Writing Tests

- Place test files in the `tests/` directory
- Name test files with `test_` prefix
- Use descriptive test names
- Test both success and failure cases
- Include edge cases

Example test:

```python
import unittest
from resources_processor.validator import SemanticValidator

class TestNewFeature(unittest.TestCase):
    def setUp(self):
        self.validator = SemanticValidator()
    
    def test_valid_case(self):
        self.assertTrue(self.validator.some_method('valid_input'))
    
    def test_invalid_case(self):
        self.assertFalse(self.validator.some_method('invalid_input'))
```

## 📝 Code Style

We follow PEP 8 style guidelines with some modifications:

- Use 4 spaces for indentation
- Maximum line length: 100 characters
- Use docstrings for all public methods and classes
- Use type hints where appropriate

### Docstring Format

```python
def method_name(param1: str, param2: int) -> bool:
    """
    Brief description of what the method does.
    
    Args:
        param1: Description of param1
        param2: Description of param2
        
    Returns:
        Description of return value
        
    Raises:
        ExceptionType: When this exception is raised
    """
    pass
```

## 🏗️ Project Structure

```
Resources/
├── src/resources_processor/   # Main source code
│   ├── __init__.py
│   ├── arsc_processor.py
│   ├── xml_processor.py
│   ├── databinding_processor.py
│   ├── validator.py
│   ├── whitelist.py
│   ├── dex_validator.py
│   ├── transaction.py
│   ├── integrity.py
│   ├── processor.py
│   └── cli.py
├── tests/                     # Test files
│   ├── test_validator.py
│   ├── test_whitelist.py
│   ├── test_databinding.py
│   └── test_transaction.py
├── examples/                  # Example scripts
├── README.md
├── CONTRIBUTING.md
├── requirements.txt
└── setup.py
```

## 🐛 Reporting Bugs

When reporting bugs, please include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Step-by-step instructions
3. **Expected Behavior**: What you expected to happen
4. **Actual Behavior**: What actually happened
5. **Environment**: Python version, OS, etc.
6. **Code Sample**: Minimal code to reproduce the issue

## 💡 Feature Requests

When requesting features, please include:

1. **Use Case**: Why this feature is needed
2. **Description**: Detailed description of the feature
3. **Examples**: Example usage if possible
4. **Alternatives**: Alternative solutions you've considered

## 🔀 Pull Request Process

1. **Update Tests**: Add or update tests for your changes
2. **Update Documentation**: Update README and docstrings
3. **Run Tests**: Ensure all tests pass
4. **Code Style**: Follow the code style guidelines
5. **Commit Messages**: Use clear, descriptive commit messages
6. **PR Description**: Clearly describe what your PR does

### Commit Message Format

```
type: Brief description (50 chars or less)

More detailed explanation if needed. Wrap at 72 characters.
Explain the problem that this commit is solving.

Fixes #123
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `chore`: Maintenance tasks

## 📚 Documentation

- Update README.md for user-facing changes
- Update docstrings for API changes
- Add examples for new features
- Keep documentation clear and concise

## 🤝 Code Review

All submissions require review. We use GitHub pull requests for this purpose.

Reviewers will check:
- Code quality and style
- Test coverage
- Documentation
- Performance implications
- Security considerations

## ⚖️ License

By contributing, you agree that your contributions will be licensed under the MIT License.

## 🙏 Recognition

Contributors will be recognized in:
- GitHub contributors list
- Release notes for significant contributions

## 📧 Contact

- GitHub Issues: [Issues Page](https://github.com/jp918898/Resources/issues)
- Discussions: [Discussions Page](https://github.com/jp918898/Resources/discussions)

## 🎯 Areas for Contribution

We welcome contributions in these areas:

1. **Performance Optimization**: Make processors faster
2. **Format Support**: Add support for more Android formats
3. **Error Handling**: Improve error messages and handling
4. **Testing**: Increase test coverage
5. **Documentation**: Improve guides and examples
6. **Bug Fixes**: Fix reported issues
7. **Features**: Implement requested features

Thank you for contributing to Resources Processor! 🎉
