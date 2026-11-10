package com.comphone.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONObject;

public class OrdersWidget extends AppWidgetProvider {
    public static final String REFRESH = "com.comphone.widget.REFRESH";

    public static void refreshAll(Context ctx) {
        Intent i = new Intent(ctx, OrdersWidget.class);
        i.setAction(REFRESH);
        ctx.sendBroadcast(i);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (REFRESH.equals(intent.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            int[] ids = m.getAppWidgetIds(new ComponentName(ctx, OrdersWidget.class));
            onUpdate(ctx, m, ids);
        }
    }

    @Override
    public void onUpdate(final Context ctx, final AppWidgetManager mgr, final int[] ids) {
        if (ids == null || ids.length == 0) return;
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            public void run() {
                String title = "הזמנות קומפון", body;
                SharedPreferences p = Api.prefs(ctx);
                String code = p.getString("code", ""), emp = p.getString("emp", "");
                if (code.isEmpty() || emp.isEmpty()) {
                    body = "פתחו את האפליקציה \"הזמנות קומפון\" כדי להתחבר.";
                } else {
                    try {
                        JSONObject j = new JSONObject(Api.get("/app/api/widget?code=" + Api.enc(code) + "&e=" + Api.enc(emp)));
                        title = j.optString("title", title);
                        body = j.optString("text", "");
                        p.edit().putString("last", body).putString("lastTitle", title).apply();
                    } catch (Exception e) {
                        title = p.getString("lastTitle", title);
                        body = p.getString("last", "") + "\n(לא מעודכן: " + e.getMessage() + ")";
                    }
                }
                for (int id : ids) mgr.updateAppWidget(id, views(ctx, title, body));
                pr.finish();
            }
        }).start();
    }

    static RemoteViews views(Context ctx, String title, String body) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.title, title);
        v.setTextViewText(R.id.body, body);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent r = new Intent(ctx, OrdersWidget.class);
        r.setAction(REFRESH);
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(ctx, 1, r, fl));
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"));
        v.setOnClickPendingIntent(R.id.body, PendingIntent.getActivity(ctx, 2, open, fl));
        v.setOnClickPendingIntent(R.id.title, PendingIntent.getActivity(ctx, 2, open, fl));
        return v;
    }
}
