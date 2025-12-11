# Contributing to Gogidix AI Services

Thank you for your interest in contributing to Gogidix AI Services! This guide provides information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Development Setup](#development-setup)
- [Contributing Guidelines](#contributing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [Testing](#testing)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Development Setup

### Prerequisites

- Python 3.9 or higher
- Docker and Docker Compose
- Git
- NVIDIA GPU with CUDA support (for ML development)
- Access to Gogidix development resources

### Setup Steps

1. **Fork the Repository**
   ```bash
   git clone https://github.com/your-username/ai-services.git
   cd ai-services
   ```

2. **Create Virtual Environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install Dependencies**
   ```bash
   pip install -e ".[dev]"
   ```

4. **Setup Pre-commit Hooks**
   ```bash
   pre-commit install
   ```

5. **Environment Configuration**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

6. **Start Development Services**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

7. **Run Initial Migration**
   ```bash
   alembic upgrade head
   ```

## Contributing Guidelines

### What to Contribute

We welcome contributions in the following areas:

- **Bug fixes**: Help us squash bugs!
- **New features**: Propose new AI models, APIs, or features
- **Documentation**: Improve docs, tutorials, and examples
- **Performance**: Optimize models and infrastructure
- **Testing**: Add or improve test coverage
- **Security**: Help identify and fix vulnerabilities

### Before You Start

1. Check the [issues](https://github.com/gogidix/ai-services/issues) for existing discussions
2. Create an issue for your proposed change if one doesn't exist
3. Wait for feedback from maintainers
4. Fork the repository and create a feature branch

### Branch Naming

- Use descriptive branch names
- Prefix with type of change:
  - `feat/` for new features
  - `fix/` for bug fixes
  - `docs/` for documentation
  - `refactor/` for refactoring
  - `test/` for test-related changes
  - `perf/` for performance improvements

Examples:
- `feat/property-valuation-model-v2`
- `fix/authentication-bug`
- `docs/api-examples`

## Pull Request Process

### 1. Prepare Your PR

- Ensure your code follows our style guidelines
- Add tests for new functionality
- Update documentation as needed
- Run all tests locally

### 2. Create Pull Request

1. Push your branch to your fork
2. Create a pull request against `main` branch
3. Fill out the PR template completely
4. Link any relevant issues

### 3. PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Performance tests pass

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] CI/CD passes
```

### 4. Review Process

- Automated checks must pass
- At least one maintainer approval required
- Address all review feedback
- PR must be up-to-date with main branch

## Code Style

### Python Code Style

We follow:
- [PEP 8](https://www.python.org/dev/peps/pep-0008/) for Python style
- [Black](https://black.readthedocs.io/) for formatting
- [isort](https://isort.readthedocs.io/) for import sorting
- [flake8](https://flake8.pycqa.org/) for linting
- [mypy](https://mypy.readthedocs.io/) for type checking

### Formatting

Run formatting tools before committing:
```bash
black .
isort .
```

### Linting

Check for lint issues:
```bash
flake8 .
mypy .
```

### Type Hints

All new code must include type hints:
```python
from typing import List, Dict, Optional

def process_data(
    data: List[Dict[str, Any]],
    config: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    """Process the input data and return results."""
    ...
```

### Documentation

All public functions and classes must have docstrings:
```python
def train_model(
    training_data: pd.DataFrame,
    hyperparameters: Dict[str, Any]
) -> Model:
    """
    Train a machine learning model.

    Args:
        training_data: DataFrame containing training features and labels
        hyperparameters: Dictionary of model hyperparameters

    Returns:
        Trained model instance

    Raises:
        ValueError: If training_data is empty or invalid
    """
    ...
```

## Testing

### Test Structure

```
tests/
├── unit/           # Unit tests
├── integration/    # Integration tests
├── e2e/           # End-to-end tests
├── fixtures/      # Test data
└── conftest.py    # Pytest configuration
```

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src --cov-report=html

# Run specific test file
pytest tests/unit/test_models.py

# Run with specific marker
pytest -m "slow"
```

### Writing Tests

- Test files should be named `test_*.py`
- Test functions should start with `test_`
- Use descriptive test names
- Follow AAA pattern: Arrange, Act, Assert

```python
def test_property_valuation_model_returns_valid_prediction():
    # Arrange
    model = PropertyValuationModel()
    property_data = {
        "square_feet": 1200,
        "bedrooms": 3,
        "bathrooms": 2,
        "location": "Manhattan, NY"
    }

    # Act
    prediction = model.predict(property_data)

    # Assert
    assert prediction["estimated_value"] > 0
    assert 0 <= prediction["confidence"] <= 1
    assert "price_per_sqft" in prediction
```

### Test Coverage

- Aim for >90% code coverage
- All critical paths must be tested
- Edge cases and error conditions need tests

## Documentation

### Types of Documentation

1. **API Documentation**: Auto-generated from docstrings
2. **User Guides**: Step-by-step tutorials
3. **Developer Docs**: Architecture and design decisions
4. **Reference Docs**: Complete API reference

### Writing Documentation

- Use clear, concise language
- Include code examples
- Add diagrams where helpful
- Keep documentation up-to-date

### Building Docs

```bash
# Build documentation
cd docs/
make html

# Serve locally
python -m http.server 8000
```

## Performance Guidelines

### Code Performance

- Profile code before optimizing
- Use appropriate data structures
- Cache expensive computations
- Optimize database queries

### ML Model Performance

- Monitor model inference time
- Track memory usage
- Implement model versioning
- Use batch inference when possible

### Example: Efficient Model Loading

```python
class ModelManager:
    def __init__(self):
        self._models: Dict[str, Any] = {}
        self._model_lock = asyncio.Lock()

    async def get_model(self, model_name: str) -> Any:
        if model_name not in self._models:
            async with self._model_lock:
                if model_name not in self._models:
                    self._models[model_name] = await self._load_model(model_name)
        return self._models[model_name]
```

## Security Guidelines

### Secure Coding Practices

- Never commit secrets or API keys
- Validate all inputs
- Use parameterized queries
- Implement proper authentication
- Follow the principle of least privilege

### Security Testing

- Run security scans regularly
- Test for common vulnerabilities
- Review dependencies for known issues

```python
# Example: Input validation
from pydantic import BaseModel, validator

class PropertyData(BaseModel):
    square_feet: int

    @validator('square_feet')
    def validate_square_feet(cls, v):
        if v <= 0 or v > 100000:
            raise ValueError('Invalid square footage')
        return v
```

## Release Process

### Version Management

We use [Semantic Versioning](https://semver.org/):
- Major: Breaking changes
- Minor: New features (backward compatible)
- Patch: Bug fixes (backward compatible)

### Release Checklist

1. Update version number
2. Update CHANGELOG.md
3. Tag the release
4. Create GitHub release
5. Deploy to production

## Getting Help

### Resources

- [Documentation](https://docs.gogidix.com/ai-services)
- [API Reference](https://api.gogidix.com/docs)
- [GitHub Issues](https://github.com/gogidix/ai-services/issues)

### Contact

- Create a GitHub issue for bugs or features
- Start a discussion for questions
- Email: ai-team@gogidix.com

## Recognition

Contributors are recognized in:
- README.md contributors section
- Release notes
- Annual contributor awards

Thank you for contributing to Gogidix AI Services! 🚀