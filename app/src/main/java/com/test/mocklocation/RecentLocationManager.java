package com.test.mocklocation;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentLocationManager {
    private static final String PREFS_NAME = "recent_mock_locations";
    private static final String KEY_RECENT = "recent_locations";
    public static final int MAX_RECENT = 3;

    private final SharedPreferences prefs;

    public RecentLocationManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveRecent(double lat, double lng) {
        try {
            List<LocationItem> oldItems = getRecentLocations();
            List<LocationItem> updated = new ArrayList<LocationItem>();
            long now = System.currentTimeMillis();
            String label = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(now));
            updated.add(new LocationItem(label, lat, lng, now));
            for (LocationItem item : oldItems) {
                if (isSamePoint(item.lat, item.lng, lat, lng)) continue;
                updated.add(item);
                if (updated.size() >= MAX_RECENT) break;
            }
            save(updated);
        } catch (Exception ignored) {}
    }

    public List<LocationItem> getRecentLocations() {
        List<LocationItem> result = new ArrayList<LocationItem>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECENT, "[]"));
            for (int i = 0; i < arr.length() && i < MAX_RECENT; i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new LocationItem(
                        obj.optString("label", "最近位置" + (i + 1)),
                        obj.getDouble("lat"),
                        obj.getDouble("lng"),
                        obj.optLong("time", 0)
                ));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private void save(List<LocationItem> items) throws Exception {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < items.size() && i < MAX_RECENT; i++) {
            LocationItem item = items.get(i);
            JSONObject obj = new JSONObject();
            obj.put("label", item.label);
            obj.put("lat", item.lat);
            obj.put("lng", item.lng);
            obj.put("time", item.time);
            arr.put(obj);
        }
        prefs.edit().putString(KEY_RECENT, arr.toString()).apply();
    }

    private boolean isSamePoint(double lat1, double lng1, double lat2, double lng2) {
        // 只在经纬度都几乎完全一致时才去重，避免不同地点被误合并成一个历史点。
        return Math.abs(lat1 - lat2) < 0.0000001 && Math.abs(lng1 - lng2) < 0.0000001;
    }

    public static class LocationItem {
        public final String label;
        public final double lat;
        public final double lng;
        public final long time;

        public LocationItem(String label, double lat, double lng, long time) {
            this.label = label;
            this.lat = lat;
            this.lng = lng;
            this.time = time;
        }
    }
}
