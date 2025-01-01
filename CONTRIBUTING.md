# Contributing to VigiLens

> [!NOTE]
> First off, thank you for considering contributing to VigiLens! It's people like you that make VigiLens such a great tool.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Java Coding Guidelines](#java-coding-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

> [!IMPORTANT]
> By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

### Our Standards

- Using welcoming and inclusive language
- Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

## Getting Started

### Find an Issue

> [!TIP]
> Look for issues labeled `good first issue` or `help wanted` to get started quickly!

1. Check our [Issue Tracker](../../issues)
2. Look through our [Roadmap](https://VigiLens.shashi.app/roadmap)
3. Join discussions in our [Community Forums](https://VigiLens.shashi.app/community)

### Types of Contributions

- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation improvements
- 🌍 Translations
- 🎨 UI/UX enhancements
- ⚡ Performance improvements

## Development Setup

### Prerequisites

```bash
# Required Tools
- Android Studio Arctic Fox or newer
- JDK 11+
- Android SDK 30+
- Git
```

### Environment Setup

1. Fork the Repository
```bash
# Clone your fork
git clone https://github.com/ShashiLab/VigiLens.git

# Add upstream remote
git remote add upstream https://github.com/ShashiLab/VigiLens.git

# Create a new branch
git checkout -b feature/feature-name
```

2. Android Studio Setup
```bash
# Open project in Android Studio
- Import project
- Wait for Gradle sync
- Verify build success
```

> [!CAUTION]
> Never commit sensitive information or API keys to the repository!

## Pull Request Process

### 1. Before Creating a PR

- Update your fork
```bash
git fetch upstream
git rebase upstream/main
```

- Run all tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### 2. Creating Your PR

> [!TIP]
> Use our PR template to ensure you provide all necessary information!

- Write clear, concise commit messages
- Reference related issues
- Update documentation
- Add tests for new features
- Follow the PR template

### 3. PR Review Process

1. Automated checks must pass
2. Code review by maintainers
3. Documentation review
4. Testing verification
5. Final approval

## Java Coding Guidelines

### Code Style

```java
// File structure example
public class RecordingManager {
    // Constants
    private static final int MAX_DURATION = 3600;
    
    // Instance variables
    private final Context context;
    private boolean isRecording;
    
    // Constructor
    public RecordingManager(Context context) {
        this.context = context;
    }
    
    // Public methods
    public boolean startRecording(RecordingConfig config) {
        // Implementation
    }
    
    // Private helper methods
    private void validatePermissions() {
        // Implementation
    }
}
```

### Best Practices

- Follow SOLID principles
- Use meaningful variable and method names
- Keep methods focused and concise
- Implement proper exception handling
- Use appropriate access modifiers
- Document public APIs
- Follow Android best practices

### Code Quality

> [!IMPORTANT]
> All code must pass these quality checks:

- CheckStyle
- PMD
- SpotBugs
- Android Lint
- Unit test coverage (minimum 70%)

## Testing Guidelines

### Unit Tests (JUnit)

```java
public class RecordingManagerTest {
    @Test
    public void startRecording_WithValidConfig_ReturnsTrue() {
        // Arrange
        RecordingManager manager = new RecordingManager(mockContext);
        RecordingConfig config = new RecordingConfig.Builder()
            .setQuality(Quality.HD)
            .build();
            
        // Act
        boolean result = manager.startRecording(config);
        
        // Assert
        assertTrue(result);
    }
}
```

### Instrumentation Tests (Espresso)

```java
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
        new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void clickRecordButton_StartsRecording() {
        // Arrange & Act
        onView(withId(R.id.record_button))
            .perform(click());
            
        // Assert
        onView(withId(R.id.recording_indicator))
            .check(matches(isDisplayed()));
    }
}
```

## Documentation

### JavaDoc Standards

```java
/**
 * Records video in background mode with specified configuration.
 *
 * @param config The recording configuration including quality and duration settings
 * @return true if recording started successfully, false otherwise
 * @throws SecurityException if required permissions are not granted
 * @throws IllegalStateException if recording is already in progress
 */
public boolean startRecording(RecordingConfig config) {
    // Implementation
}
```

### Required Documentation

- Class-level JavaDoc explaining purpose and usage
- Public method documentation
- Complex algorithm explanations
- Important implementation notes
- README.md updates for new features
- CHANGELOG.md entries

## Building and Testing

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

### Continuous Integration

> [!NOTE]
> Our CI pipeline runs these checks on every PR:

1. CheckStyle verification
2. Static analysis (PMD & SpotBugs)
3. Unit tests
4. Instrumentation tests
5. Build verification

## Community

### Getting Help

- 💬 [Discord Server](https://discord.gg/VigiLens)
- 📧 [Development Mailing List](mailto:VigiLens-dev@googlegroups.com)
- 🤝 [Developer Forums](https://VigiLens.shashi.app/forums)


## Recognition

> [!NOTE]
> All contributors are recognized in our:

- [Contributors page](https://VigiLens.shashi.app/contributors)
- Release notes
- Annual reports

### Contributor Levels

| Level | Requirements | Benefits |
|-------|--------------|----------|
| Rookie | 1-5 PRs merged | Name in contributors list |
| Regular | 6-20 PRs merged | Badge + Access to dev channels |
| Expert | 20+ PRs merged | Direct commit access + Mentorship rights |

## Questions?

Feel free to reach out:
- 📧 Email: development@VigiLens.shashi.app
- 💬 Discord: [VigiLens Developers](https://discord.gg/VigiLens)
- 🐦 Twitter: [@VigiLensdev](https://twitter.com/VigiLensdev)

---

<div align="center">

Thank you for contributing to VigiLens! ❤️

[Website](https://VigiLens.shashi.app) • [GitHub](https://github.com/ShashiLab/VigiLens) • [Report Issues](../../issues)

</div>