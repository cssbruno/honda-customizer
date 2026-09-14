"""Reconstruct the reviewed Honda firmware's bytes. No device or network I/O.

These are conditional static examples, not validated vehicle/OBD commands.
Requires the live route 0x50/0x2E to exist; does not perform discovery.
"""
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def trace(enabled: bool, da_plus: bool) -> dict:
    root = ET.parse(Path(__file__).with_name("vehicle_customize_config.xml"))
    item = next(x for x in root.findall("Item")
                if int(x.findtext("Category"), 0) == 3
                and int(x.findtext("Id"), 0) == 0x5C)
    api_value = 1 if enabled else 2
    values = {int(e.get("key"), 0): int(e.text, 0)
              for e in item.findall("DataList/entry")}
    routes = {int(e.get("key"), 0): int(e.text, 0)
              for e in item.findall("InfoFrom/entry")}
    menu = routes[0x50]
    payload = bytes([0x40, menu, values[api_value]])
    bcan_id = 0x16305055 if da_plus else 0x16305054
    # JNI struct: method, cycle time (BE16), length, format|ID (BE32), data[8].
    native = bytes([0, 0, 0, len(payload)]) + bcan_id.to_bytes(4, "big")
    native += payload.ljust(8, b"\0")
    body = bytes.fromhex("51 07 44 00") + native
    # send_syscon_message: STX, BE16(body length + ETX), complements, body, ETX.
    length = (len(body) + 1).to_bytes(2, "big")
    ipc = b"\x02" + length + bytes(v ^ 0xFF for v in length) + body + b"\x03"
    return {
        "status": "static reconstruction; not verified on a vehicle",
        "setting": "0x03/0x5C", "enabled": enabled, "da_plus": da_plus,
        "api_value": api_value, "encoded_value": values[api_value],
        "bcan_id": f"0x{bcan_id:08X}", "bcan_length": len(payload),
        "bcan_payload": payload.hex(" "), "native_struct": native.hex(" "),
        "syscon_body": body.hex(" "), "syscon_ipc": ipc.hex(" "),
    }


if __name__ == "__main__":
    print(json.dumps([trace(on, plus) for plus in (False, True)
                      for on in (True, False)], indent=2))
