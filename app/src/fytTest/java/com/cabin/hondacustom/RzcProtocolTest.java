package com.cabin.hondacustom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class RzcProtocolTest {
    // Independent transcript of source writer ranges: feedback field, command-105 key, option count.
    // See rzc-persistent-contracts.json and the hashed source activity's onClick method.
    private static final int[][] CONTRACTS={
        {60,0,11},{58,2,3},{59,3,3},{63,4,3},{62,5,4},{61,6,5},{73,27,5},{192,76,2},
        {64,10,2},{65,9,2},{66,11,3},{67,8,4},{68,7,3},{69,13,2},{70,24,2},{71,25,2},
        {72,26,2},{191,75,2},{74,18,3},{75,19,2},{76,20,2},{79,23,2},{80,28,2},{81,30,2},
        {82,29,2},{83,32,2},{84,33,2},{85,31,3},{86,34,3},{114,36,3},{110,37,2},{111,38,2},
        {151,39,2},{152,40,2},{153,41,2},{154,42,2},{178,72,2},{194,77,2},{195,78,2},
        {155,43,2},{156,44,2},{157,45,2},{158,46,2},{159,47,2},{161,49,2},{109,50,2},
        {196,97,2},{166,51,2},{173,52,2}
    };
    private FytProtocol.Control find(int field){
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(c.field==field&&FytProtocol.visible(0x10012a,c))return c;
        throw new AssertionError("Missing RZC field "+field);
    }
    @Test public void allPersistentMappingsUseExactRzcWireValues()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(int[] row:CONTRACTS){
            FytProtocol.Control c=find(row[0]);assertEquals(row[2],c.options.length);
            for(int profile:new int[]{0x10012a,0x11012a,0x29012a})for(int value=0;value<row[2];value++){
                FytProtocol.change(module,profile,c,value);assertEquals(105,module.command);
                assertArrayEquals(new int[]{row[1],value},module.args);
                assertEquals(Integer.valueOf(value),c.decode(value));
            }
            assertNull(c.decode(-1));assertNull(c.decode(row[2]));assertNull(c.decode(0x101));
            for(int badValue:new int[]{-1,row[2]}){
                int before=module.writes;
                try{FytProtocol.change(module,0x10012a,c,badValue);fail();}catch(IllegalArgumentException expected){}
                assertEquals(before,module.writes);
            }
        }
    }
    @Test public void rzcRowsCannotWriteOnOtherDecoders()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(int[] row:CONTRACTS)for(int profile:new int[]{0x40141,0x50141,0xB0141,0x6012a,0x7012a,0x28012a,0x12012a,0x12a}){
            try{FytProtocol.change(module,profile,find(row[0]),0);fail();}catch(IllegalArgumentException expected){}
        }
        assertEquals(0,module.writes);
    }
    @Test public void sourceInventoryIsCoveredWithoutActionsOrDuplicateFields(){
        Set<Integer> fields=new HashSet<>();
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(0x10012a,c))assertTrue("Duplicate field "+c.field,fields.add(c.field));
        Set<Integer> expected=FytClientTest.rzcFields();expected.remove(1000);assertEquals(expected,fields);
        assertEquals(59,fields.size());
        // Language/reset/initialization keys have no verified completion field and remain absent.
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(0x10012a,c))
            assertFalse(Arrays.asList(14,15,17,48,85).contains(c.key));
    }
    @Test public void optionLabelsPreserveDifferentDoorAndTimerSemantics(){
        assertArrayEquals(new String[]{"Speed-linked","Shift out of P","Off"},find(68).options);
        assertArrayEquals(new String[]{"Driver door","All doors"},find(65).options);
        assertArrayEquals(new String[]{"All doors","Driver door"},find(71).options);
        assertEquals("15 seconds",find(63).options[0]);assertEquals(Integer.valueOf(0),find(63).decode(0));
        assertEquals("−5",find(60).options[0]);assertEquals("+5",find(60).options[10]);
        assertArrayEquals(new String[]{"Narrow","Wide"},find(158).options);
    }
}
