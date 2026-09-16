#!/usr/bin/env python3
"""Exercise HCX's five actual ARM call-site patches offline, never a device.

Explicit fixtures replace CAN queue/peripherals, tick delivery and the RTOS
notification. This tests control flow, not boot, hardware timing or compatibility.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

from audit_xp_box_firmware import IsolatedFirmware, FLASH, RAM, validate
from build_hcx_bench import HOOKS, APPEND, branch_link
from trace_cluster_transport import derive

def packet(feature=0, api=0, transaction=1, op=1):
    return bytes((0xD7,11,0x48,0x43,0x58,1,op))+transaction.to_bytes(4,'big')+bytes((feature,api))

def frame(address, payload):
    return struct.pack('<IIBBB',0,address,4,0,len(payload))+payload.ljust(8,b'\0')+b'\0'

class Bench(IsolatedFirmware):
    def __init__(self, original, image):
        super().__init__(original)
        self.vm.mem_write(FLASH,image)
        self.mailbox=0
        self.hardware_transmissions=[]
        self.injected_reply=None
        self.original_tick_calls=0
        self.original_dispatch_calls=0
        # Explicit writable ring-slot fixture for the audited vendor queue guard.
        self.vm.mem_write(RAM+0x20BC,struct.pack('<I',RAM+0x6000))
        self.site(0x151D6)
    def intercept(self,vm,address,size,unused):
        r=self.registers
        if address==0x080110E8:
            ptr=vm.reg_read(r.UC_ARM_REG_R0)
            raw=bytes(vm.mem_read(ptr,20))
            self.hardware_transmissions.append(raw.hex(' ').upper())
            # Vendor routine mutates its input ID; reproduce this side effect.
            identifier=struct.unpack_from('<I',raw,4)[0]
            vm.mem_write(ptr+4,struct.pack('<I',(identifier<<3)&0xFFFFFFFF))
            vm.reg_write(r.UC_ARM_REG_R0,self.mailbox)
            self.return_to_caller()
        elif address==0x0801125E:
            if self.injected_reply is None:
                raise AssertionError('CAN receive fixture must be explicit')
            vm.mem_write(vm.reg_read(r.UC_ARM_REG_R1),self.injected_reply)
            self.return_to_caller()
        elif address==0x0800E74A:
            self.original_tick_calls+=1
            self.return_to_caller()
        else:
            if address==0x080133C0:
                self.original_dispatch_calls+=1
            super().intercept(vm,address,size,unused)
    def site(self,offset,arg0=0,arg1=0):
        r=self.registers
        saved=[r.UC_ARM_REG_R4,r.UC_ARM_REG_R5,r.UC_ARM_REG_R6,r.UC_ARM_REG_R7,
               r.UC_ARM_REG_R8,r.UC_ARM_REG_R9,r.UC_ARM_REG_R10,r.UC_ARM_REG_R11]
        for i,reg in enumerate(saved):
            self.vm.reg_write(reg,0x12340000+i)
        mask=self.vm.reg_read(r.UC_ARM_REG_PRIMASK)
        self.vm.reg_write(r.UC_ARM_REG_SP,RAM+0xF000)
        self.vm.reg_write(r.UC_ARM_REG_R0,arg0)
        self.vm.reg_write(r.UC_ARM_REG_R1,arg1)
        self.vm.emu_start((FLASH+offset)|1,FLASH+offset+4,count=20000)
        if self.vm.reg_read(r.UC_ARM_REG_PC)!=FLASH+offset+4:
            raise AssertionError('Patched call site did not return normally')
        assert self.vm.reg_read(r.UC_ARM_REG_SP)==RAM+0xF000
        assert self.vm.reg_read(r.UC_ARM_REG_PRIMASK)==mask
        for i,reg in enumerate(saved):
            assert self.vm.reg_read(reg)==0x12340000+i, 'Callee-saved register changed'
        return self.vm.reg_read(r.UC_ARM_REG_R0)
    def ticks(self,n):
        self.vm.mem_write(RAM+0x4BEC,struct.pack('<I',n))
    def transmit(self):
        f=self.frames[-1]
        self.vm.mem_write(RAM+0x6100,frame(int(f['can_id'],16),bytes.fromhex(f['payload'])))
        return self.site(0xCC68,RAM+0x6100)
    def receive(self,address,payload):
        self.injected_reply=frame(address,payload)
        self.site(0xCA9C,0,RAM+0x6200)
    def poll(self):
        self.site(0x15256)
    def reply(self):
        raw=bytes.fromhex(self.serial[-1])
        assert len(raw)==15 and raw[:3]==bytes((0x2E,0xD7,11)), raw.hex()
        assert raw[-1]==((sum(raw[1:-1])^0xFF)&0xFF)
        return raw[1:-1]

def audit(original,image,manifest):
    validate(original)
    assert hashlib.sha256(image).hexdigest()==manifest['image_sha256']
    assert manifest['installable'] is False and manifest['hardware_verified'] is False
    allowed=set()
    for offset,target,name in HOOKS:
        assert original[offset:offset+4]==branch_link(FLASH+offset,FLASH+target)
        assert image[offset:offset+4]==branch_link(FLASH+offset,manifest['symbols'][name])
        allowed.update(range(offset,offset+4))
    assert {i for i in range(len(original)) if original[i]!=image[i]} <= allowed
    assert image[len(original):APPEND]==b'\xFF'*(APPEND-len(original))
    cases=[]
    for feature,setting,count in ((1,0x5B,3),(2,0x38,2),(3,0x29,7),(4,0x2A,8)):
        for api in range(1,count+1):
            b=Bench(original,image)
            body=packet(feature,api)
            b.receive_serial(body)
            assert b.reply()[6]==0x11 and not b.original_dispatch_calls
            oem=derive(setting,api,0x50,0x54)
            expected={'can_id':oem['oem_bcan_id'],'ide':4,'rtr':0,'dlc':3,'payload':oem['oem_payload']}
            assert b.frames==[expected]
            response=bytes((0xC0,int(oem['menu'],16),oem['encoded_value']))
            b.receive(0x16305450,response)  # Correct bytes BEFORE transmit: ignored.
            b.poll(); assert b.reply()[6]==0x11
            assert b.transmit()==0
            b.poll(); assert b.reply()[6]==0x12
            b.receive(0x16305054,response)  # Wrong direction: ignored.
            b.poll(); assert b.reply()[6]==0x12
            b.receive(0x16305450,response)
            b.poll(); assert b.reply()[6]==0x13
            assert b.reply()[7:11]==(1).to_bytes(4,'big')
            assert len(b.hardware_transmissions)==1 and b.original_tick_calls==4
            cases.append({'feature':feature,'api_value':api,'caller_body':body.hex(' ').upper(),
                          'can_queue_input':b.frames[0],'serial_replies':b.serial,
                          'matching_reply':response.hex(' ').upper(),'hardware_verified':False})
    checks=[]
    b=Bench(original,image); b.receive_serial(packet(op=0))
    assert b.reply()[6]==0x10 and not b.frames
    checks.append('HELLO does not send CAN')
    b=Bench(original,image); b.receive_serial(packet(1,1),corrupt_checksum=True)
    assert b.serial==['F0'] and not b.frames
    checks.append('Original checksum rejection remains before custom dispatch')
    b=Bench(original,image); b.receive_serial(bytes.fromhex('C6 02 16 01'))
    assert b.frames==[{'can_id':'0x16305054','ide':4,'rtr':0,'dlc':3,'payload':'40 2E 00'}]
    assert b.original_dispatch_calls==1 and b.ack_calls==1
    checks.append('Stock C6 tachometer dispatch remains unchanged')
    b=Bench(original,image); bad=bytearray(packet(1,1)); bad[5]=2
    b.receive_serial(bytes(bad)); assert b.reply()[6]==0x16 and not b.frames
    checks.append('Unknown protocol revision rejected')
    b=Bench(original,image); b.receive_serial(packet(1,1)); b.ticks(800); b.poll()
    assert b.reply()[6]==0x14
    assert b.transmit()==4 and not b.hardware_transmissions
    checks.append('Expired queued frame suppressed before mailbox writer')
    b=Bench(original,image); b.receive_serial(packet(1,1)); b.mailbox=4
    assert b.transmit()==4
    b.poll(); assert b.reply()[6]==0x18
    b.receive(0x16305450,bytes.fromhex('C0 15 00')); b.poll(); assert b.reply()[6]==0x18
    checks.append('No free mailbox cannot be turned into success by a matching CAN frame')
    b=Bench(original,image); b.vm.mem_write(RAM+0x6004,b'\x01')
    b.receive_serial(packet(1,1)); assert b.reply()[6]==0x17 and not b.frames
    checks.append('Full vendor queue rejected before enqueue')
    b=Bench(original,image); b.receive_serial(packet(1,1)); b.transmit()
    b.ticks(800); b.receive(0x16305450,bytes.fromhex('C0 15 00')); b.poll()
    assert b.reply()[6]==0x14
    checks.append('Late matching reply remains timeout')
    b=Bench(original,image); b.receive_serial(packet(1,1)); b.transmit()
    b.receive_serial(packet(2,1,2)); assert b.reply()[6]==0x15 and len(b.frames)==1
    checks.append('Concurrent request rejected without second CAN write')
    b=Bench(original,image); b.receive_serial(packet(1,1))
    b.vm.reg_write(b.registers.UC_ARM_REG_PRIMASK,1)
    b.transmit(); b.poll()
    assert b.vm.reg_read(b.registers.UC_ARM_REG_PRIMASK)==1
    checks.append('Already-masked interrupt state, callee-saved registers and stack preserved')
    b=Bench(original,image); b.receive_serial(packet(1,1))
    b.vm.mem_write(RAM+0x6100,frame(0x16305054,bytes.fromhex('40 2E 00')))
    assert b.site(0xCC68,RAM+0x6100)==0
    b.poll(); assert b.reply()[6]==0x11 and len(b.hardware_transmissions)==1
    assert b.transmit()==0
    b.poll(); assert b.reply()[6]==0x12 and len(b.hardware_transmissions)==2
    checks.append('Unrelated stock transmit preserved without starting the custom request')
    return {'image_sha256':manifest['image_sha256'],'installable':False,'hardware_verified':False,
            'scope':'Five patched ARM call sites; explicit queue/UART/CAN/tick fixtures, no boot or physical hardware',
            'oem_value_cases':cases,'additional_checks':checks,
            'passed_cases':len(cases)+len(checks)}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--firmware',type=Path,required=True)
    parser.add_argument('--build-dir',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    manifest=json.loads((args.build_dir/'manifest.json').read_text())
    report=audit(args.firmware.read_bytes(),(args.build_dir/manifest['image_file']).read_bytes(),manifest)
    args.output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'passed_cases':report['passed_cases'],'hardware_verified':False,'output':str(args.output)}))
