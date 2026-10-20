#!/usr/bin/env python3
"""Generate placeholder launcher icons for the Android app."""
import struct, zlib

def create_png(width, height, color_rgb):
    def create_chunk(chunk_type, data):
        chunk = chunk_type + data
        crc = zlib.crc32(chunk) & 0xffffffff
        return struct.pack('>I', len(data)) + chunk + struct.pack('>I', crc)
    signature = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr = create_chunk(b'IHDR', ihdr_data)
    raw_data = b''
    for y in range(height):
        raw_data += b'\x00'
        for x in range(width):
            cx, cy = width // 2, height // 2
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            radius = min(width, height) * 0.35
            if dist < radius:
                r, g, b = 0, 212, 255
            else:
                r, g, b = color_rgb
            raw_data += struct.pack('BBB', r, g, b)
    compressed = zlib.compress(raw_data, 9)
    idat = create_chunk(b'IDAT', compressed)
    iend = create_chunk(b'IEND', b'')
    return signature + ihdr + idat + iend

sizes = {'mipmap-mdpi': 48, 'mipmap-hdpi': 72, 'mipmap-xhdpi': 96, 'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192}
bg_color = (15, 52, 96)
for folder, size in sizes.items():
    png_data = create_png(size, size, bg_color)
    for name in ['ic_launcher.png', 'ic_launcher_round.png']:
        path = f'app/src/main/res/{folder}/{name}'
        with open(path, 'wb') as f:
            f.write(png_data)
        print(f'Created {path} ({size}x{size})')
print('Done!')
