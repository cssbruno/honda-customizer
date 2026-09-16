#!/usr/bin/env python3
"""Audit one manufacturer's Honda XP firmware offline. Never opens a device.

Requires Unicorn 2.1.4 for isolated ARM execution. The manufacturer binary is
an explicit input, is not redistributed, and must match the audited SHA-256.
Neither this harness nor its RAM fixtures belong in the Android APK.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

from trace_cluster_transport import derive

FIRMWARE_SHA256 = 'df254fb42d5fe02e985ab57f9d23fcce963fa70868e337eb9941a99c8196bf99'
FIRMWARE_VERSION = 'CRI V1.13.111BYPT'
FIRMWARE_URL = 'http://www.ssinterface.com/upload/file/20230825/1692940642406248.zip'
FLASH, RAM = 0x08000000, 0x20000000
STATE, RX, STOP = RAM + 0x438C, RAM + 0x1310, FLASH + 0x1F000
RX_BODY = RX + 0x34
TABLE_OFFSET, TABLE_COUNT = 0x14E78, 35


def validate(binary):
    if hashlib.sha256(binary).hexdigest() != FIRMWARE_SHA256:
        raise ValueError('Unknown firmware: addresses are valid only for the audited 111 BIN')
    if binary[0x40FC:0x40FC + len(FIRMWARE_VERSION)] != FIRMWARE_VERSION.encode():
        raise ValueError('Unexpected firmware identification')


def templates(binary):
    """Extract the fixed 20-byte templates referenced by 08004896.

    Entries include special actions. An index alone is not a usable XP command;
    the dispatcher must select that entry and translate the input value.
    """
    validate(binary)
    result = []
    for index in range(TABLE_COUNT):
        offset = TABLE_OFFSET + index * 20
        entry = binary[offset:offset + 20]
        result.append({
            'index': index, 'file_offset': f'0x{offset:05X}',
            'can_id': f'0x{struct.unpack_from("<I", entry, 4)[0]:08X}',
            'ide': entry[8], 'rtr': entry[9], 'dlc': entry[10],
            'data_template': entry[11:11 + entry[10]].hex(' ').upper(),
        })
    return result


class IsolatedFirmware:
    def __init__(self, binary):
        validate(binary)
        from unicorn import Uc, UC_ARCH_ARM, UC_MODE_THUMB, UC_MODE_MCLASS, UC_HOOK_CODE
        from unicorn import arm_const
        self.registers = arm_const
        self.vm = Uc(UC_ARCH_ARM, UC_MODE_THUMB | UC_MODE_MCLASS)
        self.vm.mem_map(FLASH, 0x20000)
        self.vm.mem_write(FLASH, binary)
        self.vm.mem_map(RAM, 0x10000)
        self.frames, self.ack_calls, self.serial = [], 0, []
        self.receiver_notifications = 0
        self.vm.hook_add(UC_HOOK_CODE, self.intercept)

    def return_to_caller(self):
        r = self.registers
        self.vm.reg_write(r.UC_ARM_REG_PC, self.vm.reg_read(r.UC_ARM_REG_LR))

    def intercept(self, vm, address, size, unused):
        r = self.registers
        if address == 0x0800E92C:
            # Decoder acknowledgement routine; it is NOT a vehicle response.
            self.ack_calls += 1
            self.return_to_caller()
        elif address == 0x08004814:
            # Capture the CAN queue input before any queue/peripheral operation.
            entry = bytes(vm.mem_read(vm.reg_read(r.UC_ARM_REG_R0), 20))
            if entry[10] > 8:
                raise ValueError('Invalid CAN DLC in firmware output')
            self.frames.append({
                'can_id': f'0x{struct.unpack_from("<I", entry, 4)[0]:08X}',
                'ide': entry[8], 'rtr': entry[9], 'dlc': entry[10],
                'payload': entry[11:11 + entry[10]].hex(' ').upper(),
            })
            self.return_to_caller()
        elif address == 0x08012502:
            length = vm.reg_read(r.UC_ARM_REG_R1)
            if length > 64:
                raise ValueError('Unexpected serial output length')
            self.serial.append(bytes(vm.mem_read(vm.reg_read(r.UC_ARM_REG_R0), length)).hex(' ').upper())
            self.return_to_caller()
        elif address == 0x08012382:
            # Post to "Uart Send Sem". The harness explicitly runs E9E8 below;
            # no RTOS task, peripheral interrupt or scheduling is simulated.
            self.receiver_notifications += 1
            self.return_to_caller()

    def call(self, address, arg0=0):
        r = self.registers
        self.vm.reg_write(r.UC_ARM_REG_SP, RAM + 0xF000)
        self.vm.reg_write(r.UC_ARM_REG_LR, STOP | 1)
        self.vm.reg_write(r.UC_ARM_REG_R0, arg0)
        self.vm.emu_start(address | 1, STOP, count=10000)
        if self.vm.reg_read(r.UC_ARM_REG_PC) != STOP:
            raise RuntimeError('Firmware execution did not return within the instruction limit')

    def c6(self, key, value, corrupt_checksum=False):
        # These are explicit test preconditions, not readings or compatibility
        # discovery: the settings state machine is ready, its table is available.
        self.vm.mem_write(STATE + 0x13, b'\x01')
        self.vm.mem_write(STATE + 0x8C, b'\x01')
        body = bytes((0xC6, 2, key, value))
        checksum = (sum(body) ^ 0xFF) & 0xFF
        if corrupt_checksum:
            checksum ^= 1
        self.vm.mem_write(RX_BODY, body + bytes((checksum,)))
        self.call(0x0800E948)  # Actual checksum validation and command dispatcher.
        self.call(0x08006B48)  # One actual settings state-machine step.
        return {
            'decoder_packet': body.hex(' ').upper(),
            'decoder_ack_routine_calls': self.ack_calls,
            'captured_can_queue_inputs': self.frames,
            'captured_serial_outputs': self.serial,
            'hardware_verified': False,
        }

    def receive_serial(self, caller_body, corrupt_checksum=False):
        """Exercise EA24 with g0/r.f's framing of literal caller bytes.

        E9 is an outer routing selector, not part of the box's 2E stream.
        Neither SYU nor this function invents a CAN command or replaces a
        caller's length byte. In particular, OEM structures are NOT XP packets.
        """
        self.vm.mem_write(STATE + 0x13, b'\x01')
        self.vm.mem_write(STATE + 0x8C, b'\x01')
        checksum = (sum(caller_body) ^ 0xFF) & 0xFF
        if corrupt_checksum:
            checksum ^= 1
        stream = b'\x2E' + caller_body + bytes((checksum,))
        for byte in stream:
            self.call(0x0800EA24, byte)
            self.call(0x0800E9E8)
        self.call(0x08006B48)
        return {
            'literal_caller_body': caller_body.hex(' ').upper(),
            'serial_input': stream.hex(' ').upper(),
            'receiver_notifications': self.receiver_notifications,
            'receiver_state': self.vm.mem_read(RX + 0x68, 1)[0],
            'receiver_buffered_bytes': self.vm.mem_read(RX + 0x69, 1)[0],
            'decoder_ack_routine_calls': self.ack_calls,
            'captured_can_queue_inputs': self.frames,
            'captured_serial_outputs': self.serial,
            'hardware_verified': False,
        }


def serial_audit(binary):
    """Compare a stock XP command and literal OEM representations at RX.

    Explicit test input: OEM API value 1, source 50, sender 54. This does not
    discover installed source/menu support, identity or physical compatibility.
    """
    known = IsolatedFirmware(binary).receive_serial(bytes.fromhex('C6 02 16 01'))
    expected = {'can_id': '0x16305054', 'ide': 4, 'rtr': 0,
                'dlc': 3, 'payload': '40 2E 00'}
    if (known['captured_can_queue_inputs'] != [expected]
            or known['decoder_ack_routine_calls'] != 1):
        raise AssertionError('Known XP serial packet did not reach the expected CAN queue input')
    invalid = IsolatedFirmware(binary).receive_serial(
        bytes.fromhex('C6 02 16 01'), corrupt_checksum=True)
    if (invalid['captured_can_queue_inputs'] or invalid['decoder_ack_routine_calls']
            or invalid['captured_serial_outputs'] != ['F0']):
        raise AssertionError('Corrupted serial packet was not rejected before dispatch')
    cases = []
    for setting in (0x28, 0x29, 0x2A, 0x38, 0x5B, 0x5C):
        oem = derive(setting, 1, 0x50, 0x54)
        for representation in ('oem_payload', 'oem_native', 'oem_syscon_ipc'):
            case = IsolatedFirmware(binary).receive_serial(bytes.fromhex(oem[representation]))
            if case['captured_can_queue_inputs'] or case['decoder_ack_routine_calls']:
                raise AssertionError('A literal OEM representation unexpectedly dispatched a CAN command')
            case.update({'oem_setting': f'03/{setting:02X}',
                         'representation': representation,
                         'oem_can_id': oem['oem_bcan_id'],
                         'oem_api_value': oem['api_value'],
                         'oem_payload': oem['oem_payload']})
            cases.append(case)
    return {
        'scope': 'Actual byte receiver, pending-message processing and one ready-state settings step; RTOS notification intercepted',
        'framing_reference': 'SYU 2.23.0711.1001 g0/r.f: 2E + caller body + complemented sum; outer E9 removed',
        'known_xp_case': known, 'invalid_checksum_case': invalid,
        'literal_oem_cases': cases,
        'conclusion': 'Literal OEM payload, native and syscon representations do not substitute for an XP CAN transmit contract on the tested 111 path',
    }


def audit(binary):
    table = templates(binary)
    cases = []
    # Values are translated by the firmware, then compared with independently
    # reconstructed OEM XML packets. No replacement decoder dialect is used.
    for key, setting in ((0x01, 0x28), (0x13, 0x28), (0x16, 0x5C), (0x23, 0x5C)):
        for value in (0, 1):
            case = IsolatedFirmware(binary).c6(key, value)
            oem = derive(setting, 2 - value, 0x50, 0x54)
            expected = {'can_id': oem['oem_bcan_id'], 'ide': 4, 'rtr': 0,
                        'dlc': 3, 'payload': oem['oem_payload']}
            if case['captured_can_queue_inputs'] != [expected] or case['decoder_ack_routine_calls'] != 1:
                raise AssertionError('Firmware output differs from independent OEM reconstruction')
            case['matches_oem_setting'] = f'03/{setting:02X}'
            cases.append(case)
    # Counterexamples to inserting an OEM menu directly into C6's key slot.
    for key in (0x15, 0x20, 0x32, 0x33):
        for value in (0, 1):
            case = IsolatedFirmware(binary).c6(key, value)
            expected = ([{'can_id': '0x16305054', 'ide': 4, 'rtr': 0,
                          'dlc': 3, 'payload': f'40 2F {value:02X}'}] if key == 0x15 else [])
            if case['captured_can_queue_inputs'] != expected or case['decoder_ack_routine_calls'] != 1:
                raise AssertionError('Unexpected result for direct-menu counterexample')
            cases.append(case)
    invalid = IsolatedFirmware(binary).c6(0x16, 1, corrupt_checksum=True)
    if invalid['decoder_ack_routine_calls'] or invalid['captured_can_queue_inputs'] or invalid['captured_serial_outputs'] != ['F0']:
        raise AssertionError('Invalid checksum was not rejected before dispatch')
    missing = []
    for setting in (0x29, 0x2A, 0x38, 0x5B):
        oem = derive(setting, 1, 0x50, 0x54)
        menu = int(oem['menu'], 16)
        matches = [t for t in table if t['can_id'] == oem['oem_bcan_id']
                   and t['dlc'] == 3 and bytes.fromhex(t['data_template'])[1] == menu]
        if matches:
            raise AssertionError('Previously missing menu now has a template')
        missing.append({'oem_setting': f'03/{setting:02X}', 'menu': oem['menu'],
                        'matching_fixed_templates': []})
    return {
        'firmware_version': FIRMWARE_VERSION, 'firmware_sha256': FIRMWARE_SHA256,
        'manufacturer_archive_url': FIRMWARE_URL,
        'physical_hardware_tested': False,
        'scope': 'Isolated 111 firmware receiver and one ready-state settings step; no peripherals, RTOS, boot or complete vehicle simulation',
        'fixture_preconditions': {
            '0x2000439F': '01: settings state machine ready',
            '0x20004418': '01: settings table available',
            'other_mapped_ram': 'Zero-initialized harness memory; not captured vehicle state',
            'hardware_register_parser_selection': 'Not executed; explicitly exercise the normal 2E receiver',
        },
        'installed_108_firmware_compatibility': 'Not established by this 111 binary',
        'fixed_templates': table, 'c6_cases': cases,
        'invalid_checksum_case': invalid,
        'serial_receiver_audit': serial_audit(binary),
        'extra_cluster_menus_absent_from_fixed_table': missing,
        'generic_can_passthrough_contract': None,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--firmware', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = json.dumps(audit(args.firmware.read_bytes()), indent=2) + '\n'
    if args.output:
        args.output.write_text(result)
    else:
        print(result, end='')


if __name__ == '__main__':
    main()
