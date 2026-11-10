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

    @Override
    protected void onResume() {
        super.onResume();
        if (!Api.prefs(this).getString("emp", "").isEmpty() && root.getChildCount() > 0 && listBox != null) loadList();
    }

    LinearLayout listBox;

    void showHome() {
        root.removeAllViews();
        SharedPreferences p = Api.prefs(this);
        text("שלום " + p.getString("name", ""), 24, true);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        loadList();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row);
        button("⟳ רענון", new View.OnClickListener() { public void onClick(View v) { loadList(); OrdersWidget.refreshAll(MainActivity.this); } });
        button("＋ הזמנה / פתיחת אפליקציית ההזמנות", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"))); }
        });
        if (Build.VERSION.SDK_INT >= 26) {
            final AppWidgetManager m = getSystemService(AppWidgetManager.class);
            if (m != null && m.isRequestPinAppWidgetSupported() && m.getAppWidgetIds(new ComponentName(this, OrdersWidget.class)).length == 0) {
                button("➕ הוספת הווידג'ט למסך הבית", new View.OnClickListener() {
                    public void onClick(View v) { m.requestPinAppWidget(new ComponentName(MainActivity.this, OrdersWidget.class), null, null); }
                });
            }
        }
        button("החלפת משתמש", new View.OnClickListener() {
            public void onClick(View v) { Api.prefs(MainActivity.this).edit().remove("emp").apply(); showLogin(); }
        });
    }

    void loadList() {
        final SharedPreferences p = Api.prefs(this);
        if (listBox == null) return;
        listBox.removeAllViews();
        TextView t = new TextView(this);
        t.setText("טוען…");
        t.setPadding(0, 24, 0, 24);
        listBox.addView(t);
        new Thread(new Runnable() {
            public void run() {
                try {
                    final JSONObject j = new JSONObject(Api.get("/app/api/widget?code=" + Api.enc(p.getString("code", "")) + "&e=" + Api.enc(p.getString("emp", ""))));
                    runOnUiThread(new Runnable() { public void run() { renderList(j); } });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() { public void run() { listBox.removeAllViews(); TextView x = new TextView(MainActivity.this); x.setText("שגיאה: " + e.getMessage()); listBox.addView(x); } });
                }
            }
        }).start();
    }

    void renderList(JSONObject j) {
        listBox.removeAllViews();
        TextView h = new TextView(this);
        h.setText(j.optString("title"));
        h.setTextSize(16);
        h.setTextColor(Color.parseColor("#0F62FE"));
        h.setPadding(0, 8, 0, 16);
        listBox.addView(h);
        JSONArray items = j.optJSONArray("items");
        if (items == null || items.length() == 0) {
            TextView e = new TextView(this);
            e.setText("אין כלום לטיפול 🎉");
            e.setTextSize(16);
            e.setPadding(0, 24, 0, 24);
            listBox.addView(e);
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            final JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            if ("header".equals(it.optString("type"))) {
                TextView hd = new TextView(this);
                hd.setText(it.optString("text"));
                hd.setTextSize(15);
                hd.setTypeface(null, android.graphics.Typeface.BOLD);
                hd.setTextColor(Color.parseColor("#374151"));
                hd.setPadding(0, 28, 0, 8);
                listBox.addView(hd);
                continue;
            }
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, 10, 0, 10);
            final TextView chk = new TextView(this);
            chk.setText("◯");
            chk.setTextSize(26);
            chk.setTextColor(Color.parseColor("#0F62FE"));
            chk.setPadding(8, 0, 24, 0);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView a = new TextView(this);
            a.setText(it.optString("text"));
            a.setTextSize(16);
            a.setTextColor(Color.parseColor("#111827"));
            TextView b = new TextView(this);
            b.setText(it.optString("sub"));
            b.setTextSize(13);
            b.setTextColor(Color.parseColor("#6B7280"));
            col.addView(a);
            if (!it.optString("sub").isEmpty()) col.addView(b);
            r.addView(chk);
            r.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            chk.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chk.setText("◉");
                    final SharedPreferences p = Api.prefs(MainActivity.this);
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Api.get("/app/api/widget/done?code=" + Api.enc(p.getString("code", "")) + "&e=" + Api.enc(p.getString("emp", "")) + "&type=" + Api.enc(it.optString("type")) + "&id=" + it.optInt("id") + "&next=" + Api.enc(it.optString("next")));
                            } catch (Exception ignored) {}
                            runOnUiThread(new Runnable() { public void run() { loadList(); OrdersWidget.refreshAll(MainActivity.this); } });
                        }
                    }).start();
                }
            });
            listBox.addView(r);
        }
    }
}
