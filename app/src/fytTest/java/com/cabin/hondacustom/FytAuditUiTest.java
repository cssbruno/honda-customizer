package com.cabin.hondacustom;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="pt-rBR")
public class FytAuditUiTest {
    private View find(View root,String text){
        if(root.getVisibility()!=View.VISIBLE)return null;
        if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){
            View found=find(group.getChildAt(i),text);if(found!=null)return found;
        }}
        return null;
    }
    private void assertFullScreen(View root,int width,int height){
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        root.layout(0,0,width,height);
        View title=find(root,"Auditoria FYT");
        View audit=(View)title.getParent().getParent();
        View container=(View)audit.getParent();
        assertTrue(audit instanceof FytAuditView);assertTrue(audit.getWidth()>0);assertTrue(audit.getHeight()>0);
        assertEquals(container.getWidth(),audit.getWidth());assertEquals(container.getHeight(),audit.getHeight());
    }

    @Test public void auditOpensOnScreenWhileDisconnectedAndBackReturnsToControls(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();
            find(root,"Auditoria FYT").performClick();
            assertFullScreen(root,480,800);
            assertNotNull(find(root,"Central e decodificador"));
            assertNotNull(find(root,"nenhum perfil recebido"));
            assertNotNull(find(root,"Indisponível — desconectado"));
            assertNull(find(root,"Conectar ao FYT"));
            activity.onBackPressed();assertNotNull(find(root,"Conectar ao FYT"));
        }finally{controller.pause().stop().destroy();}
    }

    @Test @Config(qualifiers="pt-rBR-w1024dp-h600dp-land")
    public void liveAuditAndCopyUseLatestFeedbackAndOfferExplicitDataRequest()throws Exception{
        FytClientTest fixture=new FytClientTest();fixture.setup();
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            fixture.xpReady();MainActivity activity=controller.get();
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("client");field.setAccessible(true);
            ((FytClient)field.get(activity)).destroy();field.set(activity,fixture.client);
            View root=activity.getWindow().getDecorView();find(root,"Auditoria FYT").performClick();
            assertEquals(1,fixture.module.reads); // Opening the audit does not send another request.
            assertFullScreen(root,1024,600);
            assertNotNull(find(root,"0 de 22"));
            fixture.module.emit(78,1);FytAuditTest.version(fixture.module.callback,"screen-fixture");
            ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
            assertNotNull(find(root,"1 de 22"));assertNotNull(find(root,"screen-fixture"));
            assertNotNull(find(root,"[78]: Ativado"));
            // A reply arriving between display refreshes must still reach the clipboard.
            FytAuditTest.version(fixture.module.callback,"copy-fixture");ShadowLooper.idleMainLooper();
            find(root,"Copiar").performClick();
            ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            String copied=clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            assertTrue(copied.contains("copy-fixture"));assertFalse(copied.contains("screen-fixture"));
            assertNotNull(find(root,"Leituras do veículo"));
            ShadowLooper.idleMainLooper(30,TimeUnit.SECONDS);
            assertNotNull(find(root,"0 de 22"));assertNotNull(find(root,"retorno vencido"));
            find(root,"Solicitar dados FYT").performClick();
            fixture.await(()->fixture.module.reads==2);
            ShadowLooper.idleMainLooper(1,TimeUnit.SECONDS);
            assertFalse(find(root,"Solicitar dados FYT").isEnabled());
            ShadowLooper.idleMainLooper(8,TimeUnit.SECONDS);
            assertTrue(find(root,"Solicitar dados FYT").isEnabled());
            assertNotNull(find(root,"nenhum retorno válido dos ajustes observado"));
            find(root,"Voltar").performClick();assertTrue(fixture.client.connected());
            assertEquals(1,fixture.bindings);assertEquals(0,fixture.module.writes);
        }finally{controller.pause().stop().destroy();fixture.close();}
    }
}
