# Contributing to GullyCaster

Thank you for your interest in contributing to GullyCaster! This project is built for the cricket-loving communities of South Asia, and we welcome contributions from developers worldwide.

## 🌿 Branching Model (Gitflow)

| Branch | Purpose |
|--------|---------|
| `main` | Stable release code only |
| `develop` | Active development |
| `feature/feature-name` | New features (e.g., `feature/scoreboard-update`) |
| `bugfix/issue-number` | Bug fixes |
| `release/x.x.x` | Release preparation |

### Workflow

1. Fork the repository
2. Create a feature branch from `develop`
3. Make your changes
4. Submit a Pull Request to `develop`

## 💻 Code Style

### Kotlin
- Follow [ktlint](https://github.com/pinterest/ktlint) standards
- Run `./gradlew ktlintCheck` before submitting
- Comments must be in **English** for universal maintainability
- UI strings must be **localized** (see `res/values-*/strings.xml`)

### License Header
Every source file must include:

```kotlin
/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */
```

## 📝 Pull Request Policy

### Requirements
- [ ] Code follows ktlint standards
- [ ] All new strings are localized (EN, UR, HI, BN)
- [ ] No hardcoded API keys or secrets
- [ ] **Screen recording included** if UI changes are made

### Commit Messages
Use conventional commits:
```
feat: add zoom slider to camera preview
fix: resolve hotspot connection timeout
docs: update README with build instructions
```

## 🎯 Priority Guidelines

1. **Support low-end devices** - Don't merge code that increases RAM usage unnecessarily
2. **Thermal efficiency** - Consider battery and heat impact
3. **Regional diversity** - All features must work with RTL (Urdu) and all supported languages

## 🐛 Reporting Issues

When reporting bugs, please include:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Logs (if available)

## 💬 Questions?

Open a GitHub Discussion or reach out to the maintainers.

---

**Every contribution helps bring live cricket streaming to more communities!** 🏏
