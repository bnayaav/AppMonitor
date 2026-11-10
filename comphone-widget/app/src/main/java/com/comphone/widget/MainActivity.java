package com.comphone.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sv.addView(root);
        setContentView(sv);
        SharedPreferences p = Api.prefs(this);
        if (p.getString("emp", "").isEmpty()) showLogin(); else showHome();
    }

    TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.parseColor("#111827"));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, 16, 0, 16);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.addView(t);
        return t;
    }

    Button button(String s, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(s);
        bt.setAllCaps(false);
        bt.setTextSize(17);
        bt.setOnClickListener(l);
        root.addView(bt);
        return bt;
    }

    void showLogin() {
        root.removeAllViews();
        text("הזמנות קומפון", 24, true);
        text("קוד חנות", 15, false);
        final EditText code = new EditText(this);
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        code.setText(Api.prefs(this).getString("code", ""));
        root.addView(code);
        final TextView err = text("", 14, false);
        button("המשך", new View.OnClickListener() {
            public void onClick(View v) {
                final String c = code.getText().toString().trim();
                err.setText("טוען…");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            final JSONArray emps = new JSONObject(Api.get("/app/api/emps?code=" + Api.enc(c))).getJSONArray("emps");
                            runOnUiThread(new Runnable() { public void run() { pickEmp(c, emps); } });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() { public void run() { err.setText(e.getMessage()); } });
                        }
                    }
                }).start();
            }
        });
    }

    void pickEmp(final String code, JSONArray emps) {
        root.removeAllViews();
        text("מי אתה?", 22, true);
        for (int i = 0; i < emps.length(); i++) {
            final JSONObject e = emps.optJSONObject(i);
            button(e.optString("name"), new View.OnClickListener() {
                public void onClick(View v) {
                    Api.prefs(MainActivity.this).edit().putString("code", code).putString("emp", String.valueOf(e.optInt("id"))).putString("name", e.optString("name")).apply();
                    OrdersWidget.refreshAll(MainActivity.this);
                    showHome();
                }
            });
        }
    }

    void showHome() {
        root.removeAllViews();
        SharedPreferences p = Api.prefs(this);
        text("שלום " + p.getString("name", ""), 24, true);
        text("הווידג'ט מציג את המשימות וההזמנות שלך ומתעדכן לבד. לחיצה על ⟳ בווידג'ט מרעננת מיד, ולחיצה על הרשימה פותחת את אפליקציית ההזמנות.", 15, false);
        if (Build.VERSION.SDK_INT >= 26) {
            final AppWidgetManager m = getSystemService(AppWidgetManager.class);
            if (m != null && m.isRequestPinAppWidgetSupported()) {
                button("➕ הוספת הווידג'ט למסך הבית", new View.OnClickListener() {
                    public void onClick(View v) { m.requestPinAppWidget(new ComponentName(MainActivity.this, OrdersWidget.class), null, null); }
                });
            }
        }
        text("אם הכפתור לא עובד: לחיצה ארוכה על מסך הבית ← ווידג'טים ← \"הזמנות קומפון\" ← גוררים למסך.", 14, false);
        button("פתיחת אפליקציית ההזמנות", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"))); }
        });
        button("רענון הווידג'ט", new View.OnClickListener() {
            public void onClick(View v) { OrdersWidget.refreshAll(MainActivity.this); }
        });
        button("החלפת משתמש", new View.OnClickListener() {
            public void onClick(View v) { Api.prefs(MainActivity.this).edit().remove("emp").apply(); showLogin(); }
        });
    }
}
