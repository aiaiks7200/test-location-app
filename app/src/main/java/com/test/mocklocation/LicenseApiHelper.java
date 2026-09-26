package com.test.mocklocation;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LicenseApiHelper {
    // 使用 StringObfuscator 拼接，防止静态搜索到完整域名
    public static final String SERVER_BASE = StringObfuscator.getServerBase();
    private static final String PREFS = "license_cache";
    private static final String KEY_AUTH = "authorized";
    private static final String KEY_EXPIRE = "expire_at";
    private static final String KEY_CACHE_UNTIL = "cache_until";
    private static final String KEY_MUST_UPGRADE = "must_upgrade";
    private static final String KEY_DOWNLOAD_URL = "download_url";
    private static final String KEY_LATEST_VERSION = "latest_version";
    private static final String KEY_CHANGELOG = "changelog";

    public interface LicenseCallback {
        void onResult(boolean canUse, boolean mustUpgrade, String message);
    }

    public static boolean canUseCached(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis() / 1000;
        return p.getBoolean(KEY_AUTH, false) && p.getLong(KEY_EXPIRE, 0) > now && p.getLong(KEY_CACHE_UNTIL, 0) > now && !p.getBoolean(KEY_MUST_UPGRADE, false);
    }

    public static boolean mustUpgradeCached(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MUST_UPGRADE, false);
    }

    public static String getDownloadUrl(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DOWNLOAD_URL, SERVER_BASE + "/download");
    }

    public static String getLatestVersion(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LATEST_VERSION, "");
    }

    public static String getChangelog(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CHANGELOG, "");
    }

    public static void checkLicense(final Context context, final String deviceCode, final LicenseCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject req = new JSONObject();
                    req.put("device_code", deviceCode);
                    String vName = "1.11";
                    int vCode = 12;
                    try {
                        android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                        vName = pi.versionName;
                        if (android.os.Build.VERSION.SDK_INT >= 28) vCode = (int) pi.getLongVersionCode();
                        else vCode = pi.versionCode;
                    } catch (Exception ignored) {}
                    req.put("version_name", vName);
                    req.put("version_code", vCode);
                    String response = httpJson(SERVER_BASE + "/api/license/check", req.toString());
                    JSONObject json = new JSONObject(response);
                    if (json.optInt("code") != 1) {
                        callback.onResult(canUseCached(context), mustUpgradeCached(context), json.optString("msg", "授权查询失败"));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    boolean authorized = data.optBoolean("authorized", false);
                    boolean mustUpgrade = data.optBoolean("must_upgrade", false);
                    long expireAt = data.optLong("expire_at", 0);
                    long cacheUntil = data.optLong("cache_until", 0);
                    String downloadUrl = SERVER_BASE + "/download";
                    String latestVersion = "";
                    String changelog = "";
                    if (!data.isNull("latest_version")) {
                        JSONObject latest = data.getJSONObject("latest_version");
                        downloadUrl = latest.optString("download_url", downloadUrl);
                        latestVersion = latest.optString("version_name", "");
                        changelog = latest.optString("changelog", "");
                    }
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putBoolean(KEY_AUTH, authorized)
                            .putLong(KEY_EXPIRE, expireAt)
                            .putLong(KEY_CACHE_UNTIL, cacheUntil)
                            .putBoolean(KEY_MUST_UPGRADE, mustUpgrade)
                            .putString(KEY_DOWNLOAD_URL, downloadUrl)
                            .putString(KEY_LATEST_VERSION, latestVersion)
                            .putString(KEY_CHANGELOG, changelog)
                            .apply();
                    if (expireAt > 0) {
                        new TrialManager(context).syncSubscriptionEndFromServer(expireAt);
                    }
                    callback.onResult(authorized && !mustUpgrade, mustUpgrade, mustUpgrade ? "发现强制升级版本" : (authorized ? "授权有效" : "会员已到期"));
                } catch (Exception e) {
                    callback.onResult(canUseCached(context), mustUpgradeCached(context), "网络异常，使用48小时内缓存授权");
                }
            }
        }).start();
    }

    public static void registerOrder(final String orderNo, final String deviceCode, final String plan, final String amount) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject req = new JSONObject();
                    req.put("order_no", orderNo);
                    req.put("device_code", deviceCode);
                    req.put("plan", plan);
                    req.put("amount", amount);
                    httpJson(SERVER_BASE + "/api/order/register", req.toString());
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private static String httpJson(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();
        return response.toString();
    }
}
