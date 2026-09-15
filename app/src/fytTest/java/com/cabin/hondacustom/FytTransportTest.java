package com.cabin.hondacustom;

import android.os.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Isolated wire fixtures, based on the documented Joying x/c$a dispatch contract. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FytTransportTest {
    private Binder remote(String descriptor, boolean handled, RuntimeException failure) {
        return new Binder() {
            {attachInterface(null,descriptor);}
            @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags) {
                data.enforceInterface(descriptor);assertEquals(0,flags);
                if(failure!=null)reply.writeException(failure);
                return handled;
            }
        };
    }
    @Test public void joyingVoidOperationsAcceptEmptyReplies()throws Exception {
        Binder remote=remote(FytProtocol.MODULE,true,null);
        FytProtocol.register(remote,new Binder(),1000,true);
        FytProtocol.register(remote,new Binder(),1000,false);
        FytProtocol.change(remote,0x40141,FytProtocol.CONTROLS.get(9),1);
    }
    @Test public void emptyToolkitReplyIsRejected() {
        assertThrows(RemoteException.class,()->FytProtocol.module(remote(FytProtocol.TOOLKIT,true,null)));
    }
    @Test public void emptyReadReplyIsRejected() {
        assertThrows(RemoteException.class,()->FytProtocol.exchange(remote(FytProtocol.MODULE,true,null),
                FytProtocol.MODULE,2,p->{},p->null));
    }
    @Test public void emptyUnknownOperationReplyIsRejected() {
        assertThrows(RemoteException.class,()->FytProtocol.exchange(remote(FytProtocol.MODULE,true,null),
                FytProtocol.MODULE,5,p->{},p->null));
    }
    @Test public void unhandledCommandFailsWithoutRetry() {
        final int[] sends={0};
        Binder remote=new Binder(){
            {attachInterface(null,FytProtocol.MODULE);}
            @Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags){sends[0]++;return false;}
        };
        assertThrows(RemoteException.class,()->FytProtocol.change(remote,0x40141,FytProtocol.CONTROLS.get(9),1));
        assertEquals(1,sends[0]);
    }
    @Test public void nonemptyExceptionIsNotIgnored() {
        assertThrows(SecurityException.class,()->FytProtocol.register(
                remote(FytProtocol.MODULE,true,new SecurityException("Denied")),new Binder(),1000,true));
    }
    @Test public void wrongInterfaceIsRejected() {
        assertThrows(RemoteException.class,()->FytProtocol.register(
                remote(FytProtocol.TOOLKIT,true,null),new Binder(),1000,true));
    }
}
