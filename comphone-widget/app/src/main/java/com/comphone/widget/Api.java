package com.comphone.widget;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Api {
    public static final String BASE = "https://comphone-back-office.hanania2004.workers.dev";

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
    }

    public static String get(String path) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(BASE + path).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(20000);
        int code = con.getResponseCode();
        BufferedReader r = new BufferedReader(new InputStreamReader(code >= 400 ? con.getErrorStream() : con.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        if (code == 401) throw new Exception("קוד חנות שגוי");
        if (code >= 400) throw new Exception("שגיאה " + code);
        return sb.toString();
    }

    public static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }
}
