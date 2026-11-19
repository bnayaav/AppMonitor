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

// ווידג'ט נפרד: מלאי המחשבים בחנות (קובייה משלו על המסך)
public class StockWidget extends AppWidgetProvider {
    public static final String REFRESH = "com.comphone.widget.STOCK_REFRESH";

    static int[] ids(Context ctx) {
        return AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx, StockWidget.class));
    }

    public static void refreshAll(Context ctx) {
        Intent i = new Intent(ctx, StockWidget.class);
        i.setAction(REFRESH);
        ctx.sendBroadcast(i);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (REFRESH.equals(intent.getAction())) onUpdate(ctx, AppWidgetManager.getInstance(ctx), ids(ctx));
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
        String title = p.getString("ptitle", "📦 מלאי מחשבים"), empty = "אין מחשבים במלאי";
        if (code.isEmpty() || emp.isEmpty()) {
            empty = "פתחו את האפליקציה \"הזמנות קומפון\" כדי להתחבר.";
        } else {
            try {
                String raw = Api.get("/app/api/widget?code=" + Api.enc(code) + "&e=" + Api.enc(emp) + "&l=stock&v=" + OrdersWidget.ver(ctx));
                JSONObject j = new JSONObject(raw);
                JSONObject pin = j.optJSONObject("pinned");
                if (pin != null) {
                    title = pin.optString("title", title);
                    p.edit().putString("pitems", pin.optJSONArray("items") == null ? "[]" : pin.getJSONArray("items").toString()).putString("ptitle", title).apply();
                    if (pin.optJSONArray("items") == null || pin.getJSONArray("items").length() == 0) empty = pin.optString("empty", empty);
                } else {
                    p.edit().putString("pitems", "[]").apply();
                    empty = "המלאי מוצג רק לעובדים שהוגדרו לכך";
                }
            } catch (Exception e) {
                title = title.replace(" · ⚠️ אין חיבור", "") + " · ⚠️ אין חיבור";
                empty = "אין חיבור לשרת – לחצו ⟳";
            }
        }
        for (int id : ids) mgr.updateAppWidget(id, views(ctx, id, title, empty));
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.slist);
    }

    static RemoteViews views(Context ctx, int wid, String title, String empty) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.stock_widget);
        v.setTextViewText(R.id.stitle, title);
        v.setTextViewText(R.id.sempty, empty);
        Intent svc = new Intent(ctx, ListService.class);
        svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        svc.putExtra("src", "pin");
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.slist, svc);
        v.setEmptyView(R.id.slist, R.id.sempty);
        int imm = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent r = new Intent(ctx, StockWidget.class);
        r.setAction(REFRESH);
        v.setOnClickPendingIntent(R.id.srefresh, PendingIntent.getBroadcast(ctx, 201, r, imm));
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.BASE + "/app"));
        v.setOnClickPendingIntent(R.id.stitle, PendingIntent.getActivity(ctx, 202, open, imm));
        Intent tpl = new Intent(ctx, OrdersWidget.class);
        tpl.setAction(OrdersWidget.DONE);
        v.setPendingIntentTemplate(R.id.slist, PendingIntent.getBroadcast(ctx, 203, tpl, PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0)));
        return v;
    }
}
