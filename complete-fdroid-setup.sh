#!/bin/bash

# complete-fdroid-setup.sh
# Complete F-Droid setup including branch creation, graphics, and metadata

set -e

echo "=========================================="
echo "Complete F-Droid Setup Script for Àpò"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Error: Not in a git repository"
    exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
FDROID_BRANCH="fdroid"

echo -e "${CYAN}Step 1/5: Creating F-Droid branch${NC}"
echo "----------------------------------------"

if git show-ref --verify --quiet refs/heads/$FDROID_BRANCH; then
    echo -e "${YELLOW}Branch '$FDROID_BRANCH' already exists.${NC}"
    git checkout $FDROID_BRANCH
else
    git checkout -b $FDROID_BRANCH
    echo -e "${GREEN}✓ Created new branch: $FDROID_BRANCH${NC}"
fi

echo ""
echo -e "${CYAN}Step 2/5: Organizing graphics${NC}"
echo "----------------------------------------"
./organize-fdroid-graphics.sh

echo ""
echo -e "${CYAN}Step 3/5: Preparing build configuration${NC}"
echo "----------------------------------------"
./scripts/prepare-fdroid.sh

echo ""
echo -e "${CYAN}Step 4/5: Cleaning proprietary files${NC}"
echo "----------------------------------------"
./scripts/cleanup-fdroid.sh

echo ""
echo -e "${CYAN}Step 5/5: Validating${NC}"
echo "----------------------------------------"
./scripts/validate-fdroid.sh || echo "Some validations failed - review above"

echo ""
echo -e "${GREEN}=========================================="
echo "Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Review changes: git status"
echo "2. Update build.gradle.kts with F-Droid flavors (see documentation)"
echo "3. Test build: ./scripts/test-fdroid-build.sh"
echo "4. Commit: git add . && git commit -m 'Prepare F-Droid branch'"
echo "5. Push: git push -u origin $FDROID_BRANCH"
echo ""
