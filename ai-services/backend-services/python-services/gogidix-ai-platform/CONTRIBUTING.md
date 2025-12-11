# Contributing to Gogidix AI Services

We welcome contributions from the community! This document provides guidelines for contributing to the Gogidix AI Services project.

## Table of Contents
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)
- [Community Guidelines](#community-guidelines)

## Getting Started

### 1. Fork and Clone
```bash
# Fork the repository on GitHub
git clone https://github.com/YOUR_USERNAME/ai-services.git
cd ai-services

# Add upstream remote
git remote add upstream https://github.com/gogidix/ai-services.git
```

### 2. Create a Branch
```bash
git checkout -b feature/your-feature-name
```

### 3. Install Dependencies
```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt
```

## Development Setup

### Pre-commit Hooks
```bash
# Install pre-commit hooks
pre-commit install

# Run pre-commit on all files
pre-commit run --all-files
```

### IDE Configuration

#### VS Code
Install these extensions:
- Python
- Pylance
- Black Formatter
- isort
- Docker
- Kubernetes
- GitLens

#### PyCharm
- Enable code style inspection
- Configure Black and isort
- Set up pytest runner

## Code Style

### Python Code Style
We use:
- **Black** for code formatting
- **isort** for import sorting
- **Flake8** for linting
- **MyPy** for type checking

### Code Formatting
```bash
# Format code
black src/ tests/

# Sort imports
isort src/ tests/

# Lint code
flake8 src/ tests/

# Type check
mypy src/
```

### Naming Conventions
- **Classes**: `PascalCase`
- **Functions/Variables**: `snake_case`
- **Constants**: `UPPER_SNAKE_CASE`
- **Private Members**: `_leading_underscore`

### Docstrings
All functions, classes, and modules should have docstrings:

```python
def calculate_property_value(features: Dict[str, Any]) -> float:
    """
    Calculate property value using machine learning model.

    Args:
        features: Dictionary of property features

    Returns:
        Estimated property value

    Example:
        >>> calculate_property_value({
        ...     "square_feet": 1200,
        ...     "bedrooms": 3
        ... })
        850000.0
    """
    pass
```

## Testing

### Test Requirements
- **Coverage**: Minimum 90% for new code
- **Unit Tests**: Test individual functions/methods
- **Integration Tests**: Test service interactions
- **E2E Tests**: Test complete workflows

### Running Tests
```bash
# Run all tests
pytest tests/

# Run with coverage
pytest --cov=src --cov-report=html tests/

# Run specific test file
pytest tests/test_model.py -v
```

### Writing Tests
```python
import pytest
from unittest.mock import Mock

from src.gogidix_ai.property_intelligence.valuation import PropertyValuationModel

class TestPropertyValuationModel:
    def test_init(self):
        """Test model initialization."""
        model = PropertyValuationModel()
        assert model.is_trained is False

    def test_predict(self):
        """Test prediction functionality."""
        model = PropertyValuationModel()
        model.load_model("test_model.pkl")

        features = {"square_feet": 1200, "bedrooms": 3}
        prediction = model.predict(features)

        assert isinstance(prediction, float)
        assert prediction > 0
```

## Submitting Changes

### 1. Create Pull Request
- Use a descriptive title
- Reference any related issues
- Include screenshots if applicable

### 2. Update Documentation
- Update relevant documentation
- Add examples for new features
- Update CHANGELOG.md

### 3. Code Review
- Ensure all tests pass
- Request at least one review
- Address all feedback

### 4. Merge
- Ensure CI/CD passes
- Resolve all conflicts
- Merge via GitHub interface or rebase

## Pull Request Template

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests pass locally
- [ ] Ready for review
```

## Reporting Bugs

### Bug Report Template
```markdown
## Bug Description
Clear and concise description of the bug

## Steps to Reproduce
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

### Expected Behavior
What you expected to happen

### Actual Behavior
What actually happened

### Environment
- OS: [e.g. Ubuntu 20.04]
- Python version: [e.g. 3.9.0]
- Dependencies: [list relevant versions]

### Additional Context
Any other context about the problem
```

## Feature Requests

### Feature Request Template
```markdown
## Feature Description
Clear and concise description of the feature

## Problem Statement
What problem does this feature solve?

## Proposed Solution
How do you envision implementing this feature?

## Alternatives Considered
What other approaches have you considered?

## Additional Context
Any other context or screenshots
```

## Development Workflow

### 1. Planning
- Create issue for discussion
- Get approval from maintainers
- Design implementation approach

### 2. Development
- Create feature branch
- Implement with tests
- Document changes

### 3. Review
- Create pull request
- Address feedback
- Update based on reviews

### 4. Release
- Merge to main branch
- Update version
- Create release notes

## Release Process

### Versioning
We follow Semantic Versioning (SemVer):
- `MAJOR`: Breaking changes
- `MINOR`: New features (backwards compatible)
- `PATCH`: Bug fixes

### Release Notes
Update CHANGELOG.md with:
- New features
- Bug fixes
- Breaking changes
- Migration guide

## Community Guidelines

### Code of Conduct
We are committed to providing a welcoming and inclusive environment. Please read our [Code of Conduct](CODE_OF_CONDUCT.md).

### Communication
- Be respectful and constructive
- Ask questions when unsure
- Help others learn and grow

### Getting Help
- Check [documentation](https://docs.gogidix.com)
- Search [existing issues](https://github.com/gogidix/ai-services/issues)
- Join our [Discussions](https://github.com/gogidix/ai-services/discussions)

## Recognition

### Contributors
All contributors are recognized in:
- README.md contributors section
- Release notes
- Annual report

### Types of Contributions
- Code contributions
- Documentation
- Bug reports
- Feature requests
- Community support
- Design and feedback

## Resources

### Documentation
- [API Documentation](https://docs.gogidix.com/api)
- [Architecture Guide](https://docs.gogidix.com/architecture)
- [Best Practices](https://docs.gogidix.com/best-practices)

### Tools and Resources
- [Python Style Guide](https://pep8.org/)
- [Black Code Formatter](https://black.readthedocs.io/)
- [pytest](https://pytest.org/)
- [Docker](https://docs.docker.com/)
- [Kubernetes](https://kubernetes.io/)

### Community
- [GitHub Discussions](https://github.com/gogidix/ai-services/discussions)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/gogidix-ai)
- [Discord Server](https://discord.gg/gogidix)

## License

By contributing, you agree that your contributions will be licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

- **Maintainers**: ai-team@gogidix.com
- **Security**: security@gogidix.com
- **Support**: support@gogidix.com

Thank you for contributing to Gogidix AI Services! 🚀