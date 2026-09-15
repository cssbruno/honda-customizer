package com.cabin.hondacustom;

import android.content.pm.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.net.URL;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class AppUpdaterTest {
    private String release(){return "{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v3.4.0\",\"assets\":[{\"name\":\"Honda-Customizer-3.4.0-FYT.apk\",\"browser_download_url\":\"https://github.com/cssbruno/honda-customizer/releases/download/v3.4.0/Honda-Customizer-3.4.0-FYT.apk\",\"digest\":\"sha256:"+new String(new char[64]).replace('\0','a')+"\",\"size\":1234}]}";}
    @Test public void selectsOnlyNewStableRelease()throws Exception{
        assertEquals("3.4.0",AppUpdater.parse(release(),"3.3.0").version);
        assertNull(AppUpdater.parse(release(),"3.4.0"));assertNull(AppUpdater.parse(release(),"3.5.0"));
        assertNull(AppUpdater.parse(release().replace("\"prerelease\":false","\"prerelease\":true"),"3.3.0"));
        assertNull(AppUpdater.parse(release().replace("\"draft\":false","\"draft\":true"),"3.3.0"));
        assertTrue(AppUpdater.compare("3.10.0","3.9.0")>0);
    }
    @Test public void rejectsMissingChecksumWrongRepositoryAndInvalidSize()throws Exception{
        for(String json:new String[]{release().replace("sha256:","sha512:"),release().replace("cssbruno/","attacker/"),release().replace("1234","0"),release().replace("1234","104857601"),release().replace("-FYT.apk","-original-HU.apk")}){
            try{AppUpdater.parse(json,"3.3.0");fail("Accepted invalid release");}catch(java.io.IOException|org.json.JSONException expected){}
        }
    }
    @Test public void restrictsRedirectHostsAndRequiresHttps()throws Exception{
        assertTrue(AppUpdater.trusted(new URL("https://release-assets.githubusercontent.com/asset")));
        for(String url:new String[]{"http://github.com/x","https://github.com.evil.test/x","https://user@github.com/x","https://github.com:8443/x","https://evil.test/x"})assertFalse(AppUpdater.trusted(new URL(url)));
    }
    private PackageInfo info(int code,String signature){PackageInfo p=new PackageInfo();p.packageName="com.cabin.hondacustom";p.versionCode=code;p.versionName="3.4.0";p.signatures=new Signature[]{new Signature(signature)};return p;}
    @Test public void rejectsWrongSignerPackageAndDowngrade(){
        PackageInfo installed=info(8,"aabb"),apk=info(9,"aabb");assertTrue(AppUpdater.validApk(apk,installed,"3.4.0"));
        assertFalse(AppUpdater.validApk(info(9,"ccdd"),installed,"3.4.0"));assertFalse(AppUpdater.validApk(info(8,"aabb"),installed,"3.4.0"));
        assertFalse(AppUpdater.validApk(apk,installed,"3.5.0"));apk.packageName="other";assertFalse(AppUpdater.validApk(apk,installed,"3.4.0"));
        assertFalse(AppUpdater.validApk(null,installed,"3.4.0"));
    }
    @Test public void providerRejectsTraversalAndWrites()throws Exception{
        UpdateProvider provider=Robolectric.buildContentProvider(UpdateProvider.class).create().get();
        Uri valid=Uri.parse("content://com.cabin.hondacustom.updates/verified-update.apk");
        assertEquals("application/vnd.android.package-archive",provider.getType(valid));
        try{provider.openFile(valid,"w");fail();}catch(java.io.FileNotFoundException expected){}
        try{provider.getType(Uri.parse("content://com.cabin.hondacustom.updates/../secret"));fail();}catch(IllegalArgumentException expected){}
    }
}
