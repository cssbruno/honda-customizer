#!/usr/bin/env python3
"""Build the experimental HCX/111 bench image. Never opens or flashes a device."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess

from audit_xp_box_firmware import validate, FIRMWARE_SHA256

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'firmware/cluster-extension'
BASE, APPEND = 0x08000000, 0x16400
HOOKS = (
    (0x151D6, 0x15260, 'hcx_boot'),
    (0x0E9C6, 0x133C0, 'hcx_dispatch'),
    (0x15256, 0x0E74A, 'hcx_main_tick'),
    (0x0CC68, 0x110E8, 'hcx_transmit'),
    (0x0CA9C, 0x1125E, 'hcx_receive'),
)

def digest(b):
    return hashlib.sha256(b).hexdigest()

def branch_link(source, target):
    delta = (target & ~1) - (source + 4)
    if delta & 1 or not -(1 << 24) <= delta < (1 << 24):
        raise ValueError('Thumb BL target is out of range or unaligned')
    s, i1, i2 = (delta >> 24) & 1, (delta >> 23) & 1, (delta >> 22) & 1
    first = 0xF000 | (s << 10) | ((delta >> 12) & 0x3FF)
    second = 0xD000 | (((~i1 ^ s) & 1) << 13) | (((~i2 ^ s) & 1) << 11) | ((delta >> 1) & 0x7FF)
    return struct.pack('<HH', first, second)

def build(firmware, toolchain, output):
    binary = firmware.read_bytes()
    validate(binary)
    if len(binary) > APPEND:
        raise ValueError('Extension would overwrite manufacturer bytes')
    for offset, target, _ in HOOKS:
        if binary[offset:offset+4] != branch_link(BASE+offset, BASE+target):
            raise ValueError(f'Original call-site mismatch: {offset:#x}')
    output.mkdir(parents=True, exist_ok=True)
    elf, extension = output/'hcx.elf', output/'hcx-extension.bin'
    command = [str(toolchain/'clang'), '--target=arm-none-eabi', '-mcpu=cortex-m3', '-mthumb',
               '-Oz', '-ffreestanding', '-fno-builtin', '-fno-unwind-tables',
               '-fno-asynchronous-unwind-tables', '-nostdlib', '-Wall', '-Wextra', '-Werror',
               '-Wl,--build-id=none', f'-Wl,-T,{SOURCE/"bench.ld"}',
               f'-Wl,-Map,{output/"hcx.map"}', str(SOURCE/'hcx.c'), str(SOURCE/'port_111.c'),
               '-o', str(elf)]
    subprocess.run(command, check=True)
    subprocess.run([str(toolchain/'llvm-objcopy'), '-O', 'binary', str(elf), str(extension)], check=True)
    symbols = {}
    nm = subprocess.check_output([str(toolchain/'llvm-nm'), '--defined-only', str(elf)], text=True)
    for line in nm.splitlines():
        parts = line.split()
        if len(parts)==3 and parts[1].lower()=='t':
            symbols[parts[2]] = int(parts[0], 16) & ~1
    image = bytearray(binary.ljust(APPEND, b'\xFF') + extension.read_bytes())
    patches = []
    for offset, target, name in HOOKS:
        replacement = branch_link(BASE+offset, symbols[name])
        patches.append({'offset': hex(offset), 'original_target': hex(BASE+target),
                        'original': binary[offset:offset+4].hex(),
                        'replacement': replacement.hex(), 'symbol': name})
        image[offset:offset+4] = replacement
    changed = {i for i in range(len(binary)) if image[i] != binary[i]}
    permitted = {i for offset, _, _ in HOOKS for i in range(offset, offset+4)}
    if not changed <= permitted or image[:0x4000] != binary[:0x4000]:
        raise AssertionError('Unexpected changes outside the five application hooks')
    name = 'HDSS06C-111-HCX1.experimental-bench.bin'
    (output/name).write_bytes(image)
    manifest = {
        'format': 'Experimental raw memory image, NOT an XP updater container',
        'installable': False, 'hardware_verified': False, 'base_version': 'CRI V1.13.111BYPT',
        'installed_108_compatibility': 'Unverified; never patch or flash a different binary',
        'original_sha256': FIRMWARE_SHA256, 'image_file': name, 'image_sha256': digest(image),
        'extension_sha256': digest(extension.read_bytes()), 'symbols': symbols, 'hooks': patches,
        'ram_reservation': '0x20004E80..0x20004EFF: bench assumption, not proven free on hardware',
        'clock': 'Vendor counter at 0x20004BEC; duration in seconds not established',
        'missing_for_installation': ['Exact board and bootloader identification plus recoverable backup',
            'Physical RAM/stack and flash capacity validation', 'Vendor update integrity/container format',
            'Real CAN TX/RX and FYT reply routing validation'],
        'compiler': subprocess.check_output([str(toolchain/'clang'), '--version'], text=True).splitlines()[0],
        'source_sha256': {p.name: digest(p.read_bytes()) for p in sorted(SOURCE.iterdir()) if p.suffix in ('.c','.h','.ld')},
    }
    (output/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    return manifest

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--firmware', type=Path, required=True)
    parser.add_argument('--toolchain-dir', type=Path, required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    args=parser.parse_args()
    print(json.dumps(build(args.firmware, args.toolchain_dir, args.output_dir),indent=2))
