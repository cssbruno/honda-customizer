package com.cabin.hondacustom;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public final class MainActivity extends Activity {
    private AppUpdater updater;
    private FytClient client; private LinearLayout rows; private TextView status; private CheckBox parked; private Button connect;
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(12,8,12,8);return t;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,12,16,12);
        root.addView(text(getString(R.string.fyt_title),26));
        status=text("",17);root.addView(status);
        LinearLayout actions=new LinearLayout(this);
        connect=new Button(this);connect.setOnClickListener(v->{parked.setChecked(false);if(client.connected())client.disconnect();else client.connect();});actions.addView(connect);
        Button refresh=new Button(this);refresh.setText(getString(R.string.fyt_refresh));refresh.setOnClickListener(v->{parked.setChecked(false);client.connect();});actions.addView(refresh);root.addView(actions);
        Button report=new Button(this);report.setText(getString(R.string.fyt_report));report.setOnClickListener(v->report());actions.addView(report);
        parked=new CheckBox(this);parked.setText(R.string.parked);parked.setOnCheckedChangeListener((b,on)->render());root.addView(parked);
        ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button updates=new Button(this);updates.setText(R.string.update_check);root.addView(updates);
        client=new FytClient(this,this::render);setContentView(root);render();
        updater=new AppUpdater(this,updates);updater.check(false);
    }
    private void render(){if(client==null)return;
        status.setText(client.status);connect.setText(client.connected()?getString(R.string.fyt_disconnect):getString(R.string.fyt_connect));rows.removeAllViews();
        if(!client.connected()&&parked.isChecked())parked.setChecked(false);
        if(!FytProtocol.supported(client.profile())){
            rows.addView(text(client.connected()&&client.profile()!=0?getString(R.string.fyt_unmapped_help):getString(R.string.fyt_connect_help),18));return;
        }
        if(FytProtocol.xp(client.profile())){
            Button packet=new Button(this);packet.setText(R.string.xp_packet_title);
            packet.setEnabled(parked.isChecked()&&client.canSendPacket());packet.setOnClickListener(v->packet());rows.addView(packet);
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
    private void report(){String content=client.report();TextView body=text(content,15);body.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(getString(R.string.fyt_report_title)).setView(scroll).setNegativeButton(getString(R.string.fyt_close),null).setPositiveButton(getString(R.string.fyt_copy),(d,w)->{
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(getString(R.string.fyt_clipboard_label),content));Toast.makeText(this,getString(R.string.fyt_report_copied),Toast.LENGTH_SHORT).show();}).show();
    }
    @Override protected void onStop(){super.onStop();if(client!=null)client.disconnect();}
    @Override protected void onDestroy(){if(updater!=null)updater.close();if(client!=null)client.destroy();super.onDestroy();}
}
