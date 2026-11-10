package com.comphone.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import org.json.JSONArray;
import org.json.JSONObject;

public class ListService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    static class Factory implements RemoteViewsFactory {
        final Context ctx;
        JSONArray items = new JSONArray();
        Factory(Context c) { ctx = c; }
        public void onCreate() {}
        public void onDataSetChanged() {
            try { items = new JSONArray(Api.prefs(ctx).getString("items", "[]")); } catch (Exception e) { items = new JSONArray(); }
        }
        public void onDestroy() {}
        public int getCount() { return items.length(); }
        public RemoteViews getViewAt(int i) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) return null;
            if ("header".equals(it.optString("type"))) {
                RemoteViews h = new RemoteViews(ctx.getPackageName(), R.layout.header);
                h.setTextViewText(R.id.h, it.optString("text"));
                return h;
            }
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.row);
            v.setTextViewText(R.id.text, it.optString("text"));
            v.setTextViewText(R.id.sub, it.optString("sub"));
            v.setTextViewText(R.id.check, it.optBoolean("pending") ? "◉" : "◯");
            Intent fill = new Intent();
            fill.putExtra("type", it.optString("type"));
            fill.putExtra("id", it.optInt("id"));
            fill.putExtra("next", it.optString("next"));
            v.setOnClickFillInIntent(R.id.check, fill);
            Intent open = new Intent();
            open.putExtra("open", 1);
            v.setOnClickFillInIntent(R.id.text, open);
            return v;
        }
        public RemoteViews getLoadingView() { return null; }
        public int getViewTypeCount() { return 2; }
        public long getItemId(int i) { return i; }
        public boolean hasStableIds() { return false; }
    }
}
