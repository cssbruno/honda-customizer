package com.cabin.hondacustom;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Updates the application only. Never accesses FYT or changes vehicle state. */
final class AppUpdater {
    static final String REPO="https://github.com/cssbruno/honda-customizer/";
    static final String API="https://api.github.com/repos/cssbruno/honda-customizer/releases/latest";
    private final Activity activity;
    private final Button button;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private boolean busy;
    private File ready;
    static final class Release {
        final String version,url,digest; final long size;
        Release(String v,String u,String d,long s){version=v;url=u;digest=d;size=s;}
    }
    AppUpdater(Activity a,Button b){activity=a;button=b;b.setOnClickListener(v->{if(ready!=null)install();else check(true);});}
    static Release parse(String json,String installed) throws Exception {
        JSONObject r=new JSONObject(json);
        if(r.getBoolean("draft")||r.getBoolean("prerelease"))return null;
        String tag=r.getString("tag_name");
        if(!tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("Invalid version");
        String version=tag.substring(1);
        if(compare(version,installed)<=0)return null;
        JSONArray assets=r.getJSONArray("assets");Release found=null;
        for(int i=0;i<assets.length();i++){
            JSONObject a=assets.getJSONObject(i);
            if(!a.getString("name").equals("Honda-Customizer-"+version+"-FYT.apk"))continue;
            String url=a.getString("browser_download_url"),digest=a.getString("digest");long size=a.getLong("size");
            if(found!=null||!url.equals(REPO+"releases/download/"+tag+"/Honda-Customizer-"+version+"-FYT.apk")
                ||!digest.matches("sha256:[0-9a-f]{64}")||size<=0||size>100*1024*1024)throw new IOException("Invalid asset");
            found=new Release(version,url,digest.substring(7),size);
        }
        if(found==null)throw new IOException("Missing APK");return found;
    }
    static int compare(String a,String b) throws IOException {
        if(!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!b.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("Invalid version");
        String[] x=a.split("\\."),y=b.split("\\.");
        for(int i=0;i<3;i++){int c=new java.math.BigInteger(x[i]).compareTo(new java.math.BigInteger(y[i]));if(c!=0)return c;}return 0;
    }
    static boolean trusted(URL u){return "https".equals(u.getProtocol())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)
        &&Arrays.asList("api.github.com","github.com","release-assets.githubusercontent.com","objects.githubusercontent.com").contains(u.getHost());}
    static HttpURLConnection connect(String address) throws Exception {
        URL url=new URL(address);
        for(int i=0;i<6;i++){
            if(!trusted(url))throw new IOException("Untrusted URL");
            HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Honda-Customizer-Updater");
            int status=c.getResponseCode();
            if(status==200)return c;
            String next=c.getHeaderField("Location");c.disconnect();
            if(status>=300&&status<400&&next!=null){url=new URL(url,next);continue;}
            throw new IOException("HTTP "+status);
        }
        throw new IOException("Too many redirects");
    }
    private void ui(Runnable r){activity.runOnUiThread(()->{if(!closed&&!activity.isFinishing())r.run();});}
    private void state(boolean value,int label){busy=value;button.setEnabled(!value);button.setText(label);}
    void check(boolean manual){
        if(busy||closed)return;
        state(true,R.string.update_checking);
        worker.execute(()->{
            try{
                PackageInfo current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
                HttpURLConnection c=connect(API);String json;
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1024*1024)throw new IOException("Oversized response");out.write(buf,0,n);}json=out.toString("UTF-8");
                }finally{c.disconnect();}
                Release release=parse(json,current.versionName);
                ui(()->{state(false,R.string.update_check);if(release!=null)new AlertDialog.Builder(activity).setTitle(R.string.update_available)
                    .setMessage(activity.getString(R.string.update_offer,release.version)).setNegativeButton(R.string.fyt_cancel,null)
                    .setPositiveButton(R.string.update_download,(d,w)->download(release)).show();
                    else if(manual)Toast.makeText(activity,R.string.update_current,Toast.LENGTH_LONG).show();});
            }catch(Exception e){ui(()->{state(false,R.string.update_retry);if(manual)Toast.makeText(activity,R.string.update_failed,Toast.LENGTH_LONG).show();});}
        });
    }
    private void download(Release release){
        if(busy||closed)return;state(true,R.string.update_downloading);
        worker.execute(()->{
            File temp=null;
            try{
                temp=File.createTempFile("update-",".apk",activity.getCacheDir());
                HttpURLConnection c=connect(release.url);MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;
                try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(temp)){
                    byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){if(closed)throw new IOException("Closed");count+=n;
                        if(count>release.size)throw new IOException("Oversized APK");out.write(buf,0,n);hash.update(buf,0,n);}
                }finally{c.disconnect();}
                StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
                if(count!=release.size||!hex.toString().equals(release.digest))throw new IOException("Checksum mismatch");
                PackageManager pm=activity.getPackageManager();
                PackageInfo apk=pm.getPackageArchiveInfo(temp.getPath(),PackageManager.GET_SIGNATURES);
                PackageInfo installed=pm.getPackageInfo(activity.getPackageName(),PackageManager.GET_SIGNATURES);
                if(!validApk(apk,installed,release.version))throw new IOException("Incompatible APK");
                File target=new File(activity.getCacheDir(),"verified-update.apk");
                if(!temp.renameTo(target))throw new IOException("Cannot stage update");
                ui(()->{ready=target;state(false,R.string.update_install);install();});
            }catch(Exception e){ui(()->{state(false,R.string.update_retry);Toast.makeText(activity,R.string.update_failed,Toast.LENGTH_LONG).show();});}
            finally{if(temp!=null)temp.delete();}
        });
    }
    static boolean validApk(PackageInfo apk,PackageInfo installed,String version){
        return apk!=null&&installed.packageName.equals(apk.packageName)&&version.equals(apk.versionName)&&apk.versionCode>installed.versionCode
            &&apk.signatures!=null&&installed.signatures!=null&&apk.signatures.length>0
            &&new HashSet<>(Arrays.asList(apk.signatures)).equals(new HashSet<>(Arrays.asList(installed.signatures)));
    }
    private void install(){
        if(ready==null)return;
        try{
            if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                new AlertDialog.Builder(activity).setMessage(R.string.update_permission).setNegativeButton(R.string.fyt_cancel,null)
                    .setPositiveButton(R.string.update_settings,(d,w)->{try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(ActivityNotFoundException e){Toast.makeText(activity,R.string.update_failed,Toast.LENGTH_LONG).show();}}).show();return;
            }
            Uri uri=Uri.parse("content://"+activity.getPackageName()+".updates/verified-update.apk");
            Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("Update",uri));activity.startActivity(intent);
        }catch(Exception e){Toast.makeText(activity,R.string.update_failed,Toast.LENGTH_LONG).show();}
    }
    void close(){closed=true;worker.shutdownNow();}
}
