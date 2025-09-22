#!/bin/bash

# PDF Utilities Release Script
# This script helps create releases by updating version and creating tags

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to validate version format
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?$ ]]; then
        print_error "Invalid version format: $version"
        print_error "Expected format: X.Y.Z or X.Y.Z-suffix (e.g., 1.0.0, 1.2.3-beta)"
        exit 1
    fi
}

# Function to check if we're in a git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository"
        exit 1
    fi
}

# Function to check if working directory is clean
check_clean_working_dir() {
    if ! git diff-index --quiet HEAD --; then
        print_error "Working directory is not clean. Please commit or stash changes first."
        git status --short
        exit 1
    fi
}

# Function to check if tag already exists
check_tag_exists() {
    local tag=$1
    if git rev-parse "$tag" >/dev/null 2>&1; then
        print_error "Tag $tag already exists"
        exit 1
    fi
}

# Function to update version in pom.xml
update_pom_version() {
    local version=$1
    print_status "Updating version in pom.xml to $version"
    
    # Update main version
    sed -i.bak "s/<version>.*<\/version>/<version>$version<\/version>/" pom.xml
    # Update app.version property
    sed -i.bak "s/<app\.version>.*<\/app\.version>/<app.version>$version<\/app.version>/" pom.xml
    # Update all jpackage app-version arguments
    sed -i.bak "s/--app-version [0-9.]*/--app-version $version/g" pom.xml
    
    # Remove backup files
    rm -f pom.xml.bak
    
    print_success "Updated pom.xml with version $version"
}

# Function to create and push tag
create_and_push_tag() {
    local version=$1
    local tag="v$version"
    
    print_status "Creating tag $tag"
    git add pom.xml
    git commit -m "Release version $version" || print_warning "No changes to commit"
    
    git tag -a "$tag" -m "Release version $version"
    print_success "Created tag $tag"
    
    print_status "Pushing tag to remote..."
    git push origin "$tag"
    print_success "Pushed tag $tag to remote"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <version> [options]"
    echo ""
    echo "Arguments:"
    echo "  version    Version to release (e.g., 1.0.0, 1.2.3-beta)"
    echo ""
    echo "Options:"
    echo "  --dry-run  Show what would be done without making changes"
    echo "  --help     Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 1.0.0"
    echo "  $0 1.2.3-beta"
    echo "  $0 2.0.0 --dry-run"
}

# Main function
main() {
    local version=""
    local dry_run=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                dry_run=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            -*)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$version" ]]; then
                    version=$1
                else
                    print_error "Multiple versions specified"
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Check if version is provided
    if [[ -z "$version" ]]; then
        print_error "Version is required"
        show_usage
        exit 1
    fi
    
    # Validate version format
    validate_version "$version"
    
    # Check git repository
    check_git_repo
    
    # Check working directory
    if [[ "$dry_run" == "false" ]]; then
        check_clean_working_dir
    fi
    
    # Check if tag already exists
    local tag="v$version"
    if [[ "$dry_run" == "false" ]]; then
        check_tag_exists "$tag"
    fi
    
    print_status "Preparing release $version"
    
    if [[ "$dry_run" == "true" ]]; then
        print_warning "DRY RUN MODE - No changes will be made"
        echo ""
        echo "Would perform the following actions:"
        echo "1. Update pom.xml version to $version"
        echo "2. Commit changes with message 'Release version $version'"
        echo "3. Create tag $tag"
        echo "4. Push tag to remote"
        echo "5. Trigger GitHub Actions release workflow"
        echo ""
        print_status "To perform the actual release, run without --dry-run"
    else
        # Update version in pom.xml
        update_pom_version "$version"
        
        # Create and push tag
        create_and_push_tag "$version"
        
        print_success "Release $version prepared successfully!"
        echo ""
        print_status "The GitHub Actions workflow will now:"
        echo "  - Build installers for all platforms"
        echo "  - Create a GitHub release"
        echo "  - Upload release artifacts"
        echo ""
        print_status "Monitor the workflow at:"
        echo "  https://github.com/$GITHUB_REPOSITORY/actions/workflows/production-release.yml"
        echo ""
        print_status "Or trigger manually at:"
        echo "  https://github.com/$GITHUB_REPOSITORY/actions/new?workflow=production-release.yml"
    fi
}

# Run main function with all arguments
main "$@"

