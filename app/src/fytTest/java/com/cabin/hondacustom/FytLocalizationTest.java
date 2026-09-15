package com.cabin.hondacustom;

import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FytLocalizationTest {
    @Test public void everyControlAndOptionHasEnglishAndPortugueseResources(){
        Context app=RuntimeEnvironment.getApplication();
        for(Locale locale:new Locale[]{Locale.US,new Locale("pt","BR")}){
            Configuration config=new Configuration(app.getResources().getConfiguration());config.setLocale(locale);
            Context context=app.createConfigurationContext(config);
            for(FytProtocol.Control c:FytProtocol.CONTROLS){
                assertFalse(FytText.label(context,c.title).isEmpty());
                assertFalse(FytText.label(context,c.category).isEmpty());
                String[] options=FytText.options(context,c);assertEquals(c.options.length,options.length);
                for(String option:options)assertFalse(option.isEmpty());
            }
            assertEquals(locale.getLanguage().equals("pt")?"Desativado":"Off",FytText.label(context,"Off"));
        }
    }
    @Test @Config(qualifiers="pt-rBR") public void portugueseMainScreenExplainsRealConnectionRequirement(){
        MainActivity activity=Robolectric.buildActivity(MainActivity.class).setup().get();
        try{
            View root=activity.getWindow().getDecorView();
            assertNotNull(find(root,"Conectar ao FYT"));assertNotNull(find(root,"Relatório"));
            assertNotNull(find(root,"Desconectado"));assertNotNull(find(root,"Estou estacionado"));
            assertNotNull(find(root,"Apenas configurações FYT verificadas"));
            assertNull(find(root,"Connect to FYT"));
        }finally{activity.finish();}
    }
    @Test @Config(qualifiers="pt-rBR") public void portugueseFailureDoesNotCreateVehicleValuesOrFallback(){
        final int[] binds={0};
        Context context=new ContextWrapper(RuntimeEnvironment.getApplication()){
            @Override public boolean bindService(Intent intent,ServiceConnection connection,int flags){binds[0]++;return false;}
        };
        FytClient client=new FytClient(context,()->{});
        try{
            client.connect();assertEquals(1,binds[0]);assertFalse(client.connected());assertTrue(client.values.isEmpty());
            assertEquals("Serviço FYT indisponível nesta central",client.status);
            assertTrue(client.report().contains("Último perfil"));
        }finally{client.destroy();}
    }
    private View find(View view,String text){
        if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            View found=find(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;
        }
        return null;
    }
}
