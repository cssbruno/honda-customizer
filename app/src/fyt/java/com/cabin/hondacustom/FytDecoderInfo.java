package com.cabin.hondacustom;

import android.os.Parcel;

/** FYT field 1005: service-held decoder version text, not vehicle feedback. */
final class FytDecoderInfo {
    static final int VERSION=1005;
    static String readVersion(Parcel data){
        // i1/v.j sends null int/float arrays and a single string. Accept empty
        // arrays as absent too, but never reinterpret a numeric callback.
        for(int i=0;i<2;i++){
            if(data.dataAvail()<4)throw new IllegalArgumentException("Truncated version callback");
            int count=data.readInt();
            if(count!= -1&&count!=0)throw new IllegalArgumentException("Unexpected version arrays");
        }
        if(data.dataAvail()<4)throw new IllegalArgumentException("Missing version strings");
        int count=data.readInt();String value=null;
        if(count==1){
            if(data.dataAvail()<4)throw new IllegalArgumentException("Missing version text");
            value=data.readString();
            if(value!=null&&value.length()>256)throw new IllegalArgumentException("Invalid version length");
        }else if(count!=0&&count!= -1)throw new IllegalArgumentException("Unexpected version strings");
        if(data.dataAvail()!=0)throw new IllegalArgumentException("Unexpected version payload");
        if(value==null||value.trim().isEmpty())return null;
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))
            throw new IllegalArgumentException("Invalid version text");
        return value;
    }
}
