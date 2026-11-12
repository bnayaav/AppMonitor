package com.comphone.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrdersWidget extends AppWidgetProvider {
    public static final String REFRESH = "com.comphone.widget.REFRESH";
    public static final String DONE = "com.comphone.widget.DONE";
    public static final String NAV = "com.comphone.widget.NAV";
    public static final String SEL = "com.comphone.widget.SEL";

    public static void refreshAll(Context ctx) {
        Intent i = new Intent(ctx, OrdersWidget.class);
        i.setAction(REFRESH);
        ctx.sendBroadcast(i);
    }

    static int[] ids(Context ctx) {
        return AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx, OrdersWidget.class));
    }

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (REFRESH.equals(a)) {
            onUpdate(ctx, AppWidgetManager.getInstance(ctx), ids(ctx));
        } else if (SEL.equals(a)) {
            Api.prefs(ctx).edit().putString("list", intent.getStringExtra("k")).putString("items", "[]").apply();
            AppWidgetManager m0 = AppWidgetManager.getInstance(ctx);
            for (int id : ids(ctx)) m0.updateAppWidget(id, views(ctx, id, Api.prefs(ctx).getString("title", ""), "טוען…"));
            m0.notifyAppWidgetViewDataChanged(ids(ctx), R.id.list);
            onUpdate(ctx, m0, ids(ctx));
        } else if (NAV.equals(a)) {
            SharedPreferences p = Api.prefs(ctx);
            try {
                JSONArray ls = new JSONArray(p.getString("lists", "[]"));
                if (ls.length() > 0) {
                    String cur = p.getString("list", "all");
                    int idx = 0;
                    for (int i = 0; i < ls.length(); i++) if (cur.equals(ls.getJSONObject(i).optString("k"))) idx = i;
                    idx = (idx + intent.getIntExtra("dir", 1) + ls.length()) % ls.length();
                    JSONObject nx = ls.getJSONObject(idx);
                    p.edit().putString("list", nx.optString("k")).putString("title", nx.optString("name")).putString("items", "[]").apply();
                    AppWidgetManager m0 = AppWidgetManager.getInstance(ctx);
                    for (int id : ids(ctx)) m0.updateAppWidget(id, views(ctx, id, nx.optString("name") + " …", "טוען…"));
                    m0.notifyAppWidgetViewDataChanged(ids(ctx), R.id.list);
                }
            } catch (Exception ignored) {}
            onUpdate(ctx, AppWidgetManager.getInstance(ctx), ids(ctx));
        } else if (DONE.equals(a)) {
            if (intent.getIntExtra("open", 0) == 1) {
                Intent o = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"));
                o.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(o);
                return;
            }
            final String type = intent.getStringExtra("type");
            final int id = intent.getIntExtra("id", 0);
            final String next = intent.getStringExtra("next");
            if (type == null || id == 0) return;
            final SharedPreferences p = Api.prefs(ctx);
            try {
                JSONArray arr = new JSONArray(p.getString("items", "[]")), out = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject it = arr.getJSONObject(i);
                    if (type.equals(it.optString("type")) && it.optInt("id") == id) it.put("pending", true);
                    out.put(it);
                }
                p.edit().putString("items", out.toString()).apply();
            } catch (Exception ignored) {}
            final AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            m.notifyAppWidgetViewDataChanged(ids(ctx), R.id.list);
            final PendingResult pr = goAsync();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Api.get("/app/api/widget/done?code=" + Api.enc(p.getString("code", "")) + "&e=" + Api.enc(p.getString("emp", "")) + "&type=" + Api.enc(type) + "&id=" + id + "&next=" + Api.enc(next == null ? "" : next));
                    } catch (Exception ignored) {}
                    load(ctx, m, ids(ctx));
                    pr.finish();
                }
            }).start();
        }
    }

    @Override
    public void onUpdate(final Context ctx, final AppWidgetManager mgr, final int[] ids) {
        if (ids == null || ids.length == 0) return;
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            public void run() {
                load(ctx, mgr, ids);
                pr.finish();
            }
        }).start();
    }

    static void load(Context ctx, AppWidgetManager mgr, int[] ids) {
        SharedPreferences p = Api.prefs(ctx);
        String code = p.getString("code", ""), emp = p.getString("emp", "");
        String title = p.getString("title", "הזמנות קומפון"), empty = "אין כלום לטיפול 🎉";
        if (code.isEmpty() || emp.isEmpty()) {
            title = "הזמנות קומפון";
            empty = "פתחו את האפליקציה \"הזמנות קומפון\" כדי להתחבר.";
            p.edit().putString("items", "[]").apply();
        } else {
            try {
                String raw;
                try { raw = Api.get("/app/api/widget?code=" + Api.enc(code) + "&e=" + Api.enc(emp) + "&l=" + Api.enc(p.getString("list", "all"))); }
                catch (Exception e1) { try { Thread.sleep(3000); } catch (InterruptedException ie) {} raw = Api.get("/app/api/widget?code=" + Api.enc(code) + "&e=" + Api.enc(emp) + "&l=" + Api.enc(p.getString("list", "all"))); }
                JSONObject j = new JSONObject(raw);
                title = j.optString("title", title);
                SharedPreferences.Editor ed = p.edit().putString("items", j.optJSONArray("items") == null ? "[]" : j.getJSONArray("items").toString()).putString("title", title);
                if (j.optJSONArray("lists") != null) ed.putString("lists", j.getJSONArray("lists").toString());
                ed.putString("list", j.optString("cur", "all")).apply();
                if (j.optJSONArray("items") != null && j.getJSONArray("items").length() == 0) empty = "הרשימה ריקה ✓";
            } catch (Exception e) {
                title = p.getString("title", title).replace(" · ⚠️ אין חיבור", "") + " · ⚠️ אין חיבור";
                empty = "אין חיבור לשרת – לחצו ⟳ לנסות שוב";
            }
        }
        for (int id : ids) mgr.updateAppWidget(id, views(ctx, id, title, empty));
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static RemoteViews views(Context ctx, int wid, String title, String empty) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.title, title);
        v.setTextViewText(R.id.empty, empty);
        Intent svc = new Intent(ctx, ListService.class);
        svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.list, svc);
        v.setEmptyView(R.id.list, R.id.empty);
        int imm = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        int mut = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        Intent r = new Intent(ctx, OrdersWidget.class);
        r.setAction(REFRESH);
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(ctx, 1, r, imm));
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.title, PendingIntent.getActivity(ctx, 2, open, imm));
        Intent add = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app#add"));
        v.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(ctx, 3, add, imm));
        try {
            SharedPreferences p = Api.prefs(ctx);
            JSONArray ls = new JSONArray(p.getString("lists", "[]"));
            String cur = p.getString("list", "all");
            v.removeAllViews(R.id.tabs);
            RemoteViews row = null;
            for (int i = 0; i < ls.length(); i++) {
                JSONObject l = ls.getJSONObject(i);
                if (row == null || i % 3 == 0) { if (row != null) v.addView(R.id.tabs, row); row = new RemoteViews(ctx.getPackageName(), R.layout.tabrow); }
                boolean on = cur.equals(l.optString("k"));
                RemoteViews tb = new RemoteViews(ctx.getPackageName(), on ? R.layout.tab_on_v : R.layout.tab_off_v);
                String nm = l.optString("name").replaceAll("^[^\\p{L}]+", "");
                tb.setTextViewText(R.id.tab, nm + (l.optInt("n") > 0 ? " " + l.optInt("n") : ""));
                Intent sel = new Intent(ctx, OrdersWidget.class);
                sel.setAction(SEL);
                sel.putExtra("k", l.optString("k"));
                sel.putExtra("name", l.optString("name"));
                tb.setOnClickPendingIntent(R.id.tab, PendingIntent.getBroadcast(ctx, 100 + i, sel, imm));
                row.addView(R.id.row, tb);
            }
            if (ls.length() > 0) {
                RemoteViews nt = new RemoteViews(ctx.getPackageName(), R.layout.tab_plus_v);
                Intent nl = new Intent(ctx, MainActivity.class);
                nl.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                nl.putExtra("newList", true);
                nt.setOnClickPendingIntent(R.id.tab, PendingIntent.getActivity(ctx, 99, nl, imm));
                row.addView(R.id.row, nt);
            }
            if (row != null) { int rem = ls.length() % 3; if (rem != 0) for (int k = rem; k < 3; k++) row.addView(R.id.row, new RemoteViews(ctx.getPackageName(), R.layout.tab_fill)); v.addView(R.id.tabs, row); }
        } catch (Exception ignored) {}
        Intent done = new Intent(ctx, OrdersWidget.class);
        done.setAction(DONE);
        v.setPendingIntentTemplate(R.id.list, PendingIntent.getBroadcast(ctx, 4, done, mut));
        return v;
    }
}
