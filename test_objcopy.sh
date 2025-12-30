#!/bin/bash

# Test script to verify objcopy actually realigns libraries

echo "=== Testing objcopy realignment ==="

# Find a test library
TEST_LIB=$(find app/build/intermediates/stripped_native_libs -name "libbarhopper_v3.so" -type f | head -1)

if [ -z "$TEST_LIB" ]; then
    echo "Error: No test library found. Run a build first."
    exit 1
fi

echo "Testing with: $TEST_LIB"

# Check current alignment
echo -e "\n--- BEFORE objcopy ---"
readelf -Wl "$TEST_LIB" | grep "LOAD" | head -2

# Make a copy and realign it
cp "$TEST_LIB" /tmp/test_lib.so
objcopy --set-section-alignment .text=16384 \
        --set-section-alignment .data=16384 \
        --set-section-alignment .rodata=16384 \
        --set-section-alignment .bss=16384 \
        /tmp/test_lib.so /tmp/test_lib_aligned.so

echo -e "\n--- AFTER objcopy ---"
readelf -Wl /tmp/test_lib_aligned.so | grep "LOAD" | head -2

# Check if alignment changed
echo -e "\n--- Alignment check ---"
python3 -c "
import sys
import struct

def check_alignment(path):
    with open(path, 'rb') as f:
        # Read ELF header
        f.seek(0)
        ei_class = f.read(5)[4]
        f.seek(0)
        
        if ei_class == 2:  # 64-bit
            f.seek(32)  # offset to e_phoff
            e_phoff = struct.unpack('<Q', f.read(8))[0]
            f.seek(54)  # offset to e_phentsize and e_phnum
            e_phentsize = struct.unpack('<H', f.read(2))[0]
            e_phnum = struct.unpack('<H', f.read(2))[0]
        else:  # 32-bit
            f.seek(28)
            e_phoff = struct.unpack('<I', f.read(4))[0]
            f.seek(42)
            e_phentsize = struct.unpack('<H', f.read(2))[0]
            e_phnum = struct.unpack('<H', f.read(2))[0]
        
        # Read program headers
        for i in range(e_phnum):
            f.seek(e_phoff + i * e_phentsize)
            if ei_class == 2:
                p_type = struct.unpack('<I', f.read(4))[0]
                f.read(4)  # p_flags
                f.read(8)  # p_offset
                f.read(8)  # p_vaddr
                f.read(8)  # p_paddr
                f.read(8)  # p_filesz
                f.read(8)  # p_memsz
                p_align = struct.unpack('<Q', f.read(8))[0]
            else:
                p_type = struct.unpack('<I', f.read(4))[0]
                f.read(4)  # p_offset
                f.read(4)  # p_vaddr
                f.read(4)  # p_paddr
                f.read(4)  # p_filesz
                f.read(4)  # p_memsz
                f.read(4)  # p_flags
                p_align = struct.unpack('<I', f.read(4))[0]
            
            if p_type == 1:  # PT_LOAD
                if p_align >= 16384:
                    return 'ALIGNED (2**14)'
                elif p_align >= 4096:
                    return 'UNALIGNED (2**12)'
                else:
                    return f'UNALIGNED (2**{(p_align-1).bit_length()})'
    return 'UNKNOWN'

print(f'Original: {check_alignment(\"/tmp/test_lib.so\")}')
print(f'Realigned: {check_alignment(\"/tmp/test_lib_aligned.so\")}')
"

echo -e "\n=== Test complete ==="
