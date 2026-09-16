package com.cabin.hondacustom;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public final class MainActivity extends Activity {
    private AppUpdater updater;
    private DirectXpView direct;
    private FrameLayout screen; private LinearLayout controls; private FytAuditView audit;
    private final android.os.Handler auditClock=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tickAudit=new Runnable(){public void run(){if(audit!=null){audit.refresh(client);auditClock.postDelayed(this,1000);}}};
    private FytClient client; private LinearLayout rows; private TextView status; private CheckBox parked; private Button connect;
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(12,8,12,8);return t;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        LinearLayout root=new LinearLayout(this);controls=root;root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,12,16,12);
        root.addView(text(getString(R.string.fyt_title),26));
        status=text("",17);root.addView(status);
        LinearLayout actions=new LinearLayout(this);
        connect=new Button(this);connect.setOnClickListener(v->{parked.setChecked(false);if(client.connected())client.disconnect();else client.connect();});actions.addView(connect);
        Button refresh=new Button(this);refresh.setText(getString(R.string.fyt_refresh));refresh.setOnClickListener(v->{parked.setChecked(false);client.connect();});actions.addView(refresh);root.addView(actions);
        Button report=new Button(this);report.setText(R.string.audit_title);report.setOnClickListener(v->report());actions.addView(report);
        parked=new CheckBox(this);parked.setText(R.string.parked);parked.setOnCheckedChangeListener((b,on)->render());root.addView(parked);
        ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button updates=new Button(this);updates.setText(R.string.update_check);root.addView(updates);
        screen=new FrameLayout(this);screen.addView(root);client=new FytClient(this,this::render);setContentView(screen);render();
        if(saved!=null&&saved.getBoolean("audit_visible"))report();
        updater=new AppUpdater(this,updates);updater.check(false);
    }
    private void render(){if(client==null)return;
        if(audit!=null)audit.refresh(client);
        status.setText(client.status);connect.setText(client.connected()?getString(R.string.fyt_disconnect):getString(R.string.fyt_connect));rows.removeAllViews();
        if(!client.connected()&&parked.isChecked())parked.setChecked(false);
        if(!FytProtocol.supported(client.profile())){
            rows.addView(text(client.connected()&&client.profile()!=0?getString(R.string.fyt_unmapped_help):getString(R.string.fyt_connect_help),18));return;
        }
        if(FytProtocol.xp(client.profile())){
            rows.addView(text(getString(R.string.xp_actions_title),22));
            for(XpAction action:XpAction.values()){
                Button button=new Button(this);button.setText(action.title);
                button.setEnabled(parked.isChecked()&&client.canSendPacket());
                button.setOnClickListener(v->action(action));rows.addView(button);
            }
            Button packet=new Button(this);packet.setText(R.string.xp_packet_title);
            packet.setEnabled(parked.isChecked()&&client.canSendPacket());packet.setOnClickListener(v->packet());rows.addView(packet);
            Button serial=new Button(this);serial.setText(R.string.direct_title);
            serial.setEnabled(parked.isChecked()&&client.canSendPacket());
            serial.setOnClickListener(v->direct());rows.addView(serial);
        }
        java.util.Set<String> categories=new java.util.LinkedHashSet<>();
        for(FytProtocol.Control c:FytProtocol.CONTROLS)if(FytProtocol.visible(client.profile(),c))categories.add(c.category);
        for(String category:categories){
            rows.addView(text(FytText.label(this,category),22));
            for(FytProtocol.Control c:FytProtocol.CONTROLS)if(category.equals(c.category)&&FytProtocol.visible(client.profile(),c)){
            Integer value=client.value(c);Button b=new Button(this);
            b.setText(FytText.label(this,c.title)+"\n"+(value==null?getString(R.string.fyt_wait_value):FytText.label(this,c.options[value]))+(value!=null&&!client.busy()&&!client.editable(c)?getString(R.string.fyt_refresh_required):""));
            b.setEnabled(parked.isChecked()&&client.editable(c));b.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(FytText.label(this,c.title)).setItems(FytText.options(this,c),(dialog,index)->
                new AlertDialog.Builder(this).setTitle(getString(R.string.fyt_apply_question)).setMessage(FytText.label(this,c.title)+": "+FytText.label(this,c.options[index])).setNegativeButton(getString(R.string.fyt_cancel),null).setPositiveButton(getString(R.string.fyt_apply),(d,w)->client.change(c,index,parked.isChecked())).show()
            ).setNegativeButton(getString(R.string.fyt_cancel),null).show());rows.addView(b);
        }
        }
        rows.addView(text(getString(R.string.fyt_feedback_help),14));
    }
    private void action(XpAction action){
        final Object owner=client.connectionToken();
        new AlertDialog.Builder(this).setTitle(action.title)
            .setMessage(getString(action.help)+"\n\n"+getString(R.string.xp_action_confirm))
            .setNegativeButton(R.string.fyt_cancel,null).setPositiveButton(R.string.xp_action_send,(d,w)->{
                if(owner!=client.connectionToken()||!client.canSendPacket()||!parked.isChecked()){
                    Toast.makeText(this,R.string.xp_packet_session_expired,Toast.LENGTH_LONG).show();return;
                }
                client.sendAction(action,parked.isChecked(),owner);
            }).show();
    }
    private void packet(){
        final Object owner=client.connectionToken();
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.addView(text(getString(R.string.xp_packet_help),16));
        EditText input=new EditText(this);input.setHint(R.string.xp_packet_hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1024)});
        content.addView(input);ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog editor=new AlertDialog.Builder(this).setTitle(R.string.xp_packet_title).setView(scroll)
            .setNegativeButton(R.string.fyt_cancel,null).setPositiveButton(R.string.xp_packet_review,null).create();
        editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            final byte[] bytes;
            try{bytes=XpPacket.parse(input.getText().toString());}
            catch(IllegalArgumentException e){input.setError(getString(R.string.xp_packet_invalid));return;}
            if(owner!=client.connectionToken()||!client.canSendPacket()||!parked.isChecked()){
                input.setError(getString(R.string.xp_packet_session_expired));return;
            }
            new AlertDialog.Builder(this).setTitle(R.string.xp_packet_confirm_title)
                .setMessage(getString(R.string.xp_packet_confirm,XpPacket.hex(bytes)))
                .setNegativeButton(R.string.fyt_cancel,null).setPositiveButton(R.string.xp_packet_send,(d2,w)->{
                    if(owner!=client.connectionToken()||!client.canSendPacket()||!parked.isChecked()){
                        Toast.makeText(this,R.string.xp_packet_session_expired,Toast.LENGTH_LONG).show();return;
                    }
                    editor.dismiss();client.sendPacket(bytes,parked.isChecked(),owner);
                }).show();
        }));editor.show();
    }
    private void direct(){
        if(direct!=null||!FytProtocol.xp(client.profile())||!client.canSendPacket()||!parked.isChecked())return;
        client.disconnect();
        direct=new DirectXpView(this,this::closeDirect,client::recordDirect);
        controls.setVisibility(android.view.View.GONE);screen.addView(direct,new FrameLayout.LayoutParams(-1,-1));
    }
    private void closeDirect(){if(direct!=null){direct.cancel();screen.removeView(direct);direct=null;}controls.setVisibility(android.view.View.VISIBLE);render();}
    private void report(){
        if(audit!=null)return;
        audit=new FytAuditView(this,this::closeAudit,()->{
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(getString(R.string.fyt_clipboard_label),client.report()));
            Toast.makeText(this,R.string.fyt_report_copied,Toast.LENGTH_SHORT).show();
        },()->client.requestData());
        controls.setVisibility(android.view.View.GONE);screen.addView(audit,new FrameLayout.LayoutParams(-1,-1));audit.refresh(client);
        auditClock.removeCallbacks(tickAudit);auditClock.postDelayed(tickAudit,1000);
    }
    private void closeAudit(){auditClock.removeCallbacks(tickAudit);if(audit!=null){screen.removeView(audit);audit=null;}controls.setVisibility(android.view.View.VISIBLE);render();}
    @Override public void onBackPressed(){if(direct!=null)closeDirect();else if(audit!=null)closeAudit();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("audit_visible",audit!=null);super.onSaveInstanceState(state);}
    @Override protected void onResume(){super.onResume();auditClock.removeCallbacks(tickAudit);if(audit!=null)tickAudit.run();}
    @Override protected void onPause(){auditClock.removeCallbacks(tickAudit);super.onPause();}
    @Override protected void onStop(){super.onStop();if(direct!=null)closeDirect();if(client!=null)client.disconnect();}
    @Override protected void onDestroy(){auditClock.removeCallbacks(tickAudit);if(updater!=null)updater.close();if(client!=null)client.destroy();super.onDestroy();}
}
