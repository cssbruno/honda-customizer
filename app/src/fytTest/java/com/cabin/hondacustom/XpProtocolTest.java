package com.cabin.hondacustom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class XpProtocolTest {
    // Independently traced AcrivitySiYuSettings onResume/onClick for 0x4012a.
    static final int[][] ROWS={{58,2,3},{59,3,3},{60,0,11},{61,6,5},{62,5,4},{63,4,3},
        {64,10,2},{66,11,3},{69,13,2},{70,24,2},{72,26,2},{73,27,5},{74,18,3},
        {75,19,2},{76,20,2},{78,22,2},{79,23,2},{80,28,2},{81,30,2},{82,29,2},
        {87,35,2},{114,36,3}};
    static Set<Integer> fields(){Set<Integer> result=new HashSet<>();result.add(1000);for(int[] r:ROWS)result.add(r[0]);return result;}
    @Test public void onlyExactXpProfileAndSourceVisibleRowsAreEnabled(){
        Set<Integer> actual=new HashSet<>();actual.add(1000);
        assertEquals("XP",FytProtocol.family(0x4012a));
        assertFalse(FytProtocol.bnr(0x4012a));assertFalse(FytProtocol.rzc(0x4012a));
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(0x4012a,c)){
            assertTrue(c.xpOnly);assertTrue(actual.add(c.field));
        }
        assertEquals(fields(),actual);
        for(int p:new int[]{0x3012a,0x5012a,0x14012a})assertFalse(FytProtocol.supported(p));
    }
    @Test public void everyXpWireValueMatchesItsAuditedWriter()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(int[] r:ROWS){
            FytProtocol.Control c=null;
            for(FytProtocol.Control candidate:FytProtocol.CONTROLS)if(candidate.xpOnly&&candidate.field==r[0])c=candidate;
            assertNotNull(c);assertEquals(r[2],c.options.length);
            for(int value=0;value<r[2];value++){
                FytProtocol.change(module,0x4012a,c,value);
                assertEquals(105,module.command);assertArrayEquals(new int[]{r[1],value},module.args);
                assertEquals(Integer.valueOf(value),c.decode(value));
            }
            assertNull(c.decode(-1));assertNull(c.decode(r[2]));assertNull(c.decode(0x100));
        }
    }
    @Test public void profilesCannotUseEachOthersControls()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(FytProtocol.Control c:FytProtocol.CONTROLS){
            int[] profiles=c.xpOnly?new int[]{0x3012a,0x5012a,0x6012a,0xf012a,0x10012a,0x40141}:new int[]{0x4012a};
            for(int p:profiles){
                try{FytProtocol.change(module,p,c,0);fail("Cross-profile command accepted");}
                catch(IllegalArgumentException expected){}
            }
        }
        assertEquals(0,module.writes);
    }
}
