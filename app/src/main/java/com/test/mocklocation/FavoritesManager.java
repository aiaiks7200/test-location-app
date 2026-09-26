package com.test.mocklocation;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FavoritesManager {
    
    private static final String PREFS_NAME = "mock_location_favorites";
    private static final String KEY_FAVORITES = "favorites";
    private SharedPreferences prefs;
    
    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveFavorite(String name, double lat, double lng) {
        try {
            List<Favorite> favorites = getFavorites();
            // Check if name exists, update if so
            boolean found = false;
            for (int i = 0; i < favorites.size(); i++) {
                if (favorites.get(i).name.equals(name)) {
                    favorites.set(i, new Favorite(name, lat, lng));
                    found = true;
                    break;
                }
            }
            if (!found) {
                favorites.add(new Favorite(name, lat, lng));
            }
            
            JSONArray arr = new JSONArray();
            for (Favorite f : favorites) {
                JSONObject obj = new JSONObject();
                obj.put("name", f.name);
                obj.put("lat", f.lat);
                obj.put("lng", f.lng);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<Favorite> getFavorites() {
        List<Favorite> result = new ArrayList<Favorite>();
        try {
            String json = prefs.getString(KEY_FAVORITES, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new Favorite(
                    obj.getString("name"),
                    obj.getDouble("lat"),
                    obj.getDouble("lng")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public void deleteFavorite(String name) {
        try {
            List<Favorite> favorites = getFavorites();
            List<Favorite> updated = new ArrayList<Favorite>();
            for (Favorite f : favorites) {
                if (!f.name.equals(name)) {
                    updated.add(f);
                }
            }
            JSONArray arr = new JSONArray();
            for (Favorite f : updated) {
                JSONObject obj = new JSONObject();
                obj.put("name", f.name);
                obj.put("lat", f.lat);
                obj.put("lng", f.lng);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static class Favorite {
        public String name;
        public double lat;
        public double lng;
        
        public Favorite(String name, double lat, double lng) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }
    }
}
