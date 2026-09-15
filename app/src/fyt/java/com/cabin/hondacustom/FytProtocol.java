package com.cabin.hondacustom;

import android.os.*;
import java.util.*;

/** Decoder-specific contracts; see documents/FYT-IMPLEMENTATION-STATUS.md. */
final class FytProtocol {
    static final String TOOLKIT="com.syu.ipc.IRemoteToolkit", MODULE="com.syu.ipc.IRemoteModule", CALLBACK="com.syu.ipc.IModuleCallback";
    static final int PROFILE=1000;
    static final class Control {
        final String title; final int field,command,key,base;
        String category="Panel"; boolean wcOnly, rzcOnly, bnrOnly; int writeOffset; final boolean lowByte; final String[] options;
        Control(String title,int field,int key,int base,String... options){this(title,field,106,key,true,base,options);}
        Control(String title,int field,int command,int key,boolean lowByte,int base,String... options){this.title=title;this.field=field;this.command=command;this.key=key;this.lowByte=lowByte;this.base=base;this.options=options;}
        Integer decode(int raw){if(raw<0||raw>65535)return null;int v=(lowByte?(raw&255):raw)-base;return v>=0&&v<options.length?v:null;}
    }
    private static Control wcControl(String category,String title,int field,int command,int key,boolean lowByte,int base,int writeOffset,String... options){
        Control c=new Control(title,field,command,key,lowByte,base,options);
        c.category=category;c.wcOnly=true;c.writeOffset=writeOffset;return c;
    }
    private static Control rzcControl(String category,String title,int field,int key,String... options){
        Control c=new Control(title,field,105,key,false,0,options);
        c.category=category;c.rzcOnly=true;return c;
    }
    private static Control bnrControl(String category,String title,int field,int key,String... options){
        Control c=new Control(title,field,105,key,false,0,options);
        c.category=category;c.bnrOnly=true;return c;
    }
    static final List<Control> CONTROLS=Collections.unmodifiableList(Arrays.asList(
        new Control("Navigation directions",109,16,0,"Off","On"),
        new Control("Warning messages",110,15,0,"Off","On"),
        new Control("Panel configuration",111,14,0,"Type 1","Type 2","Type 3"),
        new Control("Reverse tone",88,9,0,"Off","On"),
        new Control("Speed reminders",65,6,0,"Off","On"),
        new Control("Message reminders",66,7,0,"Off","On"),
        new Control("Idle-stop reminders",67,8,0,"Off","On"),
        new Control("Eco background lighting",68,5,0,"Off","On"),
        new Control("Traffic sign display",102,13,0,"Off","On"),
        new Control("Alarm volume",69,4,1,"High","Medium","Low"),
        new Control("Trip B reset condition",70,3,1,"Refuel","Ignition off","Manual"),
        new Control("Trip A reset condition",71,2,1,"Refuel","Ignition off","Manual"),
        new Control("Outside-temperature adjustment",72,1,1,"−3","−2","−1","0","+1","+2","+3"),
        // Acrivity_RZC_17CRVSettings / AcrivitySiYuSettings; these are a separate decoder dialect.
        new Control("Speed / distance units",77,105,21,false,0,"km/h · km","mph · miles"),
        new Control("Tachometer display",78,105,22,false,0,"Off","On"),
        new Control("Tachometer setting (vendor option)",87,105,35,false,0,"Off","On"),
        wcControl("Doors","Walk-away lock",58,104,3,true,0,0,"Off","On"),
        wcControl("Doors","Remote-lock acknowledgement",60,104,1,true,0,0,"Off","On"),
        wcControl("Doors","Automatic relock time",59,104,2,true,1,0,"30 seconds","60 seconds","90 seconds"),
        wcControl("Doors","Automatic door-lock condition",87,104,4,false,0,0,"Off","Speed-linked","Shift out of P"),
        wcControl("Doors","Automatic door-unlock condition",86,104,5,false,0,0,"Off","Shift into P","Ignition off"),
        wcControl("Lighting","Wiper/headlight linkage",49,102,5,true,0,0,"Off","On"),
        wcControl("Lighting","Automatic interior-light sensitivity",50,102,4,true,0,0,"Minimum","Low","Medium","High","Maximum"),
        wcControl("Lighting","Automatic headlight sensitivity",51,102,3,true,0,0,"Minimum","Low","Medium","High","Maximum"),
        wcControl("Lighting","Automatic headlight-off delay",52,102,2,true,0,0,"0 seconds","15 seconds","30 seconds","60 seconds"),
        wcControl("Lighting","Interior-light dimming time",53,102,1,true,1,0,"15 seconds","30 seconds","60 seconds"),
        wcControl("Remote access","Keyless-access buzzer",54,103,4,true,0,0,"Off","On"),
        wcControl("Remote access","Keyless-access exterior-light acknowledgement",55,103,3,true,0,0,"Off","On"),
        wcControl("Remote access","Keyless alarm volume (vendor wording)",56,103,2,true,0,0,"Close (vendor wording)","Distant (vendor wording)"),
        wcControl("Remote access","Horn with remote start",57,103,1,true,0,0,"Off","On"),
        wcControl("Driver assistance","LKAS suspension tone",62,105,3,true,0,0,"Off","On"),
        wcControl("Driver assistance","ACC vehicle-ahead detection tone",63,105,2,true,0,0,"Off","On"),
        wcControl("Driver assistance","Lane-departure system behavior",61,105,4,true,1,0,"Middle","Wide","Warnings only"),
        wcControl("Driver assistance","Forward-collision warning distance",64,105,1,true,1,0,"Far","Middle","Near"),
        wcControl("Cameras","LaneWatch with turn signal",93,110,12,false,0,0,"Off","On"),
        wcControl("Cameras","LaneWatch duration after turn signal",94,110,12,false,0,4,"0 seconds","2 seconds"),
        wcControl("Cameras","Rear-view dynamic reminder (vendor wording)",95,110,14,false,0,0,"Off","On"),
        wcControl("Driver assistance","Rise warning (vendor wording; meaning unresolved)",96,105,8,false,0,0,"Off","On"),
        wcControl("Driver assistance","Driver attention monitor",97,105,7,false,0,0,"Off","Visual warning","Tactile and visual warnings"),
        wcControl("Seats","Seat-position memory linkage",98,106,11,false,0,0,"Off","On"),
        wcControl("Seats","Electronic preloaded seat-belt movement mode (vendor wording)",99,106,12,false,0,0,"Off","On"),
        wcControl("Driver assistance","SWITCH LOCK (vendor wording; meaning unresolved)",100,106,10,false,0,0,"Off","On"),
        wcControl("Driver assistance","Lane-departure prevention behavior",101,105,9,false,1,0,"Standard","Delay","Warning only","Advance"),
        rzcControl("Mirrors and windows","Mirror folding",177,71,"Manual","Automatic folding"),
        rzcControl("Mirrors and windows","Remote window control",190,81,"Off","On"),
        rzcControl("Doors","Tailgate sensor",176,70,"Off","On"),
        rzcControl("Driver assistance","Rear-seat reminder",175,74,"Off","On"),
        rzcControl("Driver assistance","Straight-line driving assistance",179,73,"During cruise control","Within specified speed range"),
        rzcControl("Driver assistance","Overspeed warning deviation",193,79,"+0 km/h","+5 km/h","+10 km/h","+15 km/h"),
        rzcControl("Driver assistance","Blind-zone warning",197,82,"Visual warning","Visual and audible warning"),
        rzcControl("Panel","Outside-temperature adjustment",60,0,"−5","−4","−3","−2","−1","0","+1","+2","+3","+4","+5"),
        rzcControl("Panel","Trip A reset condition",58,2,"Refuel","Ignition off","Manual"),
        rzcControl("Panel","Trip B reset condition",59,3,"Refuel","Ignition off","Manual"),
        rzcControl("Lighting","Interior-light dimming time",63,4,"15 seconds","30 seconds","60 seconds"),
        rzcControl("Lighting","Automatic headlight-off delay",62,5,"0 seconds","15 seconds","30 seconds","60 seconds"),
        rzcControl("Lighting","Automatic headlight sensitivity",61,6,"Minimum","Low","Medium","High","Maximum"),
        rzcControl("Lighting","Automatic interior-light sensitivity",73,27,"Minimum","Low","Medium","High","Maximum"),
        rzcControl("Lighting","Automatic high beam",192,76,"Off","On"),
        rzcControl("Doors","Remote-lock acknowledgement",64,10,"Off","On"),
        rzcControl("Doors","Key and remote unlock mode",65,9,"Driver door","All doors"),
        rzcControl("Doors","Automatic relock time",66,11,"30 seconds","60 seconds","90 seconds"),
        rzcControl("Doors","Automatic door-unlock condition",67,8,"All doors when driver door opens","All doors when shifted into P","All doors when ignition off","Off"),
        rzcControl("Doors","Automatic door-lock condition",68,7,"Speed-linked","Shift out of P","Off"),
        rzcControl("Remote access","Keyless-access buzzer",69,13,"Off","On"),
        rzcControl("Remote access","Remote-start system",70,24,"Off","On"),
        rzcControl("Doors","Door unlock mode",71,25,"All doors","Driver door"),
        rzcControl("Remote access","Keyless-access exterior-light acknowledgement",72,26,"Off","On"),
        rzcControl("Doors","Lockout Preset (vendor wording; meaning unresolved)",191,75,"Off","On"),
        rzcControl("Panel","Alarm volume",74,18,"High","Medium","Low"),
        rzcControl("Panel","Eco background lighting",75,19,"Off","On"),
        rzcControl("Panel","New-message notifications",76,20,"Off","On"),
        rzcControl("Doors","Walk-away lock",79,23,"Off","On"),
        rzcControl("Lighting","Wiper/headlight linkage",80,28,"Off","On"),
        rzcControl("Panel","Voice alarm system volume (vendor wording)",81,30,"Low","High"),
        rzcControl("Driver assistance","Energy-saving automatic start/stop (vendor wording)",82,29,"Off","On"),
        rzcControl("Driver assistance","ACC vehicle-ahead detection tone",83,32,"Off","On"),
        rzcControl("Driver assistance","LKAS suspension tone",84,33,"Off","On"),
        rzcControl("Driver assistance","Forward-collision warning distance",85,31,"Far","Middle","Near"),
        rzcControl("Driver assistance","Lane-departure system behavior",86,34,"Middle","Wide","Warnings only"),
        rzcControl("Driver assistance","Driver attention monitor",114,36,"Off","Visual warning","Tactile and visual warnings"),
        rzcControl("Doors","Electric tailgate remote-opening condition",110,37,"Any time","After unlocking"),
        rzcControl("Doors","Electric tailgate external-handle opening",111,38,"Off","On"),
        rzcControl("Driver assistance","Traffic-sign small icon (vendor wording)",151,39,"Off","On"),
        rzcControl("Driver assistance","Rise warning (vendor wording; meaning unresolved)",152,40,"Off","On"),
        rzcControl("Seats","Seat-position memory linkage",153,41,"Off","On"),
        rzcControl("Seats","Electronic preloaded seat-belt movement mode (vendor wording)",154,42,"Off","On"),
        rzcControl("Panel","Turning-point guide sign (vendor wording)",178,72,"Off","On"),
        rzcControl("Driver assistance","Traffic-sign recognition (vendor option 1)",194,77,"Off","On"),
        rzcControl("Driver assistance","Traffic-sign recognition (vendor option 2)",195,78,"Off","On"),
        rzcControl("Cameras","Static reversing guidelines",155,43,"Off","On"),
        rzcControl("Cameras","Dynamic reversing guidelines",156,44,"Off","On"),
        rzcControl("Cameras","Show camera after reversing",157,45,"Off","On"),
        rzcControl("Cameras","Reversing parking-space width",158,46,"Narrow","Wide"),
        rzcControl("Cameras","Rear-view dynamic reminder (vendor wording)",159,47,"Off","On"),
        rzcControl("Cameras","Rear multifunctional system (vendor wording)",161,49,"Off","On"),
        rzcControl("Cameras","Reverse tone",109,50,"Off","On"),
        rzcControl("Cameras","Reversing delay",196,97,"Off","On"),
        rzcControl("Doors","Automatic trunk opening (vendor wording)",166,51,"Off","On"),
        rzcControl("Seats","Entry/exit seat movement",173,52,"Off","On"),
        bnrControl("Panel","Trip A reset condition",58,2,"Refuel","Ignition off","Manual"),
        bnrControl("Panel","Trip B reset condition",59,3,"Refuel","Ignition off","Manual"),
        bnrControl("Panel","Outside-temperature adjustment",60,0,"−5","−4","−3","−2","−1","0","+1","+2","+3","+4","+5"),
        bnrControl("Lighting","Automatic headlight sensitivity",61,6,"Minimum","Low","Medium","High","Maximum"),
        bnrControl("Lighting","Automatic headlight-off delay",62,5,"0 seconds","15 seconds","30 seconds","60 seconds"),
        bnrControl("Lighting","Interior-light dimming time",63,4,"15 seconds","30 seconds","60 seconds"),
        bnrControl("Doors","Remote-lock acknowledgement",64,10,"Off","On"),
        bnrControl("Doors","Key and remote unlock mode (vendor option)",65,9,"Off","On"),
        bnrControl("Doors","Automatic relock time",66,11,"30 seconds","60 seconds","90 seconds"),
        bnrControl("Doors","Automatic door-unlock condition",67,8,"All doors when driver door opens","All doors when shifted into P","All doors when ignition off","Off"),
        bnrControl("Doors","Automatic door-lock condition",68,7,"Speed-linked","Shift out of P","Off"),
        bnrControl("Remote access","Keyless-access buzzer",69,13,"Off","On"),
        bnrControl("Remote access","Remote-start system",70,24,"Off","On"),
        bnrControl("Doors","Door unlock mode",71,25,"All doors","Driver door"),
        bnrControl("Remote access","Keyless-access exterior-light acknowledgement",72,26,"Off","On"),
        bnrControl("Lighting","Automatic interior-light sensitivity",73,27,"Minimum","Low","Medium","High","Maximum"),
        bnrControl("Panel","Alarm volume",74,18,"High","Medium","Low"),
        bnrControl("Panel","Eco background lighting",75,19,"Off","On"),
        bnrControl("Panel","New-message notifications",76,20,"Off","On"),
        bnrControl("Doors","Walk-away lock",79,23,"Off","On"),
        bnrControl("Lighting","Wiper/headlight linkage",80,28,"Off","On"),
        bnrControl("Panel","Voice alarm system volume (vendor wording)",81,30,"Low","High"),
        bnrControl("Driver assistance","Energy-saving automatic start/stop (vendor wording)",82,29,"Off","On"),
        bnrControl("Driver assistance","ACC vehicle-ahead detection tone",83,32,"Off","On"),
        bnrControl("Driver assistance","LKAS suspension tone",84,33,"Off","On"),
        bnrControl("Driver assistance","Forward-collision warning distance",85,31,"Far","Middle","Near"),
        bnrControl("Driver assistance","Lane-departure system behavior",86,34,"Middle","Wide","Warnings only"),
        bnrControl("Cameras","Reverse tone",109,36,"Off","On"),
        bnrControl("Doors","Electric tailgate remote-opening condition",110,37,"Any time","After unlocking"),
        bnrControl("Doors","Electric tailgate external-handle opening",111,38,"Off","On"),
        bnrControl("Seats","Seat-position memory linkage",112,39,"Off","On"),
        bnrControl("Seats","Entry/exit seat movement",113,40,"Off","On"),
        bnrControl("Driver assistance","Driver attention monitor",114,36,"Off","Visual warning","Tactile and visual warnings"),
        bnrControl("Panel","Fatigue-driving information",149,42,"Off","On"),
        bnrControl("Panel","AWD information",150,43,"Off","On")
    ));
    static boolean wc(int p){return p==0x40141||p==0x50141||p==0x60141||p==0xB0141||p==0xC0141||p==0xD0141;}
    static boolean rzc(int p){return p==0x10012a||p==0x11012a||p==0x29012a;}
    static boolean bnr(int p){return p==0x6012a||p==0x7012a||p==0x8012a||p==0x9012a||p==0xa012a||p==0xb012a||p==0xf012a||p==0x28012a;}
    static boolean supported(int p){return wc(p)||rzc(p)||bnr(p);}
    static String family(int p){return wc(p)?"WC":rzc(p)?"RZC":bnr(p)?"BNR":"Unmapped";}
    static boolean visible(int p,Control c){
        if(!supported(p)||!CONTROLS.contains(c))return false;
        if(c.bnrOnly){
            if(!bnr(p))return false;
            boolean guandao=p==0x8012a||p==0x9012a||p==0xa012a||p==0xb012a;
            // AcrivitySiYuSettings.onResume + layout defaults, independently audited.
            switch(c.field){
                case 65:case 67:case 68:case 71:case 149:case 150:return p==0xf012a;
                case 83:case 84:return p!=0xf012a;
                case 109:case 112:case 113:return guandao;
                case 110:case 111:return guandao||p==0xf012a;
                default:return true;
            }
        }
        if(c.rzcOnly)return rzc(p);
        if(c.wcOnly){
            if(!wc(p))return false;
            return !(c.command==102&&c.key>=3&&(p==0x50141||p==0x60141));
        }
        if(c.command==105)return rzc(p)||(bnr(p)&&c.field!=77);
        if(!wc(p))return false;
        if(c.key>=14)return p==0x40141||p==0xC0141||p==0xD0141;
        if(c.key==9)return p==0x50141||p==0x60141||p==0xB0141;
        if(c.key==6||c.key==7)return p!=0x50141&&p!=0x60141;
        if(c.key==13)return p==0xB0141;
        return true;
    }
    interface Writer {void write(Parcel p);}
    interface Reader<T> {T read(Parcel p);}
    static <T>T exchange(IBinder binder,String descriptor,int code,Writer writer,Reader<T> reader)throws RemoteException{
        return exchange(binder,descriptor,code,writer,reader,()->{});
    }
    private static <T>T exchange(IBinder binder,String descriptor,int code,Writer writer,Reader<T> reader,Runnable beforeSend)throws RemoteException{
        if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new RemoteException("Unexpected FYT interface");
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{data.writeInterfaceToken(descriptor);writer.write(data);
            beforeSend.run();
            if(!binder.transact(code,data,reply,0)||reply.dataSize()>65536||reply.dataAvail()<4)throw new RemoteException("Invalid FYT reply");
            reply.readException();return reader.read(reply);
        }finally{data.recycle();reply.recycle();}
    }
    static IBinder module(IBinder toolkit)throws RemoteException{return exchange(toolkit,TOOLKIT,1,p->p.writeInt(7),Parcel::readStrongBinder);}
    static void register(IBinder module,IBinder callback,int field,boolean add)throws RemoteException{
        exchange(module,MODULE,add?3:4,p->{p.writeStrongBinder(callback);p.writeInt(field);if(add)p.writeInt(1);},p->null);
    }
    static void change(IBinder module,int profile,Control c,int value)throws RemoteException{
        change(module,profile,c,value,()->{});
    }
    static void change(IBinder module,int profile,Control c,int value,Runnable beforeSend)throws RemoteException{
        if(!visible(profile,c)||value<0||value>=c.options.length)throw new IllegalArgumentException("Unsupported FYT setting");
        exchange(module,MODULE,1,p->{p.writeInt(c.command);p.writeIntArray(new int[]{c.bnrOnly&&c.field==114&&profile==0xf012a?41:c.key,value+c.base+c.writeOffset});p.writeFloatArray(null);p.writeStringArray(null);},p->null,beforeSend);
    }
}
