package com.cabin.hondacustom;

import android.content.*;
import android.content.pm.ResolveInfo;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

final class FytClient {
    static final long FRESH_MS=30000, TIMEOUT_MS=8000;
    final Map<Integer,Integer> values=new HashMap<>();
    private final Map<Integer,Long> received=new HashMap<>();
    private final Context context; private final Runnable observer;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile Session session;
    private volatile int profile;
    private boolean closed;
    private int lastProfile;
    private final List<String> trace=new ArrayList<>();
    private volatile Write pending;
    private volatile Object pendingPacket;
    private static final class Write {
        final FytProtocol.Control control; final int value; final long baselineTime;
        volatile boolean started;
        boolean accepted; Integer feedback;
        Write(FytProtocol.Control control,int value,long baselineTime){this.control=control;this.value=value;this.baselineTime=baselineTime;}
    }
    String status;
    FytClient(Context context,Runnable observer){this.context=context;this.observer=observer;status=message(R.string.fyt_disconnected);}
    private String message(int id,Object... args){return context.getString(id,args);}
    private String family(int value){return FytProtocol.supported(value)?FytProtocol.family(value):message(R.string.fyt_unmapped_family);}
    boolean connected(){return session!=null;}
    int profile(){return profile;}
    boolean busy(){return pending!=null||pendingPacket!=null;}
    Object connectionToken(){return session;}
    boolean canSendPacket(){Session s=session;return !closed&&s!=null&&s.ready&&s.module!=null&&FytProtocol.xp(profile)&&!busy();}
    private final class Session implements ServiceConnection {
        final ExecutorService worker=FytClient.this.worker;
        volatile IBinder module; boolean bound,ready;
        final List<Integer> registered=new ArrayList<>();
        final IBinder callback=new Binder(){
            {attachInterface(null,FytProtocol.CALLBACK);}
            @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
                if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(FytProtocol.CALLBACK);return true;}
                if(code!=1)return super.onTransact(code,data,reply,flags);
                data.enforceInterface(FytProtocol.CALLBACK);
                if(data.dataAvail()<8||data.dataAvail()>4096)return false;
                int field=data.readInt(),count=data.readInt();
                if(count>16||count< -1||count>data.dataAvail()/4)return false;
                if(count>0){int raw=data.readInt();long arrived=SystemClock.elapsedRealtime();
                    Write request=pending;
                    // Record ownership at callback arrival, not when the UI drains its queue.
                    Write observed=request!=null&&request.started?request:null;
                    main.post(()->update(Session.this,field,raw,arrived,observed));}
                if(reply!=null)reply.writeNoException();return true;
            }
        };
        @Override public void onServiceConnected(ComponentName name,IBinder toolkit){
            if(session!=this)return;
            worker.execute(()->{try{
                module=FytProtocol.module(toolkit);
                if(module==null)throw new RemoteException(message(R.string.fyt_module_missing));
                if(session!=this)return;
                registered.add(FytProtocol.PROFILE);FytProtocol.register(module,callback,FytProtocol.PROFILE,true);
            }catch(Exception e){main.post(()->connectionFailed(this,message(R.string.fyt_connection_failed,e.getMessage())));}});
        }
        @Override public void onServiceDisconnected(ComponentName name){fail(this,message(R.string.fyt_service_disconnected));}
        @Override public void onBindingDied(ComponentName name){fail(this,message(R.string.fyt_binding_ended));}
        @Override public void onNullBinding(ComponentName name){connectionFailed(this,message(R.string.fyt_service_unavailable));}
    }
    void connect(){
        if(closed)return;trace.clear();lastProfile=0;disconnect();bindRoute();
    }
    private void bindRoute(){
        if(closed)return;
        Session owner=new Session();session=owner;status=message(R.string.fyt_connecting);observer.run();
        Intent intent=new Intent("com.syu.ms.toolkit").setPackage("com.syu.ms");
        ResolveInfo resolved=null;
        try{resolved=context.getPackageManager().resolveService(intent,0);}catch(RuntimeException ignored){}
        ComponentName target=new ComponentName("com.syu.ms","app.ToolkitService");
        if(resolved!=null&&resolved.serviceInfo!=null&&"com.syu.ms".equals(resolved.serviceInfo.packageName)&&resolved.serviceInfo.exported&&resolved.serviceInfo.enabled)
            target=new ComponentName(resolved.serviceInfo.packageName,resolved.serviceInfo.name);
        intent.setComponent(target);
        trace.add(message(R.string.fyt_bind_trace,intent.getAction(),target.flattenToShortString()));
        try{owner.bound=context.bindService(intent,owner,Context.BIND_AUTO_CREATE);
            // A synchronous binding callback may already have disconnected this owner.
            if(session!=owner){releaseBinding(owner);return;}
            if(!owner.bound){connectionFailed(owner,message(R.string.fyt_unit_unavailable));return;}
        }catch(RuntimeException e){connectionFailed(owner,message(R.string.fyt_connection_denied,e.getMessage()));return;}
        main.postDelayed(()->{if(session==owner&&!owner.ready)connectionFailed(owner,message(R.string.fyt_discovery_timeout));},TIMEOUT_MS);
    }
    private void connectionFailed(Session owner,String message){
        if(session!=owner)return;
        fail(owner,message);
    }
    private void update(Session owner,int field,int raw,long arrived,Write observed){
        if(session!=owner)return;
        if(field==FytProtocol.PROFILE){
            if(profile==raw&&profile!=0)return;
            if(profile!=0&&profile!=raw){fail(owner,message(R.string.fyt_profile_changed));return;}
            profile=raw;lastProfile=raw;
            trace.add(message(R.string.fyt_profile_trace,raw,raw,family(raw)));
            if(!FytProtocol.supported(raw)){owner.ready=true;status=message(R.string.fyt_unmapped_status,raw);observer.run();return;}
            status=message(R.string.fyt_loading,raw);observer.run();
            if(session!=owner||closed)return;
            owner.worker.execute(()->{try{
                // Subscribe to future setting events only; cached startup zeros are not vehicle feedback.
                for(FytProtocol.Control c:FytProtocol.CONTROLS){if(session!=owner)return;if(!FytProtocol.visible(raw,c))continue;owner.registered.add(c.field);FytProtocol.register(owner.module,owner.callback,c.field,true);}
                main.post(()->{if(session==owner){owner.ready=true;status=message(R.string.fyt_connected,raw);observer.run();}});
            }catch(Exception e){main.post(()->connectionFailed(owner,message(R.string.fyt_settings_failed,e.getMessage())));}});
            return;
        }
        // Manual packets have no verified response contract. Do not attribute events to them.
        if(pendingPacket!=null)return;
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(c.field==field&&FytProtocol.visible(profile,c)){
            Integer value=c.decode(raw);
            if(value==null){values.remove(field);received.remove(field);}else{values.put(field,value);received.put(field,arrived);}
            if(observed!=null&&pending==observed&&observed.control==c){observed.feedback=value;finishIfConfirmed(observed);}
            observer.run();
            main.postDelayed(()->{
                if(session!=owner)return;
                Long time=received.get(field);
                if(time!=null&&SystemClock.elapsedRealtime()-time>=FRESH_MS){values.remove(field);received.remove(field);observer.run();}
            },FRESH_MS+1);return;
        }
    }
    Integer value(FytProtocol.Control c){
        Long time=received.get(c.field);
        return session!=null&&FytProtocol.visible(profile,c)&&time!=null&&SystemClock.elapsedRealtime()-time<FRESH_MS?values.get(c.field):null;
    }
    boolean editable(FytProtocol.Control c){
        Session s=session;Long time=received.get(c.field);
        return s!=null&&s.ready&&s.module!=null&&!busy()&&FytProtocol.visible(profile,c)&&values.containsKey(c.field)&&time!=null&&SystemClock.elapsedRealtime()-time<FRESH_MS;
    }
    void sendPacket(byte[] input,boolean parked,Object expectedConnection){
        if(!parked||!canSendPacket()||session!=expectedConnection)return;
        XpPacket.unsigned(input);final byte[] bytes=input.clone();
        final Session owner=session;final int expectedProfile=profile;
        final Object request=new Object();final long deadline=SystemClock.elapsedRealtime()+TIMEOUT_MS;
        pendingPacket=request;values.clear();received.clear();
        status=message(R.string.xp_packet_sending);trace.add(message(R.string.xp_packet_requested,XpPacket.hex(bytes)));observer.run();
        if(closed||session!=owner||pendingPacket!=request)return;
        owner.worker.execute(()->{
            if(session!=owner||pendingPacket!=request)return;
            try{
                FytProtocol.sendXpPacket(owner.module,expectedProfile,bytes,()->{
                    if(closed||session!=owner||profile!=expectedProfile||pendingPacket!=request||SystemClock.elapsedRealtime()>=deadline)
                        throw new IllegalStateException(message(R.string.fyt_request_expired));
                });
                main.post(()->{
                    if(session!=owner||pendingPacket!=request)return;
                    // Start any later editing with a new session and new live observations.
                    disconnect();status=message(R.string.xp_packet_dispatched);trace.add(status);observer.run();
                });
            }catch(Exception e){main.post(()->fail(owner,message(R.string.xp_packet_failed,e.getMessage())));}
        });
        main.postDelayed(()->{if(session==owner&&pendingPacket==request)fail(owner,message(R.string.xp_packet_timeout));},TIMEOUT_MS);
    }
    void change(FytProtocol.Control c,int value,boolean parked){
        if(!parked||!editable(c)||value<0||value>=c.options.length)return;
        final Session owner=session;final int expectedProfile=profile;
        final Write request=new Write(c,value,received.get(c.field));
        pending=request;status=message(R.string.fyt_sending,FytText.label(context,c.title));observer.run();
        if(closed||session!=owner||pending!=request)return;
        owner.worker.execute(()->{
            if(session!=owner||profile!=expectedProfile||pending!=request)return;
            if(SystemClock.elapsedRealtime()-request.baselineTime>=FRESH_MS){main.post(()->fail(owner,message(R.string.fyt_value_expired)));return;}
            try{FytProtocol.change(owner.module,expectedProfile,c,value,()->{
                    // Descriptor lookup is also IPC and may block. Recheck immediately before dispatch.
                    if(session!=owner||profile!=expectedProfile||pending!=request||SystemClock.elapsedRealtime()-request.baselineTime>=FRESH_MS)
                        throw new IllegalStateException(message(R.string.fyt_request_expired));
                    request.started=true;
                });
                main.post(()->{if(session==owner&&pending==request){request.accepted=true;status=message(R.string.fyt_wait_feedback,FytText.label(context,c.title));finishIfConfirmed(request);observer.run();}});
            }catch(Exception e){main.post(()->fail(owner,message(R.string.fyt_write_failed,e.getMessage())));}
        });
        main.postDelayed(()->{if(session==owner&&pending==request)fail(owner,message(R.string.fyt_no_matching_feedback));},TIMEOUT_MS);
    }
    private void finishIfConfirmed(Write request){
        if(pending==request&&request.accepted&&request.feedback!=null&&request.feedback==request.value){pending=null;status=message(R.string.fyt_confirmed,FytText.label(context,request.control.title));}
    }
    private void releaseBinding(Session owner){if(owner.bound){owner.bound=false;try{context.unbindService(owner);}catch(RuntimeException ignored){}}}
    private void fail(Session owner,String message){if(session!=owner)return;trace.add(message);disconnect();status=message;observer.run();}
    String report(){
        StringBuilder out=new StringBuilder(message(R.string.fyt_title)).append('\n');
        out.append(message(R.string.fyt_report_unit,Build.MANUFACTURER,Build.MODEL,Build.VERSION.RELEASE));
        try{android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo("com.syu.ms",0);
            out.append(message(R.string.fyt_report_service,info.versionName,info.versionCode));
        }catch(Exception e){out.append(message(R.string.fyt_package_missing));}
        out.append(message(R.string.fyt_report_status,status,lastProfile,lastProfile,family(lastProfile)));
        for(String line:trace)out.append(line).append('\n');
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(profile,c))
            out.append(FytText.label(context,c.title)).append(" [").append(c.field).append("]: ")
                .append(value(c)!=null?FytText.label(context,c.options[value(c)]):message(R.string.fyt_no_feedback)).append('\n');
        return out.toString();
    }
    void disconnect(){
        boolean unconfirmed=busy();
        Session old=session;session=null;profile=0;pending=null;pendingPacket=null;values.clear();received.clear();main.removeCallbacksAndMessages(null);
        if(old!=null){
            releaseBinding(old);
            old.worker.execute(()->{if(old.module!=null)for(int field:old.registered)try{FytProtocol.register(old.module,old.callback,field,false);}catch(Exception ignored){}});
        }
        status=unconfirmed?message(R.string.fyt_disconnected_unconfirmed):message(R.string.fyt_disconnected);observer.run();
    }
    void destroy(){if(closed)return;closed=true;disconnect();worker.shutdown();}
}
