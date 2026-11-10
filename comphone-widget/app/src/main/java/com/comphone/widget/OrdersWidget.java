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
                JSONObject j = new JSONObject(Api.get("/app/api/widget?code=" + Api.enc(code) + "&e=" + Api.enc(emp)));
                title = j.optString("title", title);
                p.edit().putString("items", j.optJSONArray("items") == null ? "[]" : j.getJSONArray("items").toString()).putString("title", title).apply();
            } catch (Exception e) {
                empty = "לא מעודכן: " + e.getMessage();
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
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"));
        v.setOnClickPendingIntent(R.id.title, PendingIntent.getActivity(ctx, 2, open, imm));
        Intent add = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app#add"));
        v.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(ctx, 3, add, imm));
        Intent done = new Intent(ctx, OrdersWidget.class);
        done.setAction(DONE);
        v.setPendingIntentTemplate(R.id.list, PendingIntent.getBroadcast(ctx, 4, done, mut));
        return v;
    }
}
