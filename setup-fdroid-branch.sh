#!/bin/bash

# setup-fdroid-branch.sh
# Creates and sets up an F-Droid compatible branch

set -e  # Exit on error

echo "=========================================="
echo "F-Droid Branch Setup Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${RED}Error: Not in a git repository${NC}"
    exit 1
fi

# Check for uncommitted changes
if [[ -n $(git status -s) ]]; then
    echo -e "${YELLOW}Warning: You have uncommitted changes.${NC}"
    echo "Please commit or stash your changes before proceeding."
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $CURRENT_BRANCH"

# Create fdroid branch
FDROID_BRANCH="fdroid"
echo ""
echo "Creating F-Droid branch from $CURRENT_BRANCH..."

if git show-ref --verify --quiet refs/heads/$FDROID_BRANCH; then
    echo -e "${YELLOW}Warning: Branch '$FDROID_BRANCH' already exists.${NC}"
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git branch -D $FDROID_BRANCH
        echo "Deleted existing branch."
    else
        echo "Using existing branch."
        git checkout $FDROID_BRANCH
    fi
else
    git checkout -b $FDROID_BRANCH
    echo -e "${GREEN}Created new branch: $FDROID_BRANCH${NC}"
fi

echo ""
echo "Running F-Droid preparation scripts..."
echo ""

# Make scripts executable
chmod +x scripts/prepare-fdroid.sh
chmod +x scripts/cleanup-fdroid.sh
chmod +x scripts/validate-fdroid.sh

# Run preparation script
./scripts/prepare-fdroid.sh

echo ""
echo -e "${GREEN}=========================================="
echo "F-Droid branch setup complete!"
echo "==========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Review the changes: git status"
echo "2. Test the F-Droid build: ./gradlew assembleFdroidRelease"
echo "3. Commit changes: git add . && git commit -m 'Prepare F-Droid branch'"
echo "4. Push to remote: git push -u origin $FDROID_BRANCH"
echo ""
echo "To validate F-Droid compatibility:"
echo "  ./scripts/validate-fdroid.sh"
echo ""
echo "To switch back to main branch:"
echo "  git checkout $CURRENT_BRANCH"
echo ""
