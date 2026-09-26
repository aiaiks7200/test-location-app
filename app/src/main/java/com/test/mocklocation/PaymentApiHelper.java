package com.test.mocklocation;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PaymentApiHelper {
    private static final String BASE_URL = LicenseApiHelper.SERVER_BASE;

    public interface PaymentCallback {
        void onSuccess(String payUrl, String h5Url, String wechatPayUrl, String tradeNo);
        void onError(String error);
    }

    public interface QueryCallback {
        void onResult(boolean paid, String status, long expireAtSeconds);
        void onError(String error);
    }

    public static String generateOrderId() {
        return "FL" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
    }

    public static void createOrder(final String amount, final String subject,
                                    final String payType, final String outTradeNo,
                                    final String deviceCode, final String plan,
                                    final PaymentCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject req = new JSONObject();
                    req.put("order_no", outTradeNo);
                    req.put("device_code", deviceCode);
                    req.put("plan", plan);
                    req.put("amount", amount);
                    req.put("paytype_code", payType);
                    String response = httpJson(BASE_URL + "/api/pay/create", req.toString());
                    JSONObject json = new JSONObject(response);
                    if (json.optInt("code") == 1) {
                        JSONObject data = json.getJSONObject("data");
                        String payUrl = data.optString("pay_url", "");
                        String h5Url = data.optString("h5_url", payUrl);
                        String wechatPayUrl = data.optString("wechat_pay_url", "");
                        callback.onSuccess(payUrl, h5Url, wechatPayUrl, data.optString("trade_no"));
                    } else {
                        callback.onError(json.optString("msg", "create order failed"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public static void queryOrder(final String outTradeNo, final QueryCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject req = new JSONObject();
                    req.put("order_no", outTradeNo);
                    String response = httpJson(BASE_URL + "/api/pay/query", req.toString());
                    JSONObject json = new JSONObject(response);
                    if (json.optInt("code") == 1) {
                        JSONObject data = json.getJSONObject("data");
                        callback.onResult(data.optBoolean("paid", false), data.optString("status", ""), data.optLong("expire_at", 0));
                    } else {
                        callback.onError(json.optString("msg", "query failed"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static String httpJson(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        BufferedReader br;
        if (conn.getResponseCode() >= 400) {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();
        return response.toString();
    }
}
