package com.test.mocklocation;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrialManager {
    private static final String PREFS_NAME = "trial_prefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch_time";
    private static final String KEY_SUBSCRIPTION_END = "subscription_end_time";
    private static final String KEY_DEVICE_ID = "bound_device_code";
    private static final String KEY_USED_ORDERS = "used_paid_orders";
    private static final String KEY_MAX_SEEN_END = "max_seen_subscription_end";
    private static final int TRIAL_DAYS = 15;

    private final SharedPreferences prefs;
    private final SharedPreferences backupPrefs;
    private final Context context;

    public TrialManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        backupPrefs = this.context.getSharedPreferences("device_license_backup", Context.MODE_PRIVATE);
        bindDeviceIfNeeded();
        restoreAntiRollbackState();
    }

    public String getDeviceCode() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.length() == 0) androidId = "unknown";
        return MD5Util.md5("test-location-device-" + androidId).substring(0, 16).toUpperCase(Locale.US);
    }

    private void bindDeviceIfNeeded() {
        String current = getDeviceCode();
        if (prefs.getString(KEY_DEVICE_ID, null) == null) prefs.edit().putString(KEY_DEVICE_ID, current).apply();
        if (backupPrefs.getString(KEY_DEVICE_ID, null) == null) backupPrefs.edit().putString(KEY_DEVICE_ID, current).apply();
    }

    private void restoreAntiRollbackState() {
        long end = prefs.getLong(KEY_SUBSCRIPTION_END, 0);
        long backupEnd = backupPrefs.getLong(KEY_SUBSCRIPTION_END, 0);
        long maxSeen = Math.max(prefs.getLong(KEY_MAX_SEEN_END, 0), backupPrefs.getLong(KEY_MAX_SEEN_END, 0));
        long fixed = Math.max(end, Math.max(backupEnd, maxSeen));
        if (fixed > end || fixed > backupEnd) {
            prefs.edit().putLong(KEY_SUBSCRIPTION_END, fixed).putLong(KEY_MAX_SEEN_END, fixed).apply();
            backupPrefs.edit().putLong(KEY_SUBSCRIPTION_END, fixed).putLong(KEY_MAX_SEEN_END, fixed).apply();
        }
        long first = prefs.getLong(KEY_FIRST_LAUNCH, 0);
        long backupFirst = backupPrefs.getLong(KEY_FIRST_LAUNCH, 0);
        long selectedFirst = 0;
        if (first > 0 && backupFirst > 0) selectedFirst = Math.min(first, backupFirst);
        else selectedFirst = Math.max(first, backupFirst);
        if (selectedFirst > 0) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, selectedFirst).apply();
            backupPrefs.edit().putLong(KEY_FIRST_LAUNCH, selectedFirst).apply();
        }
    }

    public void recordFirstLaunch() {
        restoreAntiRollbackState();
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0) == 0) {
            long now = System.currentTimeMillis();
            prefs.edit().putLong(KEY_FIRST_LAUNCH, now).apply();
            backupPrefs.edit().putLong(KEY_FIRST_LAUNCH, now).apply();
        }
    }

    public int getRemainingTrialDays() {
        restoreAntiRollbackState();
        long firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0);
        if (firstLaunch == 0) return TRIAL_DAYS;
        long elapsed = System.currentTimeMillis() - firstLaunch;
        long totalTrialMs = (long) TRIAL_DAYS * 24 * 60 * 60 * 1000L;
        long remaining = totalTrialMs - elapsed;
        if (remaining <= 0) return 0;
        return (int) (remaining / (24 * 60 * 60 * 1000L)) + 1;
    }

    public boolean isTrialExpired() { return getRemainingTrialDays() <= 0; }

    public boolean hasActiveSubscription() {
        restoreAntiRollbackState();
        long endTime = prefs.getLong(KEY_SUBSCRIPTION_END, 0);
        return endTime > System.currentTimeMillis();
    }

    public boolean canUseApp() { return hasActiveSubscription() || !isTrialExpired(); }

    public long getSubscriptionEndTime() {
        restoreAntiRollbackState();
        return prefs.getLong(KEY_SUBSCRIPTION_END, 0);
    }

    public String getSubscriptionEndText() {
        long end = getSubscriptionEndTime();
        if (end <= 0) return "未开通";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(end));
    }

    public boolean wasOrderActivated(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.length() == 0) return false;
        String used = prefs.getString(KEY_USED_ORDERS, "");
        return used.contains("|" + outTradeNo + "|");
    }

    public boolean markOrderActivated(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.length() == 0) return false;
        if (wasOrderActivated(outTradeNo)) return false;
        String used = prefs.getString(KEY_USED_ORDERS, "") + "|" + outTradeNo + "|";
        prefs.edit().putString(KEY_USED_ORDERS, used).putString(KEY_DEVICE_ID, getDeviceCode()).apply();
        backupPrefs.edit().putString(KEY_USED_ORDERS, used).putString(KEY_DEVICE_ID, getDeviceCode()).apply();
        return true;
    }

    public boolean activateSubscriptionOnce(int months, String outTradeNo) {
        if (outTradeNo == null || outTradeNo.length() == 0) return false;
        restoreAntiRollbackState();
        if (wasOrderActivated(outTradeNo)) return false;
        long currentEnd = prefs.getLong(KEY_SUBSCRIPTION_END, 0);
        long baseTime = Math.max(currentEnd, System.currentTimeMillis());
        long endTime = baseTime + (long) months * 30 * 24 * 60 * 60 * 1000L;
        String used = prefs.getString(KEY_USED_ORDERS, "") + "|" + outTradeNo + "|";
        prefs.edit()
                .putLong(KEY_SUBSCRIPTION_END, endTime)
                .putLong(KEY_MAX_SEEN_END, endTime)
                .putString(KEY_USED_ORDERS, used)
                .putString(KEY_DEVICE_ID, getDeviceCode())
                .apply();
        backupPrefs.edit()
                .putLong(KEY_SUBSCRIPTION_END, endTime)
                .putLong(KEY_MAX_SEEN_END, endTime)
                .putString(KEY_USED_ORDERS, used)
                .putString(KEY_DEVICE_ID, getDeviceCode())
                .apply();
        return true;
    }

    /** Backward compatible API; prefer activateSubscriptionOnce(order). */
    public void activateSubscription(int months) {
        activateSubscriptionOnce(months, "LOCAL-" + System.currentTimeMillis());
    }


    public void syncSubscriptionEndFromServer(long expireAtSeconds) {
        if (expireAtSeconds <= 0) return;
        long expireAtMillis = expireAtSeconds * 1000L;
        restoreAntiRollbackState();
        long currentEnd = prefs.getLong(KEY_SUBSCRIPTION_END, 0);
        if (expireAtMillis > currentEnd) {
            prefs.edit()
                    .putLong(KEY_SUBSCRIPTION_END, expireAtMillis)
                    .putLong(KEY_MAX_SEEN_END, expireAtMillis)
                    .putString(KEY_DEVICE_ID, getDeviceCode())
                    .apply();
            backupPrefs.edit()
                    .putLong(KEY_SUBSCRIPTION_END, expireAtMillis)
                    .putLong(KEY_MAX_SEEN_END, expireAtMillis)
                    .putString(KEY_DEVICE_ID, getDeviceCode())
                    .apply();
        }
    }

    public int getTotalTrialDays() { return TRIAL_DAYS; }
}
