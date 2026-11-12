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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
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
        if (getIntent() != null && getIntent().getBooleanExtra("newList", false)) {
            getIntent().removeExtra("newList");
            if (!Api.prefs(this).getString("emp", "").isEmpty()) newListDialog();
        }
        if (!Api.prefs(this).getString("emp", "").isEmpty() && root.getChildCount() > 0 && listBox != null) loadList();
    }

    LinearLayout listBox, tabs;
    Button delBtn;

    void showHome() {
        root.removeAllViews();
        SharedPreferences p = Api.prefs(this);
        text("שלום " + p.getString("name", ""), 24, true);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.comphone_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams lgp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (int) (34 * getResources().getDisplayMetrics().density));
        lgp.gravity = Gravity.CENTER_HORIZONTAL;
        lgp.setMargins(0, 4, 0, 16);
        root.addView(logo, 0, lgp);
        final TextView upd = new TextView(this);
        upd.setText("⬇️ יש גרסה חדשה – לחצו לעדכון");
        upd.setTextColor(Color.WHITE);
        upd.setTextSize(15);
        upd.setGravity(Gravity.CENTER);
        upd.setPadding(24, 18, 24, 18);
        GradientDrawable ug = new GradientDrawable();
        ug.setCornerRadius(40);
        ug.setColor(Color.parseColor("#16A34A"));
        upd.setBackground(ug);
        upd.setVisibility(View.GONE);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ulp.setMargins(0, 0, 0, 16);
        root.addView(upd, 1, ulp);
        upd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(Api.BASE + "/app/apk"))); }
        });
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject j = new JSONObject(Api.get("/app/apkver"));
                    int latest = j.optInt("v", 0);
                    int mine = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    if (latest > mine) runOnUiThread(new Runnable() { public void run() { upd.setVisibility(View.VISIBLE); } });
                } catch (Exception ignored) {}
            }
        }).start();
        tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.VERTICAL);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(tabs);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(28, 20, 28, 28);
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
                    final JSONObject j = new JSONObject(Api.get("/app/api/widget?code=" + Api.enc(p.getString("code", "")) + "&e=" + Api.enc(p.getString("emp", "")) + "&l=" + Api.enc(p.getString("alist", "all"))));
                    runOnUiThread(new Runnable() { public void run() { renderList(j); } });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() { public void run() { listBox.removeAllViews(); TextView x = new TextView(MainActivity.this); x.setText("שגיאה: " + e.getMessage()); listBox.addView(x); } });
                }
            }
        }).start();
    }

    static final String[] TABC = {"#DBEAFE", "#FEF3C7", "#DCFCE7", "#FCE7F3", "#EDE9FE", "#FFEDD5", "#CFFAFE", "#E5E7EB"};
    LinearLayout tabRow;
    int tabN = 0;
    String selColor = "#FFFFFF";

    TextView chip(String label, boolean on) {
        if (tabRow == null || tabRow.getChildCount() >= 3) {
            tabRow = new LinearLayout(this);
            tabRow.setOrientation(LinearLayout.HORIZONTAL);
            tabRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            tabs.addView(tabRow);
        }
        String col = TABC[tabN++ % TABC.length];
        TextView c = new TextView(this);
        c.setText(label);
        c.setTextSize(15);
        c.setGravity(Gravity.CENTER);
        c.setMaxLines(1);
        c.setEllipsize(android.text.TextUtils.TruncateAt.END);
        c.setPadding(10, on ? 30 : 22, 10, on ? 30 : 22);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadii(new float[]{28, 28, 28, 28, 0, 0, 0, 0});
        g.setColor(Color.parseColor(col));
        g.setStroke(on ? 5 : 2, Color.parseColor(on ? "#0F62FE" : "#9CA3AF"));
        c.setBackground(g);
        c.setTextColor(Color.parseColor("#111827"));
        if (on) { c.setTypeface(null, android.graphics.Typeface.BOLD); selColor = col; }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, on ? 0 : 10, 4, 0);
        lp.gravity = Gravity.BOTTOM;
        tabRow.addView(c, lp);
        return c;
    }

    void listCall(final String q) {
        final SharedPreferences p = Api.prefs(this);
        new Thread(new Runnable() {
            public void run() {
                try { Api.get("/app/api/lists?code=" + Api.enc(p.getString("code", "")) + "&e=" + Api.enc(p.getString("emp", "")) + q); } catch (Exception ignored) {}
                runOnUiThread(new Runnable() { public void run() { loadList(); OrdersWidget.refreshAll(MainActivity.this); } });
            }
        }).start();
    }

    void newListDialog() {
        final SharedPreferences p = Api.prefs(this);
        final EditText in = new EditText(MainActivity.this);
        in.setHint("למשל: ספק אמטל");
        new AlertDialog.Builder(MainActivity.this).setTitle("לשונית חדשה").setView(in).setPositiveButton("יצירה", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                String n = in.getText().toString().trim();
                if (n.isEmpty()) return;
                p.edit().putString("alist", "L:" + n).putString("list", "L:" + n).apply();
                listCall("&action=add&name=" + Api.enc(n));
            }
        }).setNegativeButton("ביטול", null).show();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
    }

    void renderTabs(JSONObject j) {
        tabs.removeAllViews();
        tabRow = null;
        tabN = 0;
        selColor = "#FFFFFF";
        final SharedPreferences p = Api.prefs(this);
        String cur = j.optString("cur", "all");
        JSONArray ls = j.optJSONArray("lists");
        if (ls != null) for (int i = 0; i < ls.length(); i++) {
            final JSONObject l = ls.optJSONObject(i);
            String nm = l.optString("name").replaceAll("^[^\\p{L}]+", "");
            chip(nm + (l.optInt("n") > 0 ? " " + l.optInt("n") : ""), cur.equals(l.optString("k"))).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { p.edit().putString("alist", l.optString("k")).apply(); loadList(); }
            });
        }
        chip("＋", false).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { newListDialog(); }
        });
    }

    void renderList(JSONObject j) {
        listBox.removeAllViews();
        renderTabs(j);
        if (tabRow != null) while (tabRow.getChildCount() < 3) { View sp = new View(this); tabRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f)); }
        GradientDrawable pg = new GradientDrawable();
        pg.setColor(Color.parseColor(selColor));
        pg.setStroke(5, Color.parseColor("#0F62FE"));
        pg.setCornerRadii(new float[]{0, 0, 0, 0, 24, 24, 24, 24});
        listBox.setBackground(pg);
        final String cur = j.optString("cur", "all");
        if (cur.startsWith("L:")) {
            final String ln = cur.substring(2);
            LinearLayout add = new LinearLayout(this);
            add.setOrientation(LinearLayout.HORIZONTAL);
            final EditText in = new EditText(this);
            in.setHint("פריט חדש…");
            in.setSingleLine(true);
            Button ok = new Button(this);
            ok.setText("הוסף");
            ok.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tx = in.getText().toString().trim();
                    if (tx.isEmpty()) return;
                    in.setText("");
                    listCall("&action=item&name=" + Api.enc(ln) + "&title=" + Api.enc(tx));
                }
            });
            add.addView(in, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            add.addView(ok);
            listBox.addView(add);
            Button del = new Button(this);
            del.setText("מחיקת הרשימה");
            del.setAllCaps(false);
            del.setTextColor(Color.parseColor("#DC2626"));
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("למחוק את \"" + ln + "\" וכל הפריטים?").setPositiveButton("מחיקה", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { Api.prefs(MainActivity.this).edit().putString("alist", "all").apply(); listCall("&action=del&name=" + Api.enc(ln)); }
                    }).setNegativeButton("ביטול", null).show();
                }
            });
            delBtn = del;
        } else delBtn = null;
        TextView h = new TextView(this);
        h.setText(j.optString("title"));
        h.setTextSize(16);
        h.setTextColor(Color.parseColor("#0F62FE"));
        h.setPadding(0, 8, 0, 16);
        listBox.addView(h);
        JSONArray items = j.optJSONArray("items");
        if (items == null || items.length() == 0) {
            TextView e = new TextView(this);
            e.setText(cur.equals("all") ? "אין כלום לטיפול 🎉" : "הרשימה ריקה ✓");
            e.setTextSize(16);
            e.setPadding(0, 24, 0, 24);
            listBox.addView(e);
            if (delBtn != null) listBox.addView(delBtn);
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            final JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            if ("header".equals(it.optString("type"))) {
                if (listBox.getChildCount() > 1) {
                    View line = new View(this);
                    line.setBackgroundColor(Color.parseColor("#0F62FE"));
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 6);
                    llp.setMargins(0, 36, 0, 0);
                    listBox.addView(line, llp);
                }
                TextView hd = new TextView(this);
                hd.setText(it.optString("text"));
                hd.setTextSize(17);
                hd.setTypeface(null, android.graphics.Typeface.BOLD);
                hd.setTextColor(Color.parseColor("#0F62FE"));
                hd.setPadding(0, 16, 0, 8);
                listBox.addView(hd);
                continue;
            }
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, 10, 0, 10);
            final TextView chk = new TextView(this);
            final boolean info = !it.optString("icon").isEmpty();
            chk.setText(info ? it.optString("icon") : "◯");
            chk.setTextSize(info ? 20 : 26);
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
            if (!info) chk.setOnClickListener(new View.OnClickListener() {
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
        if (delBtn != null) listBox.addView(delBtn);
    }
}
