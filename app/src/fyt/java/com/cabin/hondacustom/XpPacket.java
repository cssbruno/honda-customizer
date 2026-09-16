package com.cabin.hondacustom;

import java.util.Locale;

/** User-supplied XP packet body. No examples, default bytes or OEM-frame conversion. */
final class XpPacket {
    // Application input limit, not a claimed decoder maximum.
    static final int MAX_BYTES=64;
    static byte[] parse(String input){
        if(input==null||input.length()>1024)throw new IllegalArgumentException("Invalid XP packet");
        String text=input.trim();
        if(text.isEmpty())throw new IllegalArgumentException("Empty XP packet");
        String[] words=text.split("\\s+");
        if(words.length>MAX_BYTES)throw new IllegalArgumentException("XP packet too long");
        byte[] bytes=new byte[words.length];
        for(int i=0;i<words.length;i++){
            if(!words[i].matches("[0-9a-fA-F]{2}"))throw new IllegalArgumentException("Expected hexadecimal bytes");
            bytes[i]=(byte)Integer.parseInt(words[i],16);
        }
        return bytes;
    }
    static int[] unsigned(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Invalid XP packet length");
        int[] result=new int[bytes.length];
        for(int i=0;i<bytes.length;i++)result[i]=bytes[i]&255;
        return result;
    }
    static String hex(byte[] bytes){
        StringBuilder out=new StringBuilder();
        for(int value:unsigned(bytes)){
            if(out.length()>0)out.append(' ');
            out.append(String.format(Locale.ROOT,"%02X",value));
        }
        return out.toString();
    }
}
