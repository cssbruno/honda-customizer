package com.cabin.hondacustom;

import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class XpDataRequestTest {
    private FytClientTest fixture;
    @Before public void setup(){fixture=new FytClientTest();fixture.setup();}
    @After public void close(){fixture.close();}
    private FytProtocol.Control tach(){
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(c.xpOnly&&c.field==78)return c;
        throw new AssertionError();
    }

    @Test public void protocolUsesStockRequestAndRejectsOtherProfilesAndCancellation()throws Exception{
        FytProtocol.requestXpData(fixture.module,0x4012a,()->{});
        assertEquals(1,fixture.module.reads);assertEquals(100,fixture.module.readCommand);
        assertArrayEquals(new int[]{0},fixture.module.readArgs);assertEquals(0,fixture.module.writes);
        for(int profile:new int[]{0,0x3012a,0x5012a,0x6012a,0x40141,0x10012a})
            assertThrows(IllegalArgumentException.class,()->FytProtocol.requestXpData(fixture.module,profile,()->{}));
        assertThrows(IllegalStateException.class,()->FytProtocol.requestXpData(fixture.module,0x4012a,()->{throw new IllegalStateException("Cancelled");}));
        assertEquals(1,fixture.module.reads);
    }

    @Test public void connectionRequestsOnceAfterSubscriptionsAndSilenceNeverCreatesValues()throws Exception{
        fixture.xpReady();
        assertEquals(1,fixture.module.reads);assertFalse(fixture.module.readBeforeSubscriptions);
        assertTrue(fixture.client.auditSummary().contains("no valid setting feedback"));
        assertTrue(fixture.client.values.isEmpty());assertFalse(fixture.client.editable(tach()));
        fixture.module.emit(1000,0x4012a);ShadowLooper.idleMainLooper(60,TimeUnit.SECONDS);
        assertEquals(1,fixture.module.reads);assertEquals(0,fixture.module.writes);
        fixture.client.disconnect();fixture.await(()->fixture.module.fields.isEmpty());
        fixture.ready();fixture.client.requestData();assertEquals(1,fixture.module.reads);
    }

    @Test public void explicitRequestClearsEarlierValuesAndCountsOnlyValidEventsAfterDispatch()throws Exception{
        fixture.xpReady();fixture.module.emit(78,1);ShadowLooper.idleMainLooper();
        assertEquals(Integer.valueOf(1),fixture.client.value(tach()));
        fixture.module.holdDescriptor=true;fixture.client.requestData();
        fixture.await(()->fixture.module.descriptorEntered);
        assertTrue(fixture.client.values.isEmpty());assertTrue(fixture.client.busy());
        fixture.module.emit(78,0);ShadowLooper.idleMainLooper(); // Before dispatch: not a reply to this request.
        fixture.client.requestData();fixture.client.change(tach(),1,true);
        fixture.client.sendPacket(new byte[]{1},true,fixture.client.connectionToken());
        assertEquals(1,fixture.module.reads);assertEquals(0,fixture.module.writes);
        fixture.module.releaseDescriptor=true;
        fixture.await(()->fixture.client.auditSummary().contains("Request dispatched."));
        assertTrue(fixture.client.auditSummary().contains("0 settings with valid events"));
        fixture.module.emit(78,1);fixture.module.emit(78,1);fixture.module.emit(87,256);
        ShadowLooper.idleMainLooper();
        assertTrue(fixture.client.auditSummary().contains("1 settings with valid events"));
        ShadowLooper.idleMainLooper(FytClient.TIMEOUT_MS+1,TimeUnit.MILLISECONDS);
        assertTrue(fixture.client.auditSummary().contains("Observation finished: 1 settings"));
        assertTrue(fixture.client.editable(tach()));assertEquals(2,fixture.module.reads);
        assertFalse(fixture.client.status.contains("confirmed"));assertEquals(0,fixture.module.writes);
        ShadowLooper.idleMainLooper(FytClient.FRESH_MS+1,TimeUnit.MILLISECONDS);
        assertNull(fixture.client.value(tach()));assertFalse(fixture.client.editable(tach()));
    }

    @Test public void disconnectDuringDescriptorLookupPreventsReadDispatch()throws Exception{
        fixture.xpReady();fixture.module.holdDescriptor=true;fixture.client.requestData();
        fixture.await(()->fixture.module.descriptorEntered);fixture.client.disconnect();
        assertFalse(fixture.client.status.contains("unconfirmed"));
        fixture.module.releaseDescriptor=true;fixture.await(()->fixture.module.fields.isEmpty());
        assertEquals(1,fixture.module.reads);assertTrue(fixture.client.values.isEmpty());
    }

    @Test public void timedOutRequestCannotDispatchLaterOrRetryAutomatically()throws Exception{
        fixture.xpReady();fixture.module.holdDescriptor=true;fixture.client.requestData();
        fixture.await(()->fixture.module.descriptorEntered);
        ShadowLooper.idleMainLooper(FytClient.TIMEOUT_MS+1,TimeUnit.MILLISECONDS);
        assertTrue(fixture.client.auditSummary().contains("did not complete the request call"));
        fixture.module.releaseDescriptor=true;fixture.module.holdDescriptor=false;
        // A later explicit request drains the blocked worker; only this new request may dispatch.
        fixture.client.requestData();fixture.await(()->fixture.client.auditSummary().contains("Request dispatched."));
        assertEquals(2,fixture.module.reads);
        ShadowLooper.idleMainLooper(60,TimeUnit.SECONDS);assertEquals(2,fixture.module.reads);
        assertTrue(fixture.client.values.isEmpty());assertEquals(0,fixture.module.writes);
    }

    @Test public void failedDataRequestNeverConfirmsOrFallsBack()throws Exception{
        fixture.xpReady();fixture.module.rejectWrite=true;fixture.client.requestData();
        fixture.await(()->!fixture.client.connected());
        assertTrue(fixture.client.auditHistory().contains("FYT data request failed"));
        assertEquals(1,fixture.bindings);assertTrue(fixture.client.values.isEmpty());
        assertEquals(2,fixture.module.reads);assertEquals(0,fixture.module.writes);
    }
}
