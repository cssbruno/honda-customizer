"""Execute the new C state machine on the host; no devices, no APK fixtures."""
import ctypes as c
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

from trace_cluster_transport import derive

ROOT=Path(__file__).resolve().parents[1]
class Frame(c.Structure):
    _fields_=[('standard_id',c.c_uint32),('extended_id',c.c_uint32),('ide',c.c_uint8),
              ('rtr',c.c_uint8),('dlc',c.c_uint8),('data',c.c_uint8*8),('padding',c.c_uint8)]
class State(c.Structure):
    _fields_=[('magic',c.c_uint32),('transaction',c.c_uint32),('started',c.c_uint32),
              ('frame',Frame),('status',c.c_uint8),('published',c.c_uint8),
              ('feature',c.c_uint8),('api_value',c.c_uint8),('queued_owned',c.c_uint8)]
class Reply(c.Structure):
    _fields_=[('body',c.c_uint8*13)]

def request(feature=0,value=0,transaction=1,op=1):
    return bytes((0xD7,11,0x48,0x43,0x58,1,op))+transaction.to_bytes(4,'big')+bytes((feature,value))

class HcxCoreTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory(prefix='hcx-host-')
        compiler=shutil.which('cc')
        if not compiler:
            raise RuntimeError('A host C compiler is required; HCX tests must not silently skip')
        source=ROOT/'firmware/cluster-extension'
        lib=Path(cls.temp.name)/'hcx.so'
        subprocess.run([compiler,'-std=c11','-shared','-fPIC','-O2','-Wall','-Wextra','-Werror',
                        str(source/'hcx.c'),str(source/'host_stub.c'),'-o',str(lib)],check=True)
        cls.lib=c.CDLL(str(lib))
        cls.lib.hcx_init.argtypes=[c.POINTER(State)]
        cls.lib.hcx_command.argtypes=[c.POINTER(State),c.c_void_p,c.c_uint,c.c_uint32,c.POINTER(Reply)]
        cls.lib.hcx_before_tx.argtypes=[c.POINTER(State),c.POINTER(Frame),c.c_uint32]
        cls.lib.hcx_after_tx.argtypes=[c.POINTER(State),c.c_uint,c.c_uint32]
        cls.lib.hcx_can_received.argtypes=[c.POINTER(State),c.POINTER(Frame),c.c_uint32]
        cls.lib.hcx_poll.argtypes=[c.POINTER(State),c.c_uint32,c.POINTER(Reply)]
        cls.lib.test_queue_frame.restype=c.POINTER(Frame)
    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()
    def setUp(self):
        self.s=State(); self.lib.hcx_init(c.byref(self.s)); self.lib.test_queue_reset(1)
    def command(self,p,now=0):
        r=Reply(); used=self.lib.hcx_command(c.byref(self.s),p,len(p),now,c.byref(r))
        return used,bytes(r.body)
    def received(self,now=1,**changes):
        f=Frame(); f.extended_id=0x16305450; f.ide=4; f.dlc=3
        f.data[:3]=[0xC0,self.s.frame.data[1],self.s.frame.data[2]]
        for k,v in changes.items():
            if k.startswith('data'): f.data[int(k[-1])]=v
            else: setattr(f,k,v)
        self.lib.hcx_can_received(c.byref(self.s),c.byref(f),now)
    def start_tx(self,mailbox=0,now=0):
        self.assertEqual(self.lib.hcx_before_tx(c.byref(self.s),c.byref(self.s.frame),now),1)
        self.lib.hcx_after_tx(c.byref(self.s),mailbox,now)
    def test_all_20_values_match_independent_oem_xml(self):
        for feature,setting,count in ((1,0x5B,3),(2,0x38,2),(3,0x29,7),(4,0x2A,8)):
            for api in range(1,count+1):
                with self.subTest(feature=feature,api=api):
                    self.setUp()
                    self.assertEqual(self.command(request(feature,api))[1][6],0x11)
                    oem=derive(setting,api,0x50,0x54)
                    f=self.lib.test_queue_frame().contents
                    self.assertEqual(f.extended_id,int(oem['oem_bcan_id'],16))
                    self.assertEqual(bytes(f.data[:f.dlc]),bytes.fromhex(oem['oem_payload']))
                    self.assertEqual((f.ide,f.rtr,f.dlc),(4,0,3))
    def test_every_invalid_byte_value_and_feature_rejected(self):
        for feature,count in ((1,3),(2,2),(3,7),(4,8),(0,0),(5,0),(255,0)):
            for api in range(256):
                if 1<=api<=count: continue
                self.assertEqual(self.command(request(feature,api))[1][6],0x16)
        self.assertEqual(self.lib.test_queue_count(),0)
    def test_magic_length_operation_and_zero_transaction(self):
        p=request(1,1)
        for i in (1,2,3,4,5,6):
            bad=bytearray(p); bad[i]=255
            self.assertEqual(self.command(bytes(bad))[1][6],0x16)
        for n in range(1,len(p)):
            self.assertEqual(self.command(p[:n])[1][6],0x16)
        self.assertEqual(self.command(p+b'\0')[1][6],0x16)
        self.assertEqual(self.command(request(1,1,0))[1][6],0x16)
        self.assertEqual(self.lib.test_queue_count(),0)
    def test_hello_and_stock_packets_do_not_transmit(self):
        _,r=self.command(request(op=0))
        self.assertEqual((r[6],r[11],r[12]),(0x10,1,15))
        self.assertEqual(self.command(b'\xC6\x02\x16\x01')[0],0)
        self.assertEqual(self.command(b'')[0],0)
        self.assertEqual(self.lib.test_queue_count(),0)
    def test_only_matching_post_transmit_reply_is_reported(self):
        self.command(request(1,3))
        self.received(); self.assertEqual(self.s.status,0x11)
        self.start_tx(); self.assertEqual(self.s.status,0x12)
        for bad in ({'extended_id':0x16305054},{'ide':0},{'rtr':2},{'dlc':2},
                    {'dlc':4},{'data0':0x40},{'data1':0x20},{'data2':0}):
            self.received(**bad); self.assertEqual(self.s.status,0x12)
        self.received(); self.assertEqual(self.s.status,0x13)
        reply=Reply()
        self.assertEqual(self.lib.hcx_poll(c.byref(self.s),2,c.byref(reply)),1)
        self.assertEqual(bytes(reply.body)[6],0x13)
        self.assertEqual(self.lib.hcx_poll(c.byref(self.s),3,c.byref(reply)),0)
    def test_busy_duplicate_and_queue_failure(self):
        self.command(request(1,1))
        self.assertEqual(self.command(request(2,2,2))[1][6],0x15)
        self.start_tx(mailbox=4)
        self.assertEqual(self.s.status,0x18)
        self.assertEqual(self.command(request(1,1))[1][6],0x19)
        self.lib.test_queue_reset(0)
        self.assertEqual(self.command(request(1,1,2))[1][6],0x17)
    def test_expired_queue_frame_is_cancelled_before_hardware(self):
        self.command(request(1,1))
        r=Reply(); self.lib.hcx_poll(c.byref(self.s),800,c.byref(r))
        self.assertEqual(self.s.status,0x14)
        self.assertEqual(self.command(request(1,2,2),800)[1][6],0x15)
        self.assertEqual(self.lib.hcx_before_tx(c.byref(self.s),c.byref(self.s.frame),801),2)
        self.assertEqual(self.command(request(1,2,2),802)[1][6],0x11)
    def test_timeout_and_counter_wrap_reject_late_reply(self):
        start=0xFFFFFF00
        self.command(request(2,1),start); self.start_tx(now=start)
        self.received(now=(start+800)&0xFFFFFFFF)
        self.assertEqual(self.s.status,0x14)
    def test_unrelated_transmission_does_not_start_request(self):
        self.command(request(3,1)); f=Frame(); f.extended_id=0x123
        self.assertEqual(self.lib.hcx_before_tx(c.byref(self.s),c.byref(f),1),0)
        self.assertEqual(self.s.status,0x11)
    def test_status_is_transaction_scoped_and_reset_discards_it(self):
        self.command(request(4,8,123)); self.start_tx(); self.received()
        self.assertEqual(self.command(request(transaction=124,op=2))[1][6],0x16)
        self.assertEqual(self.command(request(transaction=123,op=2))[1][6],0x13)
        self.lib.hcx_init(c.byref(self.s))
        self.assertEqual(self.command(request(transaction=123,op=2))[1][6],0x16)

if __name__=='__main__': unittest.main()
