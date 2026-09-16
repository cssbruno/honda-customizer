package com.cabin.hondacustom;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.*;

/** Read-only, in-memory audit. Updating text preserves the user's scroll position. */
final class FytAuditView extends LinearLayout {
    private final TextView summary, identity, capabilities, feedback, history;

    FytAuditView(Context context, Runnable back, Runnable copy) {
        super(context);
        setOrientation(VERTICAL);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        setPadding(padding, padding, padding, padding);
        LinearLayout actions = new LinearLayout(context);
        Button close = new Button(context);
        close.setText(R.string.audit_back);
        close.setOnClickListener(v -> back.run());
        actions.addView(close);
        TextView title = text(context.getString(R.string.audit_title), 22);
        title.setTypeface(null, Typeface.BOLD);
        actions.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button clipboard = new Button(context);
        clipboard.setText(R.string.fyt_copy);
        clipboard.setOnClickListener(v -> copy.run());
        actions.addView(clipboard);
        addView(actions);
        ScrollView scroll = new ScrollView(context);
        LinearLayout sections = new LinearLayout(context);
        sections.setOrientation(VERTICAL);
        summary = section(sections, R.string.audit_summary);
        identity = section(sections, R.string.audit_identity);
        capabilities = section(sections, R.string.audit_capabilities);
        feedback = section(sections, R.string.audit_feedback);
        history = section(sections, R.string.audit_history);
        scroll.addView(sections);
        addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setPadding(8, 8, 8, 8);
        return view;
    }

    private TextView section(LinearLayout parent, int title) {
        TextView heading = text(getContext().getString(title), 21);
        heading.setTypeface(null, Typeface.BOLD);
        parent.addView(heading);
        TextView body = text("", 17);
        body.setTextIsSelectable(true);
        parent.addView(body);
        return body;
    }

    void refresh(FytClient client) {
        update(summary, client.auditSummary());
        update(identity, client.auditIdentity());
        update(capabilities, client.auditCapabilities());
        update(feedback, client.auditFeedback());
        update(history, client.auditHistory());
    }

    private void update(TextView view, String value) {
        if (!value.contentEquals(view.getText())) view.setText(value);
    }
}
