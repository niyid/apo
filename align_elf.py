#!/usr/bin/env python3
"""
ELF alignment tool for Android 16KB page size support.
Modifies ELF program headers to set LOAD segment alignment to 16384 bytes.
"""
import sys
import struct
import os

def align_elf(input_file, output_file, alignment=16384):
    """Align ELF LOAD segments to specified alignment."""
    try:
        with open(input_file, 'rb') as f:
            data = bytearray(f.read())
    except IOError as e:
        print(f"Error reading {input_file}: {e}", file=sys.stderr)
        return False
    
    # Check ELF magic
    if data[0:4] != b'\x7fELF':
        print(f"Error: {input_file} is not an ELF file", file=sys.stderr)
        return False
    
    ei_class = data[4]  # 1=32-bit, 2=64-bit
    
    segments_updated = 0
    
    if ei_class == 2:  # 64-bit
        e_phoff = struct.unpack('<Q', data[32:40])[0]
        e_phentsize = struct.unpack('<H', data[54:56])[0]
        e_phnum = struct.unpack('<H', data[56:58])[0]
        
        for i in range(e_phnum):
            offset = e_phoff + i * e_phentsize
            p_type = struct.unpack('<I', data[offset:offset+4])[0]
            
            if p_type == 1:  # PT_LOAD
                # Read current p_align
                current_align = struct.unpack('<Q', data[offset+48:offset+56])[0]
                
                # Update p_align at offset+48
                struct.pack_into('<Q', data, offset + 48, alignment)
                segments_updated += 1
                
    elif ei_class == 1:  # 32-bit
        e_phoff = struct.unpack('<I', data[28:32])[0]
        e_phentsize = struct.unpack('<H', data[42:44])[0]
        e_phnum = struct.unpack('<H', data[44:46])[0]
        
        for i in range(e_phnum):
            offset = e_phoff + i * e_phentsize
            p_type = struct.unpack('<I', data[offset:offset+4])[0]
            
            if p_type == 1:  # PT_LOAD
                # Read current p_align
                current_align = struct.unpack('<I', data[offset+28:offset+32])[0]
                
                # Update p_align at offset+28
                struct.pack_into('<I', data, offset + 28, alignment)
                segments_updated += 1
    else:
        print(f"Error: Unknown ELF class: {ei_class}", file=sys.stderr)
        return False
    
    try:
        with open(output_file, 'wb') as f:
            f.write(data)
    except IOError as e:
        print(f"Error writing {output_file}: {e}", file=sys.stderr)
        return False
    
    return True

def main():
    if len(sys.argv) != 3:
        print("Usage: align_elf.py <input.so> <output.so>")
        print("Aligns ELF LOAD segments to 16KB for Android 16KB page size support")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    if not os.path.exists(input_file):
        print(f"Error: Input file does not exist: {input_file}", file=sys.stderr)
        sys.exit(1)
    
    if align_elf(input_file, output_file):
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == '__main__':
    main()
