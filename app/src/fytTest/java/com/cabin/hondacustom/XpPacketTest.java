package com.cabin.hondacustom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class XpPacketTest {
    @Test public void parsesOnlyExplicitBytesAndPreservesTheirOrder(){
        byte[] parsed=XpPacket.parse(" c6 02\n16\t01 ");
        assertArrayEquals(new int[]{198,2,22,1},XpPacket.unsigned(parsed));
        assertEquals("C6 02 16 01",XpPacket.hex(parsed));
    }
    @Test public void rejectsEmptyMalformedAndOversizedInput(){
        for(String input:new String[]{null,""," ","C6021601","0xC6 02","G0","0","000","-1","C6,02"})
            assertThrows(IllegalArgumentException.class,()->XpPacket.parse(input));
        StringBuilder max=new StringBuilder();for(int i=0;i<64;i++)max.append("FF ");
        assertEquals(64,XpPacket.parse(max.toString()).length);
        assertThrows(IllegalArgumentException.class,()->XpPacket.parse(max+"FF"));
        assertThrows(IllegalArgumentException.class,()->XpPacket.unsigned(new byte[0]));
        assertThrows(IllegalArgumentException.class,()->XpPacket.unsigned(new byte[65]));
    }
    @Test public void dispatchesExactBodyOnceWithoutAddingTheServicePrefix()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        byte[] body=XpPacket.parse("C6 02 16 01");
        FytProtocol.sendXpPacket(module,0x4012a,body,()->{});
        assertEquals(1,module.writes);assertEquals(1008,module.command);
        assertArrayEquals(new int[]{198,2,22,1},module.args);
    }
    @Test public void otherProfilesAndInvalidBodiesNeverDispatch(){
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(int profile:new int[]{0,0x3012a,0x5012a,0x6012a,0x40141,0x10012a})
            assertThrows(IllegalArgumentException.class,()->FytProtocol.sendXpPacket(module,profile,new byte[]{1},()->{}));
        assertThrows(IllegalArgumentException.class,()->FytProtocol.sendXpPacket(module,0x4012a,null,()->{}));
        assertEquals(0,module.writes);
    }
    @Test public void cancellationImmediatelyBeforeDispatchDoesNotSend(){
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        assertThrows(IllegalStateException.class,()->FytProtocol.sendXpPacket(module,0x4012a,new byte[]{1},()->{throw new IllegalStateException("Cancelled");}));
        assertEquals(0,module.writes);
    }
}
