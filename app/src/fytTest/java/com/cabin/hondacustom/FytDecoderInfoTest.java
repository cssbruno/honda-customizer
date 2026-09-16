package com.cabin.hondacustom;

import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FytDecoderInfoTest {
    private String parse(String... strings){
        Parcel p=Parcel.obtain();try{
            p.writeIntArray(null);p.writeFloatArray(null);p.writeStringArray(strings);p.setDataPosition(0);
            return FytDecoderInfo.readVersion(p);
        }finally{p.recycle();}
    }
    private boolean emit(IBinder callback,String version)throws Exception{
        Parcel p=Parcel.obtain();try{
            p.writeInterfaceToken(FytProtocol.CALLBACK);p.writeInt(1005);
            p.writeIntArray(null);p.writeFloatArray(null);p.writeStringArray(new String[]{version});
            return callback.transact(1,p,null,IBinder.FLAG_ONEWAY);
        }finally{p.recycle();}
    }
    @Test public void nullAndEmptyVersionStayUnavailable(){
        assertNull(parse((String[])null));assertNull(parse());assertNull(parse((String)null));assertNull(parse("  "));
    }
    @Test public void malformedTextAndWrongArrayShapeRejected(){
        for(String[] values:new String[][]{{"one","two"},{"x\ninjected"},{new String(new char[257]).replace('\0','x')}}){
            try{parse(values);fail();}catch(IllegalArgumentException expected){}
        }
        Parcel p=Parcel.obtain();try{p.writeIntArray(new int[]{1});p.setDataPosition(0);
            try{FytDecoderInfo.readVersion(p);fail();}catch(IllegalArgumentException expected){}
        }finally{p.recycle();}
    }
    @Test public void truncatedPayloadRejected(){
        Parcel p=Parcel.obtain();try{p.writeInt(-1);p.setDataPosition(0);
            try{FytDecoderInfo.readVersion(p);fail();}catch(IllegalArgumentException expected){}
            p.setDataSize(0);p.setDataPosition(0);
            p.writeInt(-1);p.writeInt(-1);p.writeInt(1);p.setDataPosition(0);
            try{FytDecoderInfo.readVersion(p);fail();}catch(IllegalArgumentException expected){}
        }finally{p.recycle();}
    }
    @Test public void textIsDiagnosticOnlyAndSubscriptionSendsNoCommand()throws Exception{
        FytClientTest f=new FytClientTest();f.setup();try{
            f.xpReady();assertEquals(Integer.valueOf(1),f.module.notificationFlags.get(1005));
            String before=f.client.status;assertTrue(emit(f.module.callback,"fixture-version"));ShadowLooper.idleMainLooper();
            assertTrue(f.client.report().contains("fixture-version"));assertTrue(f.client.report().contains("May be cached"));
            assertTrue(f.client.values.isEmpty());assertEquals(before,f.client.status);assertEquals(0,f.module.writes);
            assertTrue(emit(f.module.callback,null));ShadowLooper.idleMainLooper();assertFalse(f.client.report().contains("fixture-version"));
        }finally{f.close();}
    }
    @Test public void oldConnectionCannotSupplyNewConnectionVersion()throws Exception{
        FytClientTest f=new FytClientTest();f.setup();try{
            f.xpReady();IBinder old=f.module.callback;emit(old,"old-fixture");ShadowLooper.idleMainLooper();
            f.client.connect();assertFalse(f.client.report().contains("old-fixture"));
            emit(old,"late-fixture");ShadowLooper.idleMainLooper();assertFalse(f.client.report().contains("late-fixture"));
        }finally{f.close();}
    }
}
