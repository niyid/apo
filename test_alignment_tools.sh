#!/bin/bash

echo "=== Testing alignment tools ==="

# Find NDK path
NDK_PATH=$(find /mnt/android-sdk/ndk -type d -name "toolchains" 2>/dev/null | head -1)
if [ -n "$NDK_PATH" ]; then
    NDK_PATH=$(dirname "$NDK_PATH")
fi

if [ -z "$NDK_PATH" ]; then
    echo "Error: Could not find NDK. Please locate your NDK installation."
    exit 1
fi

echo "Found NDK at: $NDK_PATH"

# Find llvm-objcopy
LLVM_OBJCOPY=$(find "$NDK_PATH" -name "llvm-objcopy" -type f 2>/dev/null | head -1)
if [ -n "$LLVM_OBJCOPY" ]; then
    echo "Found llvm-objcopy at: $LLVM_OBJCOPY"
fi

# Find test library
TEST_LIB=$(find app/build/intermediates/stripped_native_libs -name "libbarhopper_v3.so" -type f | head -1)
if [ -z "$TEST_LIB" ]; then
    echo "Error: No test library found. Run a build first."
    exit 1
fi

echo "Testing with: $TEST_LIB"
cp "$TEST_LIB" /tmp/test_original.so

echo -e "\n--- Testing GNU objcopy (doesn't work) ---"
objcopy --set-section-alignment .text=16384 /tmp/test_original.so /tmp/test_gnu.so 2>&1
readelf -Wl /tmp/test_gnu.so | grep "LOAD" | head -1 | grep -o "0x[0-9a-f]*$"

echo -e "\n--- Testing llvm-objcopy ---"
if [ -n "$LLVM_OBJCOPY" ] && [ -f "$LLVM_OBJCOPY" ]; then
    echo "Testing: $LLVM_OBJCOPY"
    $LLVM_OBJCOPY --set-section-alignment .text=16384 /tmp/test_original.so /tmp/test_llvm.so 2>&1
    echo "Result:"
    readelf -Wl /tmp/test_llvm.so | grep "LOAD" | head -1
else
    echo "llvm-objcopy not found"
fi

echo -e "\n--- Testing align_elf_segments.py approach ---"
cat > /tmp/align_elf.py << 'EOF'
#!/usr/bin/env python3
import sys
import struct

def align_elf(input_file, output_file, alignment=16384):
    with open(input_file, 'rb') as f:
        data = bytearray(f.read())
    
    # Check ELF magic
    if data[0:4] != b'\x7fELF':
        print("Not an ELF file")
        return False
    
    ei_class = data[4]  # 1=32-bit, 2=64-bit
    
    if ei_class == 2:  # 64-bit
        e_phoff = struct.unpack('<Q', data[32:40])[0]
        e_phentsize = struct.unpack('<H', data[54:56])[0]
        e_phnum = struct.unpack('<H', data[56:58])[0]
        
        for i in range(e_phnum):
            offset = e_phoff + i * e_phentsize
            p_type = struct.unpack('<I', data[offset:offset+4])[0]
            
            if p_type == 1:  # PT_LOAD
                # Change p_align at offset+48
                struct.pack_into('<Q', data, offset + 48, alignment)
                print(f"Updated LOAD segment {i} to alignment {alignment}")
    else:  # 32-bit
        e_phoff = struct.unpack('<I', data[28:32])[0]
        e_phentsize = struct.unpack('<H', data[42:44])[0]
        e_phnum = struct.unpack('<H', data[44:46])[0]
        
        for i in range(e_phnum):
            offset = e_phoff + i * e_phentsize
            p_type = struct.unpack('<I', data[offset:offset+4])[0]
            
            if p_type == 1:  # PT_LOAD
                # Change p_align at offset+28
                struct.pack_into('<I', data, offset + 28, alignment)
                print(f"Updated LOAD segment {i} to alignment {alignment}")
    
    with open(output_file, 'wb') as f:
        f.write(data)
    
    return True

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: align_elf.py <input.so> <output.so>")
        sys.exit(1)
    
    align_elf(sys.argv[1], sys.argv[2])
EOF

chmod +x /tmp/align_elf.py
python3 /tmp/align_elf.py /tmp/test_original.so /tmp/test_python.so
readelf -Wl /tmp/test_python.so | grep "LOAD" | head -1 | grep -o "0x[0-9a-f]*$"

echo -e "\n=== Summary ==="
echo "Original alignment:"
readelf -Wl /tmp/test_original.so | grep "LOAD" | head -1
echo -e "\nPython script alignment:"
readelf -Wl /tmp/test_python.so | grep "LOAD" | head -1
