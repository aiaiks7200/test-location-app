package com.test.mocklocation;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PlanApiHelper {
    public interface PlanCallback {
        void onResult(String monthlyPrice, String yearlyPrice, boolean wxpayEnabled);
    }

    public static void fetchPlans(final PlanCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                String monthly = "19.80";
                String yearly = "168.00";
                boolean wxpay = true;
                try {
                    URL url = new URL(LicenseApiHelper.SERVER_BASE + "/api/plans");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    br.close();
                    JSONObject json = new JSONObject(response.toString());
                    if (json.optInt("code") == 1) {
                        JSONObject data = json.getJSONObject("data");
                        monthly = data.getJSONObject("monthly").optString("price", monthly);
                        yearly = data.getJSONObject("yearly").optString("price", yearly);
                        wxpay = data.optBoolean("wxpay_enabled", true);
                    }
                } catch (Exception ignored) {}
                callback.onResult(monthly, yearly, wxpay);
            }
        }).start();
    }
}
