package com.cabin.hondacustom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BnrProtocolTest {
    private static final int[] PROFILES={0x6012a,0x7012a,0x8012a,0x9012a,0xa012a,0xb012a,0xf012a,0x28012a};
    // Independent expected output from the offline source audit, including layout GONE defaults.
    static Set<Integer> expectedFields(int profile){
        Set<Integer> out=new HashSet<>(Arrays.asList(1000,58,59,60,61,62,63,64,66,69,70,72,73,74,75,76,78,79,80,81,82,85,86,87,114));
        if(profile==0xf012a)out.addAll(Arrays.asList(65,67,68,71,110,111,149,150));
        else out.addAll(Arrays.asList(83,84));
        if(profile==0x8012a||profile==0x9012a||profile==0xa012a||profile==0xb012a)out.addAll(Arrays.asList(109,110,111,112,113));
        return out;
    }
    @Test public void exactPerProfileSubscriptionsRespectHiddenRows(){
        for(int p:PROFILES){
            Set<Integer> fields=new HashSet<>();fields.add(1000);
            for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(p,c))assertTrue(fields.add(c.field));
            assertEquals("Profile "+Integer.toHexString(p),expectedFields(p),fields);
            assertFalse(fields.contains(77));assertFalse(fields.contains(108));
        }
    }
    @Test public void bnrCommandsUseItsOwnKeysAndAttentionException()throws Exception{
        // field,key,count from AcrivitySiYuSettings writers and renderers.
        int[][] expected={{58,2,3},{59,3,3},{60,0,11},{61,6,5},{62,5,4},{63,4,3},{64,10,2},
            {65,9,2},{66,11,3},{67,8,4},{68,7,3},{69,13,2},{70,24,2},{71,25,2},{72,26,2},
            {73,27,5},{74,18,3},{75,19,2},{76,20,2},{79,23,2},{80,28,2},{81,30,2},{82,29,2},
            {83,32,2},{84,33,2},{85,31,3},{86,34,3},{109,36,2},{110,37,2},{111,38,2},
            {112,39,2},{113,40,2},{114,36,3},{149,42,2},{150,43,2}};
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(int p:PROFILES)for(int[] row:expected){
            FytProtocol.Control c=null;
            for(FytProtocol.Control candidate:FytProtocol.CONTROLS)if(candidate.bnrOnly&&candidate.field==row[0])c=candidate;
            assertNotNull(c);assertEquals(row[2],c.options.length);
            if(!expectedFields(p).contains(row[0])){
                int before=module.writes;
                try{FytProtocol.change(module,p,c,0);fail("Hidden setting accepted");}catch(IllegalArgumentException expectedFailure){}
                assertEquals(before,module.writes);continue;
            }
            for(int value=0;value<row[2];value++){
                FytProtocol.change(module,p,c,value);assertEquals(105,module.command);
                assertArrayEquals(new int[]{p==0xf012a&&row[0]==114?41:row[1],value},module.args);
                assertEquals(Integer.valueOf(value),c.decode(value));
            }
            assertNull(c.decode(row[2]));assertNull(c.decode(0x101));
        }
    }
    @Test public void bnrMappingsNeverAuthorizeWcOrRzcWrites()throws Exception{
        FytClientTest.FakeModule module=new FytClientTest.FakeModule();
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(c.bnrOnly)for(int p:new int[]{0x40141,0x10012a,0x29012a,0x12012a}){
            try{FytProtocol.change(module,p,c,0);fail();}catch(IllegalArgumentException expected){}
        }
        assertEquals(0,module.writes);
    }
}
