#!/usr/bin/env python3
"""Derive Honda cluster packets from the saved OEM XML. Offline research only.

No device I/O; no assumption that an XP box accepts these packets. Source/menu
support and DA identification are runtime inputs on the original head unit.
"""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
XML = ROOT / 'documents/research/honda-cluster-analysis/vehicle_customize_config.xml'
FEATURES = {
    0x28: 'Ambient color change',
    0x29: 'Ambient color selection variant 1',
    0x2A: 'Ambient color selection variant 2',
    0x38: 'Turn-by-turn display',
    0x5B: 'Meter configuration',
    0x5C: 'Tachometer display',
}


def routes(item):
    return {int(e.get('key'), 0): int(e.text, 0)
            for e in item.findall('InfoFrom/entry')}


def derive(setting_id, api_value, source, sender, xml=XML):
    """Return a conditional OEM packet for an explicitly selected scalar value.

    Refuse shared menus: they need real sibling values, never XML DataValue
    defaults. This utility intentionally does not emulate capability discovery.
    """
    if sender not in (0x54, 0x55):
        raise ValueError('Sender must be explicitly DA (0x54) or DA+ (0x55)')
    items = ET.parse(xml).getroot().findall('Item')
    target = next((i for i in items if int(i.findtext('Category'), 0) == 3
                   and int(i.findtext('Id'), 0) == setting_id), None)
    if target is None or source == 0 or source not in routes(target):
        raise ValueError('No direct OEM route for this setting/source')
    menu = routes(target)[source]
    peers = [i for i in items if routes(i).get(source) == menu]
    if len(peers) != 1:
        raise ValueError('Shared menu requires real current sibling values')
    if int(target.findtext('DataType'), 0) != 0:
        raise ValueError('Only enumerated scalar values are implemented')
    choices = {int(e.get('key'), 0): int(e.text, 0)
               for e in target.findall('DataList/entry')}
    if api_value not in choices:
        raise ValueError('API value is absent from the OEM value map')
    encoded = choices[api_value]
    if not all(0 <= b <= 255 for b in (source, menu, encoded)):
        raise ValueError('Route/value is not a byte')
    payload = bytes((0x40, menu, encoded))
    address = 0x16300000 + (source << 8) + sender
    native = bytes((0, 0, 0, len(payload))) + address.to_bytes(4, 'big')
    native += payload.ljust(8, b'\0')  # OEM transport storage, not vehicle values.
    body = bytes.fromhex('51 07 44 00') + native
    length = (len(body) + 1).to_bytes(2, 'big')
    ipc = b'\x02' + length + bytes(b ^ 255 for b in length) + body + b'\x03'
    return {
        'category': '0x03', 'setting_id': f'0x{setting_id:02X}',
        'api_value': api_value, 'encoded_value': encoded,
        'source': f'0x{source:02X}', 'menu': f'0x{menu:02X}',
        'sender': f'0x{sender:02X}', 'oem_bcan_id': f'0x{address:08X}',
        'oem_format': 0, 'oem_data_length': len(payload),
        'oem_payload': payload.hex(' ').upper(),
        'oem_native': native.hex(' ').upper(),
        'oem_syscon_ipc': ipc.hex(' ').upper(),
        'xp_passthrough_packet': None, 'hardware_verified': False,
    }


def catalog():
    items = ET.parse(XML).getroot().findall('Item')
    output = []
    for ident, label in FEATURES.items():
        item = next(i for i in items if int(i.findtext('Category'), 0) == 3
                    and int(i.findtext('Id'), 0) == ident)
        cases = [derive(ident, int(e.get('key'), 0), 0x50, sender)
                 for sender in (0x54, 0x55)
                 for e in item.findall('DataList/entry')]
        output.append({'feature': label, 'cases': cases})
    return {
        'status': 'conditional OEM reconstruction; XP passthrough unresolved',
        'xml_sha256': hashlib.sha256(XML.read_bytes()).hexdigest(),
        'requirements': ['live source/menu capability', 'actual DA/DA+ identity',
                         'verified XP encoder and matching response mapping'],
        'features': output,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = json.dumps(catalog(), indent=2) + '\n'
    if args.output:
        args.output.write_text(result)
    else:
        print(result, end='')


if __name__ == '__main__':
    main()
