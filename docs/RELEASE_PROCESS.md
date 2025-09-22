# Release Process Documentation

This document describes the automated release process for PDF Utilities using GitHub Actions.

## Overview

The production release workflow (`production-release.yml`) provides automated version management, multi-platform builds, and GitHub release creation.

## Release Triggers

### 1. Tag-based Release (Recommended)
```bash
# Create and push a version tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 2. Manual Release
- Go to GitHub Actions → Production Release
- Click "Run workflow"
- Fill in the version number (e.g., `1.0.0`)
- Select release type (stable/beta/alpha)
- Optionally create and push a tag automatically

## Version Management

### Version Format
- **Stable releases**: `X.Y.Z` (e.g., `1.0.0`, `1.2.3`)
- **Prereleases**: `X.Y.Z-suffix` (e.g., `1.0.0-beta`, `1.2.3-alpha`)

### Version Sources
1. **Tag-based**: Extracted from Git tag (e.g., `v1.0.0` → `1.0.0`)
2. **Manual**: Provided via workflow dispatch input
3. **Automatic**: Updates `pom.xml` with the specified version

### Version Updates
The workflow automatically updates:
- Main project version in `pom.xml`
- `app.version` property
- All `jpackage` `--app-version` arguments

## Release Process Flow

### 1. Prepare Release Job
- Validates version format
- Updates `pom.xml` with new version
- Generates release notes from Git commits
- Determines if release is prerelease

### 2. Build All Platforms Job
- Builds for Linux, macOS, and Windows
- Creates platform-specific installers
- Uploads build artifacts

### 3. Create Release Job
- Downloads all build artifacts
- Creates platform-specific archives
- Creates GitHub release with proper tagging
- Uploads release assets

### 4. Post-Release Job
- Updates development version to next snapshot
- Commits version bump to main branch
- Provides release summary

## Generated Artifacts

### Platform-Specific Archives
- `PDF-Utilities-{version}-Linux.zip` - Linux installers (DEB, RPM, AppImage)
- `PDF-Utilities-{version}-macOS.zip` - macOS DMG installer
- `PDF-Utilities-{version}-Windows.zip` - Windows MSI installer

### Combined Archive
- `PDF-Utilities-{version}-all-platforms.zip` - All platform installers

## Release Notes

### Automatic Generation
- Extracts commits since last tag
- Formats as markdown changelog
- Includes system requirements
- Lists supported platforms

### Manual Customization
You can customize release notes by:
1. Editing the generated `release_notes.md` in the workflow
2. Modifying the changelog generation logic
3. Adding custom sections for major features

## Prerelease Handling

### Automatic Detection
Prereleases are automatically detected if version contains:
- `alpha`
- `beta`
- `rc`
- `pre`

### Prerelease Benefits
- Marked as prerelease in GitHub
- Won't trigger automatic updates
- Clearly labeled for testing

## Development Version Management

### Automatic Bump
After stable releases, the workflow automatically:
1. Increments patch version
2. Adds `-SNAPSHOT` suffix
3. Commits to main branch

Example: `1.0.0` → `1.0.1-SNAPSHOT`

## Workflow Configuration

### Environment Variables
- `JAVA_VERSION`: Java version to use (default: 24)
- `MAVEN_OPTS`: Maven JVM options

### Required Secrets
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions

### Matrix Strategy
- **Linux**: Ubuntu latest
- **macOS**: macOS latest  
- **Windows**: Windows latest

## Troubleshooting

### Common Issues

#### 1. Version Format Error
```
Error: Invalid version format. Expected: X.Y.Z or X.Y.Z-suffix
```
**Solution**: Use semantic versioning format (e.g., `1.0.0`, `1.2.3-beta`)

#### 2. Build Failures
- Check Java version compatibility
- Verify Maven dependencies
- Review platform-specific build logs

#### 3. Release Creation Failed
- Ensure `GITHUB_TOKEN` has proper permissions
- Check if tag already exists
- Verify release name doesn't conflict

### Debug Steps
1. Check workflow logs in GitHub Actions
2. Review artifact uploads
3. Verify version updates in `pom.xml`
4. Test with prerelease versions first

## Best Practices

### 1. Version Naming
- Use semantic versioning (SemVer)
- Tag with `v` prefix (e.g., `v1.0.0`)
- Use descriptive commit messages

### 2. Release Testing
- Test with prerelease versions first
- Verify all platform builds
- Check installer functionality

### 3. Documentation
- Update CHANGELOG.md manually
- Document breaking changes
- Include migration guides

### 4. Rollback
- Delete problematic releases
- Remove tags if needed
- Revert version in `pom.xml`

## Manual Release Steps (Fallback)

If automated release fails:

1. **Update version manually**:
   ```bash
   # Update pom.xml version
   sed -i 's/<version>.*<\/version>/<version>1.0.0<\/version>/' pom.xml
   ```

2. **Create and push tag**:
   ```bash
   git add pom.xml
   git commit -m "Release version 1.0.0"
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin main v1.0.0
   ```

3. **Build locally**:
   ```bash
   mvn clean package
   ./package-app.sh  # or package-app.bat on Windows
   ```

4. **Create GitHub release manually**:
   - Go to GitHub → Releases
   - Click "Create a new release"
   - Select tag `v1.0.0`
   - Upload build artifacts
   - Publish release

## Monitoring

### Release Status
- Check GitHub Actions workflow status
- Monitor artifact uploads
- Verify release creation

### Notifications
- GitHub Actions sends notifications on failure
- Release creation triggers repository notifications
- Consider setting up webhooks for external monitoring

## Security Considerations

### Token Permissions
- `GITHUB_TOKEN` has limited scope
- Only repository-level permissions
- No access to other repositories

### Build Security
- Uses official GitHub Actions runners
- No custom secrets required
- Maven dependencies from trusted sources

### Release Integrity
- All artifacts are built in isolated environments
- Version validation prevents injection
- Git tags provide integrity verification

