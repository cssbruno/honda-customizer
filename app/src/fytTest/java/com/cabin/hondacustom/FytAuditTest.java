package com.cabin.hondacustom;

import android.os.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FytAuditTest {
    private FytClientTest fixture;
    @Before public void setup(){fixture=new FytClientTest();fixture.setup();}
    @After public void close(){fixture.close();}

    static boolean version(IBinder callback,String version)throws Exception{
        Parcel p=Parcel.obtain();try{
            p.writeInterfaceToken(FytProtocol.CALLBACK);p.writeInt(1005);
            p.writeIntArray(null);p.writeFloatArray(null);p.writeStringArray(new String[]{version});
            return callback.transact(1,p,null,IBinder.FLAG_ONEWAY);
        }finally{p.recycle();}
    }

    @Test public void readingReasonsFollowRealFeedbackAndNeverRetainExpiredValues()throws Exception{
        fixture.xpReady();
        assertTrue(fixture.client.auditSummary().contains("0 of 22"));
        assertTrue(fixture.client.auditFeedback().contains("no valid feedback received"));
        fixture.module.emit(78,1);ShadowLooper.idleMainLooper();
        assertTrue(fixture.client.auditSummary().contains("1 of 22"));
        assertTrue(fixture.client.auditFeedback().contains("[78]: On — received 0 s ago"));
        fixture.module.emit(78,256);ShadowLooper.idleMainLooper();
        assertTrue(fixture.client.auditFeedback().contains("[78]: Unavailable — invalid FYT value"));
        assertTrue(fixture.client.auditSummary().contains("0 of 22"));
        fixture.module.emit(78,1);ShadowLooper.idleMainLooper();
        ShadowLooper.idleMainLooper(FytClient.FRESH_MS+1,TimeUnit.MILLISECONDS);
        assertTrue(fixture.client.auditFeedback().contains("[78]: Unavailable — feedback expired"));
        assertFalse(fixture.client.report().contains("[78]: On"));
        assertTrue(fixture.client.values.isEmpty());assertEquals(0,fixture.module.writes);
    }

    @Test public void decoderTimeoutCanRecoverAndInvalidReplyClearsEarlierIdentification()throws Exception{
        fixture.xpReady();ShadowLooper.idleMainLooper(FytClient.TIMEOUT_MS+1,TimeUnit.MILLISECONDS);
        assertTrue(fixture.client.auditIdentity().contains("within 8 seconds"));
        assertTrue(version(fixture.module.callback,"audit-fixture"));ShadowLooper.idleMainLooper();
        assertTrue(fixture.client.auditIdentity().contains("audit-fixture"));
        assertFalse(version(fixture.module.callback,"bad\nversion"));ShadowLooper.idleMainLooper();
        assertFalse(fixture.client.auditIdentity().contains("audit-fixture"));
        assertTrue(fixture.client.auditIdentity().contains("invalid identification"));
        assertTrue(fixture.client.connected());assertTrue(fixture.client.values.isEmpty());assertEquals(0,fixture.module.writes);
    }

    @Test public void disconnectMarksIdentificationHistoricalAndReconnectClearsIt()throws Exception{
        fixture.xpReady();version(fixture.module.callback,"historical-fixture");fixture.module.emit(78,1);ShadowLooper.idleMainLooper();
        fixture.client.disconnect();
        assertTrue(fixture.client.auditIdentity().contains("previous connection"));
        assertTrue(fixture.client.auditIdentity().contains("historical-fixture"));
        assertTrue(fixture.client.auditFeedback().contains("disconnected"));
        assertFalse(fixture.client.report().contains("[78]: On"));
        fixture.client.connect();assertFalse(fixture.client.report().contains("historical-fixture"));
        assertTrue(fixture.client.auditIdentity().contains("Last profile: unavailable"));
    }

    @Test public void capabilityFindingsUseExactProfileAndAreNeverHardwareConfirmation()throws Exception{
        assertTrue(fixture.client.auditCapabilities().contains("Connect and wait"));
        fixture.ready();assertTrue(fixture.client.auditCapabilities().contains("only to profile 0x4012A"));
        fixture.xpReady();String audit=fixture.client.auditCapabilities();
        assertTrue(audit.contains("not implemented"));assertTrue(audit.contains("Raw CAN/OBD: unverified"));
        assertTrue(audit.contains("2.23.0711.1001"));assertEquals(0,fixture.module.writes);
    }
}
