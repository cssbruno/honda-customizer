package com.cabin.hondacustom;

import android.content.Context;
import android.text.InputType;
import android.widget.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One explicit direct-port attempt; feedback remains unavailable. */
final class DirectXpView extends LinearLayout {
    interface Audit { void record(String text); }
    private final AtomicBoolean active = new AtomicBoolean(true);
    private boolean attempted;
    DirectXpView(Context context, Runnable back, Audit audit) {
        super(context);
        setOrientation(VERTICAL);
        Button close = new Button(context); close.setText(R.string.audit_back);
        close.setOnClickListener(v -> { cancel(); back.run(); }); addView(close);
        ScrollView scroll = new ScrollView(context);
        LinearLayout form = new LinearLayout(context); form.setOrientation(VERTICAL);
        form.setPadding(16, 12, 16, 12); scroll.addView(form);
        addView(scroll, new LayoutParams(-1, 0, 1));
        label(form, context.getString(R.string.direct_title));
        label(form, context.getString(R.string.direct_help));
        EditText path = field(form, R.string.direct_path, InputType.TYPE_CLASS_TEXT);
        EditText baud = field(form, R.string.direct_baud, InputType.TYPE_CLASS_NUMBER);
        EditText body = field(form, R.string.xp_packet_hint, InputType.TYPE_CLASS_TEXT);
        CheckBox ready = new CheckBox(context); ready.setText(R.string.direct_ready); form.addView(ready);
        TextView preview = label(form, ""); preview.setTextIsSelectable(true);
        Button review = new Button(context); review.setText(R.string.xp_packet_review); form.addView(review);
        Button send = new Button(context); send.setText(R.string.direct_send); send.setEnabled(false); form.addView(send);
        TextView result = label(form, context.getString(R.string.direct_no_feedback)); result.setTextIsSelectable(true);
        final String[] reviewed = new String[3];
        android.text.TextWatcher changed = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { send.setEnabled(false); preview.setText(""); }
            public void afterTextChanged(android.text.Editable text) {}
        };
        path.addTextChangedListener(changed); baud.addTextChangedListener(changed); body.addTextChangedListener(changed);
        ready.setOnCheckedChangeListener((button, checked) -> send.setEnabled(false));
        review.setOnClickListener(v -> {
            try {
                String port = path.getText().toString().trim();
                int rate = Integer.parseInt(baud.getText().toString().trim());
                DirectXpSerial.validateEndpoint(port, rate);
                byte[] bytes = XpPacket.parse(body.getText().toString());
                preview.setText(port + " / " + rate + " / 8N1\n" + DirectXpSerial.hex(DirectXpSerial.frame(0x4012a, bytes)));
                reviewed[0] = path.getText().toString(); reviewed[1] = baud.getText().toString(); reviewed[2] = body.getText().toString();
                send.setEnabled(active.get() && !attempted && ready.isChecked());
            } catch (IllegalArgumentException error) { result.setText(error.getMessage()); send.setEnabled(false); }
        });
        send.setOnClickListener(v -> {
            if (!active.get() || attempted || !ready.isChecked() ||
                !path.getText().toString().equals(reviewed[0]) || !baud.getText().toString().equals(reviewed[1]) ||
                !body.getText().toString().equals(reviewed[2])) return;
            final String port = reviewed[0].trim(); final int rate = Integer.parseInt(reviewed[1].trim());
            final byte[] bytes = XpPacket.parse(reviewed[2]);
            attempted = true; send.setEnabled(false); review.setEnabled(false);
            path.setEnabled(false); baud.setEnabled(false); body.setEnabled(false); ready.setEnabled(false);
            String request = context.getString(R.string.direct_title) + "\n" + preview.getText();
            audit.record(request); result.setText(R.string.xp_packet_sending);
            new Thread(() -> {
                String outcome;
                if (!active.get()) outcome = context.getString(R.string.direct_cancelled);
                else try {
                    int written = DirectXpSerial.send(0x4012a, port, rate, bytes);
                    outcome = context.getString(R.string.direct_written, written, bytes.length + 2);
                } catch (Exception | LinkageError error) {
                    outcome = context.getString(R.string.direct_failed, error.toString());
                }
                final String completed = outcome;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    audit.record(completed);
                    if (active.get()) result.setText(completed);
                });
            }, "xp-direct-write").start();
        });
    }
    void cancel() { active.set(false); }
    private TextView label(LinearLayout parent, String text) {
        TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(17);
        view.setPadding(8, 8, 8, 8); parent.addView(view); return view;
    }
    private EditText field(LinearLayout parent, int hint, int type) {
        EditText field = new EditText(getContext()); field.setHint(hint); field.setInputType(type);
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1024)});
        parent.addView(field); return field;
    }
}
