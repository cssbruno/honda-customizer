package com.cabin.hondacustom;

import android.content.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FytClientTest {
    FytClient client; FakeModule module; ServiceConnection connection; int bindings,unbindings; boolean nullBinding;
    boolean missingToolkit; volatile boolean holdToolkit,toolkitEntered,releaseToolkit;
    final java.util.List<String> actions=new java.util.ArrayList<>();
    final FytProtocol.Control alarm=FytProtocol.CONTROLS.get(9);
    static class FakeModule extends Binder {
        volatile IBinder callback; volatile int writes;
        final Map<Integer,Integer> notificationFlags=Collections.synchronizedMap(new HashMap<>()); int command; int[] args;
        volatile boolean holdWrite, rejectWrite, released, exceptionHeader;
        volatile boolean holdDescriptor,descriptorEntered,releaseDescriptor;
        final Set<Integer> fields=Collections.synchronizedSet(new HashSet<>());
        FakeModule(){attachInterface(null,FytProtocol.MODULE);}
        @Override public String getInterfaceDescriptor(){
            if(holdDescriptor){descriptorEntered=true;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(!releaseDescriptor&&System.nanoTime()<end)try{Thread.sleep(5);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
            return super.getInterfaceDescriptor();
        }
        @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags)throws RemoteException{
            d.enforceInterface("com.syu.ipc.IRemoteModule");
            if(code==3){callback=d.readStrongBinder();int field=d.readInt();fields.add(field);int notify=d.readInt();notificationFlags.put(field,notify);assertEquals(field==1000?1:0,notify);}
            else if(code==4){d.readStrongBinder();fields.remove(d.readInt());}
            else if(code==1){command=d.readInt();args=d.createIntArray();assertNull(d.createFloatArray());assertNull(d.createStringArray());writes++;
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(holdWrite&&!released&&System.nanoTime()<end)try{Thread.sleep(5);}catch(InterruptedException e){throw new RemoteException("Interrupted");}
                if(rejectWrite)throw new RemoteException("Rejected write");
            }
            else return false;
            assertEquals(0,flags);assertEquals(0,d.dataAvail());
            // Joying x/c$a writes no reply for cmd/register/unregister.
            if(exceptionHeader)r.writeNoException();return true;
        }
        void emit(int field,int raw)throws Exception{Parcel d=Parcel.obtain();try{d.writeInterfaceToken("com.syu.ipc.IModuleCallback");d.writeInt(field);d.writeIntArray(new int[]{raw});d.writeFloatArray(null);d.writeStringArray(null);callback.transact(1,d,null,IBinder.FLAG_ONEWAY);}finally{d.recycle();}}
    }
    @Before public void setup(){module=new FakeModule();
        Binder toolkit=new Binder(){
            {attachInterface(null,FytProtocol.TOOLKIT);}
            @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags){assertEquals(1,code);d.enforceInterface("com.syu.ipc.IRemoteToolkit");assertEquals(7,d.readInt());assertEquals(0,d.dataAvail());
                if(holdToolkit){toolkitEntered=true;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!releaseToolkit&&System.nanoTime()<end)try{Thread.sleep(5);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
                r.writeNoException();r.writeStrongBinder(module);return true;}
        };
        Context context=new ContextWrapper(RuntimeEnvironment.getApplication()){
            @Override public boolean bindService(Intent i,ServiceConnection c,int flags){assertEquals("com.syu.ms",i.getComponent().getPackageName());boolean direct="com.syu.ms.canbus".equals(i.getAction());assertEquals(direct?"app.ModuleService":"app.ToolkitService",i.getComponent().getClassName());assertEquals(direct?"com.syu.ms.canbus":"com.syu.ms.toolkit",i.getAction());actions.add(i.getAction());bindings++;connection=c;if(!direct&&missingToolkit)return false;if(nullBinding)c.onNullBinding(i.getComponent());else c.onServiceConnected(i.getComponent(),direct?module:toolkit);return true;}
            @Override public void unbindService(ServiceConnection c){unbindings++;}
        };
        client=new FytClient(context,()->{});
    }
    @After public void close(){releaseToolkit=true;module.released=true;module.releaseDescriptor=true;client.destroy();}
    void await(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!condition.getAsBoolean()&&System.nanoTime()<end){ShadowLooper.idleMainLooper();Thread.sleep(5);}ShadowLooper.idleMainLooper();assertTrue(client.status,condition.getAsBoolean());}
    static Set<Integer> rzcFields(){
        Set<Integer> fields=new HashSet<>(Arrays.asList(1000,109,110,111,114,151,152,153,154,155,156,157,158,159,161,166,173,175,176,177,178,179,190,191,192,193,194,195,196,197));
        for(int field=58;field<=87;field++)fields.add(field);return fields;
    }
    void ready()throws Exception{client.connect();await(()->module.fields.size()==1);module.emit(1000,0x40141);await(()->module.fields.size()==39);module.emit(69,2);await(()->client.editable(alarm));}
    @Test public void subscribesWithoutReplayingCachedVehicleSettings()throws Exception{
        client.connect();await(()->module.fields.size()==1);module.emit(1000,0x10012a);await(()->module.fields.size()==rzcFields().size());
        assertEquals(Integer.valueOf(1),module.notificationFlags.get(1000));
        for(int field:rzcFields())if(field!=1000)assertEquals(Integer.valueOf(0),module.notificationFlags.get(field));
        assertTrue(client.values.isEmpty());
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(0x10012a,c)){assertNull(client.value(c));assertFalse(client.editable(c));}
        assertEquals(0,module.writes);
    }
    @Test public void earlierExpiryDoesNotEraseNewerFeedback()throws Exception{
        ready();ShadowLooper.idleMainLooper(20000,TimeUnit.MILLISECONDS);module.emit(69,3);await(()->Integer.valueOf(2).equals(client.value(alarm)));
        ShadowLooper.idleMainLooper(10002,TimeUnit.MILLISECONDS);assertEquals(Integer.valueOf(2),client.value(alarm));assertTrue(client.editable(alarm));
        ShadowLooper.idleMainLooper(20000,TimeUnit.MILLISECONDS);assertNull(client.value(alarm));assertFalse(client.editable(alarm));
    }
    @Test public void bindsOnlyFytModuleSeven()throws Exception{ready();assertEquals(1,bindings);assertEquals(Integer.valueOf(1),client.values.get(69));}
    @Test public void reportedXpProfileSubscribesAndRequiresRealConfirmation()throws Exception{
        client.connect();await(()->module.fields.size()==1);module.emit(1000,0x4012a);
        await(()->module.fields.size()==XpProtocolTest.fields().size());
        assertEquals(XpProtocolTest.fields(),module.fields);assertTrue(client.values.isEmpty());
        assertTrue(client.report().contains("XP"));
        FytProtocol.Control tachometer=null;
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(c.xpOnly&&c.field==78)tachometer=c;
        final FytProtocol.Control control=tachometer;assertNotNull(control);
        assertFalse(client.editable(control));module.emit(78,0);await(()->client.editable(control));
        client.change(control,1,true);await(()->module.writes==1);
        assertEquals(105,module.command);assertArrayEquals(new int[]{22,1},module.args);
        assertTrue(client.busy());assertEquals(Integer.valueOf(0),client.value(control));
        module.emit(78,1);await(()->!client.busy());assertEquals(Integer.valueOf(1),client.value(control));
        module.emit(78,0x101);await(()->!client.editable(control));
    }
    @Test public void exceptionHeaderModuleStillConnectsAndConfirms()throws Exception{
        module.exceptionHeader=true;commandUsesFytEncodingAndWaitsForFeedback();
    }
    @Test public void emptyReplyCleanupUnregistersEveryField()throws Exception{
        ready();client.disconnect();await(()->module.fields.isEmpty());assertEquals(1,bindings);assertEquals(1,unbindings);
    }
    @Test public void commandUsesFytEncodingAndWaitsForFeedback()throws Exception{ready();client.change(alarm,2,true);await(()->module.writes==1);assertEquals(106,module.command);assertArrayEquals(new int[]{4,3},module.args);assertTrue(client.busy());assertEquals(Integer.valueOf(1),client.values.get(69));module.emit(69,3);await(()->!client.busy());assertEquals(Integer.valueOf(2),client.values.get(69));assertTrue(client.status.contains("confirmed"));}
    @Test public void parkedRequired()throws Exception{ready();client.change(alarm,2,false);assertFalse(client.busy());assertEquals(0,module.writes);}
    @Test public void unmappedProfileKeepsFytConnectionForReport()throws Exception{client.connect();await(()->module.fields.size()==1);module.emit(1000,0x140141);await(()->client.profile()==0x140141);assertTrue(client.connected());assertTrue(client.values.isEmpty());assertTrue(client.report().contains("0x140141"));}
    @Test public void changedProfileInvalidatesValues()throws Exception{ready();module.emit(1000,0xC0141);await(()->!client.connected());assertFalse(client.editable(alarm));}
    @Test public void invalidFeedbackDisablesControl()throws Exception{ready();module.emit(69,0);await(()->!client.editable(alarm));assertFalse(client.values.containsKey(69));}
    @Test public void expiredFeedbackDisablesControl()throws Exception{ready();ShadowLooper.idleMainLooper(FytClient.FRESH_MS+1,TimeUnit.MILLISECONDS);assertFalse(client.editable(alarm));assertNull(client.value(alarm));assertFalse(client.values.containsKey(alarm.field));assertFalse(client.report().contains("Alarm volume [69]: Medium"));}
    @Test public void mismatchedFeedbackDoesNotConfirm()throws Exception{ready();client.change(alarm,2,true);await(()->module.writes==1);module.emit(69,1);ShadowLooper.idleMainLooper();assertTrue(client.busy());}
    @Test public void missingFeedbackTimesOut()throws Exception{ready();client.change(alarm,2,true);await(()->module.writes==1);ShadowLooper.idleMainLooper(FytClient.TIMEOUT_MS+1,TimeUnit.MILLISECONDS);assertFalse(client.connected());assertTrue(client.status.contains("unconfirmed"));}
    @Test public void oldSessionCannotRestoreValues()throws Exception{ready();IBinder old=module.callback;client.disconnect();module.callback=old;module.emit(1000,0x40141);module.emit(69,3);ShadowLooper.idleMainLooper();assertFalse(client.connected());assertTrue(client.values.isEmpty());}
    @Test public void binderDeathClearsValues()throws Exception{ready();connection.onServiceDisconnected(new ComponentName("com.syu.ms","app.ToolkitService"));assertFalse(client.connected());assertTrue(client.values.isEmpty());}
    @Test public void wrongBinderDescriptorRejected()throws Exception{try{FytProtocol.module(new Binder());fail();}catch(RemoteException expected){assertTrue(expected.getMessage().contains("interface"));}}
    @Test public void exactProfilesAndVisibility(){assertFalse(FytProtocol.supported(0x141));assertFalse(FytProtocol.visible(0x50141,FytProtocol.CONTROLS.get(0)));assertTrue(FytProtocol.visible(0x50141,FytProtocol.CONTROLS.get(3)));assertTrue(FytProtocol.visible(0xB0141,FytProtocol.CONTROLS.get(8)));assertFalse(FytProtocol.visible(0x40141,FytProtocol.CONTROLS.get(8)));}
    @Test public void oneBasedBoundsAndLowByte(){assertNull(alarm.decode(0));assertNull(alarm.decode(4));assertNull(alarm.decode(-1));assertNull(alarm.decode(65536));assertEquals(Integer.valueOf(2),alarm.decode(0x103));assertEquals(Integer.valueOf(6),FytProtocol.CONTROLS.get(12).decode(7));}
    @Test public void hiddenOrInvalidWritesRejected()throws Exception{try{FytProtocol.change(module,0x50141,FytProtocol.CONTROLS.get(0),1);fail();}catch(IllegalArgumentException expected){}try{FytProtocol.change(module,0x40141,alarm,3);fail();}catch(IllegalArgumentException expected){}assertEquals(0,module.writes);}
    @Test public void earlyFeedbackCannotConfirmRejectedTransaction()throws Exception{ready();module.holdWrite=true;module.rejectWrite=true;client.change(alarm,2,true);await(()->module.writes==1);module.emit(69,3);ShadowLooper.idleMainLooper();assertTrue("Must wait for transaction result",client.busy());assertFalse(client.status.contains("confirmed"));module.released=true;await(()->!client.connected());assertTrue(client.status.contains("unconfirmed"));}
    @Test public void earlyFeedbackIsConfirmedAfterSuccessfulTransaction()throws Exception{ready();module.holdWrite=true;client.change(alarm,2,true);await(()->module.writes==1);module.emit(69,3);ShadowLooper.idleMainLooper();assertTrue(client.busy());module.released=true;await(()->!client.busy());assertTrue(client.status.contains("confirmed"));}
    @Test public void duplicateProfileDoesNotEraseWriteOutcome()throws Exception{ready();client.change(alarm,2,true);await(()->module.writes==1);module.emit(69,3);await(()->!client.busy());String outcome=client.status;module.emit(1000,0x40141);ShadowLooper.idleMainLooper();assertEquals(outcome,client.status);}
    @Test public void synchronousNullBindingIsReleased(){nullBinding=true;client.connect();assertFalse(client.connected());assertEquals(1,unbindings);}
    @Test public void settingsSubscribeOnlyAfterVerifiedProfile()throws Exception{client.connect();await(()->module.fields.size()==1);assertTrue(module.fields.contains(1000));assertFalse(module.fields.contains(69));module.emit(1000,0x40141);await(()->module.fields.size()==39);module.emit(69,2);await(()->client.editable(alarm));}
    @Test public void unsupportedProfileNeverSubscribesSettings()throws Exception{client.connect();await(()->module.fields.size()==1);module.emit(1000,0x141);await(()->client.profile()==0x141);assertFalse(module.fields.contains(69));assertFalse(client.editable(alarm));}
    @Test public void latestFeedbackMustStillMatchWhenTransactionCompletes()throws Exception{ready();module.holdWrite=true;client.change(alarm,2,true);await(()->module.writes==1);module.emit(69,3);module.emit(69,1);ShadowLooper.idleMainLooper();module.released=true;await(()->client.status.startsWith("Waiting"));assertTrue(client.busy());}
    @Test public void disconnectDuringWriteReportsUncertainOutcome()throws Exception{ready();module.holdWrite=true;client.change(alarm,2,true);await(()->module.writes==1);client.disconnect();assertTrue(client.status.contains("unconfirmed"));module.released=true;assertFalse(client.connected());}
    @Test public void everyControlMatchesRecordedFytWireContract()throws Exception{
        int[] fields={109,110,111,88,65,66,67,68,102,69,70,71,72};
        int[] keys={16,15,14,9,6,7,8,5,13,4,3,2,1};
        int[] bases={0,0,0,0,0,0,0,0,0,1,1,1,1};
        int[] sizes={2,2,3,2,2,2,2,2,2,3,3,3,7};
        for(int i=0;i<fields.length;i++){FytProtocol.Control c=FytProtocol.CONTROLS.get(i);assertEquals(fields[i],c.field);assertEquals(sizes[i],c.options.length);
            int p=(i==3||i==8)?0xB0141:0x40141;
            for(int value=0;value<sizes[i];value++){FytProtocol.change(module,p,c,value);assertEquals(106,module.command);assertArrayEquals(new int[]{keys[i],value+bases[i]},module.args);}
        }
    }
    @Test public void disconnectDuringDescriptorLookupPreventsDispatch()throws Exception{
        ready();module.holdDescriptor=true;client.change(alarm,2,true);await(()->module.descriptorEntered);
        client.disconnect();module.releaseDescriptor=true;await(()->module.fields.isEmpty());assertEquals(0,module.writes);
    }
    @Test public void feedbackBeforeDispatchCannotConfirmLaterWrite()throws Exception{
        ready();module.holdDescriptor=true;client.change(alarm,2,true);await(()->module.descriptorEntered);
        module.emit(69,3);ShadowLooper.idleMainLooper();module.releaseDescriptor=true;
        await(()->client.status.startsWith("Waiting"));assertTrue(client.busy());
        module.emit(69,3);await(()->!client.busy());
    }
    @Test public void missingToolkitFailsWithoutFallback(){
        missingToolkit=true;client.connect();assertFalse(client.connected());
        assertEquals(Collections.singletonList("com.syu.ms.toolkit"),actions);
        assertTrue(client.values.isEmpty());assertEquals(0,module.writes);
    }
    @Test public void blockedToolkitTimesOutWithoutFallback()throws Exception{
        holdToolkit=true;client.connect();await(()->toolkitEntered);
        ShadowLooper.idleMainLooper(FytClient.TIMEOUT_MS+1,TimeUnit.MILLISECONDS);
        assertFalse(client.connected());assertEquals(Collections.singletonList("com.syu.ms.toolkit"),actions);
        assertTrue(client.values.isEmpty());assertEquals(0,module.writes);
    }
    @Test public void rzcCivicUsesItsOwnUnitsAndTachometerCommands()throws Exception{
        client.connect();await(()->module.fields.contains(1000));module.emit(1000,0x10012a);
        await(()->module.fields.size()==60);assertEquals(rzcFields(),module.fields);
        FytProtocol.Control units=FytProtocol.CONTROLS.get(13);module.emit(77,0);await(()->client.editable(units));
        client.change(units,1,true);await(()->module.writes==1);assertEquals(105,module.command);assertArrayEquals(new int[]{21,1},module.args);
        module.emit(77,1);await(()->!client.busy());assertFalse(client.editable(alarm));
    }
    @Test public void bnrDoesNotExposeItsHiddenUnitsControl()throws Exception{
        client.connect();await(()->module.fields.contains(1000));module.emit(1000,0x6012a);await(()->module.fields.size()==27);
        assertEquals(BnrProtocolTest.expectedFields(0x6012a),module.fields);
        assertFalse(FytProtocol.visible(0x6012a,FytProtocol.CONTROLS.get(13)));
        FytProtocol.change(module,0x6012a,FytProtocol.CONTROLS.get(14),1);assertEquals(105,module.command);assertArrayEquals(new int[]{22,1},module.args);
    }
    @Test public void decoderDialectsNeverShareCommandsOrLowByteDecoding()throws Exception{
        for(int p:new int[]{0x10012a,0x11012a,0x29012a}){assertTrue(FytProtocol.supported(p));assertFalse(FytProtocol.visible(p,alarm));
            FytProtocol.change(module,p,FytProtocol.CONTROLS.get(15),1);assertArrayEquals(new int[]{35,1},module.args);assertEquals(105,module.command);}
        assertFalse(FytProtocol.supported(0x12012a));assertFalse(FytProtocol.visible(0x40141,FytProtocol.CONTROLS.get(14)));
        assertNull(FytProtocol.CONTROLS.get(14).decode(0x101));
    }
    @Test public void additionalWcContractsAreExactAndIsolated()throws Exception{
        // Independent expected values from doors-lights/findings.md: field, command, key, first wire, count.
        int[][] expected={{58,104,3,0,2},{60,104,1,0,2},{59,104,2,1,3},{87,104,4,0,3},{86,104,5,0,3},
            {49,102,5,0,2},{50,102,4,0,5},{51,102,3,0,5},{52,102,2,0,4},{53,102,1,1,3},
            {54,103,4,0,2},{55,103,3,0,2},{56,103,2,0,2},{57,103,1,0,2},
            {62,105,3,0,2},{63,105,2,0,2},{61,105,4,1,3},{64,105,1,1,3},
            {93,110,12,0,2},{94,110,12,4,2},{95,110,14,0,2},{96,105,8,0,2},
            {97,105,7,0,3},{98,106,11,0,2},{99,106,12,0,2},{100,106,10,0,2},{101,105,9,1,4}};
        for(int i=0;i<expected.length;i++){
            int[] e=expected[i];FytProtocol.Control c=FytProtocol.CONTROLS.get(16+i);
            assertEquals(e[0],c.field);assertEquals(e[4],c.options.length);
            for(int value=0;value<e[4];value++){
                FytProtocol.change(module,0x40141,c,value);
                assertEquals(e[1],module.command);assertArrayEquals(new int[]{e[2],e[3]+value},module.args);
            }
            for(int profile:new int[]{0x10012a,0x6012a,0x141,0x140141}){
                try{FytProtocol.change(module,profile,c,0);fail("Wrong decoder accepted");}catch(IllegalArgumentException expectedFailure){}
            }
        }
    }
    @Test public void additionalControlsNeedRealFreshFeedback()throws Exception{
        ready();FytProtocol.Control door=FytProtocol.CONTROLS.get(16);
        assertFalse(client.editable(door));client.change(door,1,true);assertEquals(0,module.writes);
        module.emit(58,0);await(()->client.editable(door));client.change(door,1,true);await(()->module.writes==1);
        assertTrue(client.busy());assertEquals(Integer.valueOf(0),client.values.get(58));
        module.emit(58,1);await(()->!client.busy());
        ShadowLooper.idleMainLooper(FytClient.FRESH_MS+1,TimeUnit.MILLISECONDS);assertFalse(client.editable(door));
    }
    @Test public void lightingExclusionsAndDurationReadback(){
        for(int i=21;i<=23;i++){
            assertFalse(FytProtocol.visible(0x50141,FytProtocol.CONTROLS.get(i)));
            assertFalse(FytProtocol.visible(0x60141,FytProtocol.CONTROLS.get(i)));
        }
        FytProtocol.Control duration=FytProtocol.CONTROLS.get(35);
        assertEquals(Integer.valueOf(0),duration.decode(0));assertEquals(Integer.valueOf(1),duration.decode(1));
        assertNull(duration.decode(4));assertNull(duration.decode(0x101));
        assertNull(FytProtocol.CONTROLS.get(18).decode(0));assertNull(FytProtocol.CONTROLS.get(25).decode(0));
    }

    @Test public void rzcAdditionalContractsAndDecoderIsolation()throws Exception{
        int[][] expected={{177,71,2},{190,81,2},{176,70,2},{175,74,2},{179,73,2},{193,79,4},{197,82,2}};
        for(int i=0;i<expected.length;i++){
            FytProtocol.Control c=FytProtocol.CONTROLS.get(43+i);int[] e=expected[i];
            assertEquals(e[0],c.field);assertEquals(e[2],c.options.length);
            for(int p:new int[]{0x10012a,0x11012a,0x29012a})for(int value=0;value<e[2];value++){
                FytProtocol.change(module,p,c,value);assertEquals(105,module.command);
                assertArrayEquals(new int[]{e[1],value},module.args);assertEquals(Integer.valueOf(value),c.decode(value));
            }
            assertNull(c.decode(-1));assertNull(c.decode(e[2]));assertNull(c.decode(0x101));
            for(int p:new int[]{0x40141,0x6012a,0x28012a,0x12012a}){
                try{FytProtocol.change(module,p,c,0);fail("Wrong profile accepted");}catch(IllegalArgumentException expectedFailure){}
            }
        }
    }
    @Test public void rzcMirrorRequiresRealMatchingFeedback()throws Exception{
        client.connect();await(()->module.fields.contains(1000));module.emit(1000,0x10012a);
        await(()->module.fields.size()==60);FytProtocol.Control mirror=FytProtocol.CONTROLS.get(43);
        assertFalse(client.editable(mirror));client.change(mirror,1,true);assertEquals(0,module.writes);
        module.emit(177,0);await(()->client.editable(mirror));client.change(mirror,1,true);
        await(()->module.writes==1);assertTrue(client.busy());assertEquals(Integer.valueOf(0),client.values.get(177));
        module.emit(177,0x101);await(()->!client.values.containsKey(177));assertTrue(client.busy());
        module.emit(177,1);await(()->!client.busy());assertEquals(Integer.valueOf(1),client.values.get(177));
    }
}
