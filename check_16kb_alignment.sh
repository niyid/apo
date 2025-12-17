#!/bin/bash

# Script to verify all .so libraries meet Android's 16KB page size requirement
# Libraries with alignment > 16KB will be flagged by Google Play

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Checking library alignment for 16KB page size requirement..."
echo "================================================================"
echo ""

# Counter for issues
issues=0
total_libs=0

# Find all .so files
while IFS= read -r lib; do
    ((total_libs++))
    echo "Checking: $lib"
    
    # Use readelf to check program headers - look for Align column
    alignment=$(readelf -l "$lib" 2>/dev/null | grep "LOAD" | head -1 | awk '{print $NF}')
    
    # If that didn't work, try different parsing
    if [ -z "$alignment" ]; then
        alignment=$(readelf -l "$lib" 2>/dev/null | grep -E "^\s+LOAD" | head -1 | sed 's/.*0x\([0-9a-f]*\)$/\1/')
    fi
    
    if [ -z "$alignment" ]; then
        echo -e "${YELLOW}  ⚠ Could not determine alignment${NC}"
        echo "  Debug: readelf output:"
        readelf -l "$lib" 2>/dev/null | grep -A 2 "LOAD" | head -5
        echo ""
        ((issues++))
        continue
    fi
    
    # Convert hex to decimal if needed
    if [[ $alignment == 0x* ]]; then
        alignment=$((alignment))
    elif [[ $alignment =~ ^[0-9a-fA-F]+$ ]]; then
        # Pure hex without 0x prefix
        alignment=$((16#$alignment))
    fi
    
    # Check if alignment exceeds 16KB (16384 bytes)
    if [ "$alignment" -gt 16384 ]; then
        echo -e "${RED}  ✗ FAIL: Alignment is ${alignment} bytes ($(($alignment / 1024))KB)${NC}"
        echo -e "${RED}    This exceeds 16KB requirement!${NC}"
        ((issues++))
    elif [ "$alignment" -eq 16384 ]; then
        echo -e "${GREEN}  ✓ OK: Alignment is 16KB (exactly at limit)${NC}"
    else
        echo -e "${GREEN}  ✓ OK: Alignment is ${alignment} bytes ($(($alignment / 1024))KB)${NC}"
    fi
    
    # Show all LOAD segments for detailed info
    echo "  Segments:"
    readelf -l "$lib" | grep "LOAD" | while read -r line; do
        echo "    $line"
    done
    
    echo ""
done < <(find . -name "*.so" -type f)

echo "================================================================"
echo "Summary:"
echo "  Total libraries checked: $total_libs"

if [ $issues -eq 0 ]; then
    echo -e "${GREEN}  ✓ All libraries meet 16KB requirement!${NC}"
    exit 0
else
    echo -e "${RED}  ✗ Found $issues issue(s) - libraries need recompilation with -DANDROID_MAX_PAGE_SIZE=16384${NC}"
    exit 1
fi
