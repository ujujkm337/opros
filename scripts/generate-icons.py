#!/usr/bin/env python3
"""
Generate placeholder launcher icons for the Android app.
This creates simple colored PNG files for each density.
"""

import struct
import zlib

def create_png(width, height, color_rgb):
    """Create a minimal PNG file with a solid color."""
    def create_chunk(chunk_type, data):
        chunk = chunk_type + data
        crc = zlib.crc32(chunk) & 0xffffffff
        return struct.pack('>I', len(data)) + chunk + struct.pack('>I', crc)
    
    # PNG signature
    signature = b'\x89PNG\r\n\x1a\n'
    
    # IHDR chunk (image header)
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr = create_chunk(b'IHDR', ihdr_data)
    
    # IDAT chunk (image data)
    raw_data = b''
    for y in range(height):
        raw_data += b'\x00'  # filter type: none
        for x in range(width):
            # Create a simple gradient/circle pattern
            cx, cy = width // 2, height // 2
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            radius = min(width, height) * 0.35
            
            if dist < radius:
                # Inside circle - use accent color
                r, g, b = 0, 212, 255
            else:
                # Outside circle - use background color
                r, g, b = color_rgb
            
            raw_data += struct.pack('BBB', r, g, b)
    
    compressed = zlib.compress(raw_data, 9)
    idat = create_chunk(b'IDAT', compressed)
    
    # IEND chunk (image end)
    iend = create_chunk(b'IEND', b'')
    
    return signature + ihdr + idat + iend

# Define sizes for each density
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

# Background color (dark blue)
bg_color = (15, 52, 96)

# Generate icons for each density
for folder, size in sizes.items():
    png_data = create_png(size, size, bg_color)
    
    # Write ic_launcher.png
    path = f'app/src/main/res/{folder}/ic_launcher.png'
    with open(path, 'wb') as f:
        f.write(png_data)
    print(f'Created {path} ({size}x{size})')
    
    # Write ic_launcher_round.png (same for now)
    path_round = f'app/src/main/res/{folder}/ic_launcher_round.png'
    with open(path_round, 'wb') as f:
        f.write(png_data)
    print(f'Created {path_round} ({size}x{size})')

print('\nAll launcher icons created!')
