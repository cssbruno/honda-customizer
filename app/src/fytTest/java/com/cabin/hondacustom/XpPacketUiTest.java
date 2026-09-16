package com.cabin.hondacustom;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="pt-rBR")
public class XpPacketUiTest {
    private View find(View root,Class<?> type,String text){
        if(type.isInstance(root)&&(text==null||root instanceof TextView&&text.contentEquals(((TextView)root).getText())))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View v=find(group.getChildAt(i),type,text);if(v!=null)return v;}}
        return null;
    }
    private void exercise(boolean cancelParked)throws Exception{
        FytClientTest fixture=new FytClientTest();fixture.setup();
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity=controller.get();
        try{
            fixture.xpReady();java.lang.reflect.Field field=MainActivity.class.getDeclaredField("client");field.setAccessible(true);
            ((FytClient)field.get(activity)).destroy();field.set(activity,fixture.client);
            CheckBox parked=(CheckBox)find(activity.getWindow().getDecorView(),CheckBox.class,null);parked.setChecked(true);
            View button=find(activity.getWindow().getDecorView(),Button.class,"Pacote XP manual");assertNotNull(button);assertTrue(button.isEnabled());button.performClick();
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
            AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();
            EditText input=(EditText)find(editor.getWindow().getDecorView(),EditText.class,null);assertEquals("",input.getText().toString());
            editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertNotNull(input.getError());assertEquals(0,fixture.module.writes);
            input.setText("C6 02 16 01");editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(editor,confirmation);assertEquals(0,fixture.module.writes);
            assertNotNull(find(confirmation.getWindow().getDecorView(),TextView.class,activity.getString(R.string.xp_packet_confirm,"C6 02 16 01")));
            if(cancelParked)parked.setChecked(false);
            confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.shadows.ShadowLooper.idleMainLooper();
            if(cancelParked){assertEquals(0,fixture.module.writes);assertTrue(fixture.client.connected());}
            else{fixture.await(()->!fixture.client.connected());assertEquals(1,fixture.module.writes);assertEquals(1008,fixture.module.command);}
        }finally{controller.pause().stop().destroy();fixture.close();}
    }
    @Test public void emptyEditorRequiresReviewAndExplicitSend()throws Exception{exercise(false);}
    @Test public void withdrawingParkedAcknowledgementCancelsConfirmation()throws Exception{exercise(true);}
}
