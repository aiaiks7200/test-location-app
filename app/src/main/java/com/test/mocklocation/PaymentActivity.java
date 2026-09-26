package com.test.mocklocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

public class PaymentActivity extends Activity {
    private TrialManager trialManager;
    private Handler handler;
    private String currentOutTradeNo;
    private String currentOrderId;
    private String selectedPlan = "monthly";
    private String selectedPayType = "alipay";
    private TextView tvStatus;
    private Button btnMonthly;
    private Button btnYearly;
    private String monthlyPrice = "19.80";
    private String yearlyPrice = "168.00";
    private boolean activatedThisOrder = false;
    private int pollAttempts = 0;
    private String currentPayUrl;
    private String currentH5Url;
    private String currentWechatPayUrl;
    private boolean wxpayEnabled = true; // 默认开启，从服务器读取

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        trialManager = new TrialManager(this);
        handler = new Handler(Looper.getMainLooper());

        btnMonthly = (Button) findViewById(R.id.btn_monthly);
        btnYearly = (Button) findViewById(R.id.btn_yearly);
        tvStatus = (TextView) findViewById(R.id.tv_payment_status);

        updateInitialStatus();
        refreshPlanPrices();

        btnMonthly.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedPlan = "monthly";
                showPayMethodDialog(monthlyPrice, getString(R.string.app_name) + "-" + getString(R.string.monthly_plan));
            }
        });

        btnYearly.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedPlan = "yearly";
                showPayMethodDialog(yearlyPrice, getString(R.string.app_name) + "-" + getString(R.string.yearly_plan));
            }
        });
    }

    private void refreshPlanPrices() {
        PlanApiHelper.fetchPlans((monthly, yearly, wxpay) -> runOnUiThread(() -> {
            monthlyPrice = monthly;
            yearlyPrice = yearly;
            wxpayEnabled = wxpay;
            btnMonthly.setText("月度会员 ¥" + monthlyPrice + "/月");
            btnYearly.setText("年度会员 ¥" + yearlyPrice + "/年");
        }));
    }

    private void updateInitialStatus() {
        String msg = "设备码：" + trialManager.getDeviceCode() + "\n会员有效期：" + trialManager.getSubscriptionEndText();
        if (trialManager.hasActiveSubscription()) {
            msg += "\n当前会员未到期，重复进入本页面不会增加时长；只有新订单真实支付成功才会续期。";
        }
        tvStatus.setText(msg);
    }

    private void showPayMethodDialog(final String amount, final String subject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择支付方式")
               .setMessage("设备码：" + trialManager.getDeviceCode() + "\n将按当前设备绑定会员。\n\n请选择支付方式：")
               .setPositiveButton("支付宝支付", (dialog, which) -> {
                   selectedPayType = "alipay";
                   createOrder(amount, subject + "-" + trialManager.getDeviceCode());
               })
               .setNegativeButton("取消", null);

        // 根据服务器设置决定是否显示微信支付
        if (wxpayEnabled) {
            builder.setNeutralButton("微信支付", (dialog, which) -> {
                selectedPayType = "wxpay";
                createOrder(amount, subject + "-" + trialManager.getDeviceCode());
            });
        }
        builder.show();
    }

    private void createOrder(final String amount, final String subject) {
        currentOrderId = PaymentApiHelper.generateOrderId();
        currentOutTradeNo = null;
        activatedThisOrder = false;
        pollAttempts = 0;
        currentPayUrl = null;
        currentH5Url = null;
        currentWechatPayUrl = null;
        tvStatus.setText(getString(R.string.creating_order));

        PaymentApiHelper.createOrder(amount, subject, selectedPayType, currentOrderId, trialManager.getDeviceCode(), selectedPlan,
                new PaymentApiHelper.PaymentCallback() {
                    @Override public void onSuccess(final String payUrl, final String h5Url, final String wechatPayUrl, final String tradeNo) {
                        runOnUiThread(() -> {
                            currentOutTradeNo = tradeNo;
                            currentPayUrl = payUrl;
                            currentH5Url = h5Url;
                            currentWechatPayUrl = wechatPayUrl;
                            tvStatus.setText(getString(R.string.order_created) + "\n订单号：" + currentOrderId);
                            openPayUrl();
                            handler.removeCallbacks(pollRunnable);
                            handler.postDelayed(pollRunnable, 3000);
                        });
                    }

                    @Override public void onError(final String error) {
                        runOnUiThread(() -> {
                            tvStatus.setText(getString(R.string.order_failed) + error);
                            new AlertDialog.Builder(PaymentActivity.this)
                                    .setTitle("创建订单失败")
                                    .setMessage("无法拉起支付，请检查网络后重试。\n\n错误信息：" + error)
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .show();
                        });
                    }
                });
    }

    private void openPayUrl() {
        if ("wxpay".equals(selectedPayType)) {
            openWechatPay();
            return;
        }
        String url = currentPayUrl;
        if (url == null || url.isEmpty()) url = currentH5Url;
        if (url == null || url.isEmpty()) {
            tvStatus.setText("支付链接为空，请重试");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("请打开支付链接")
                    .setMessage("当前设备没有可打开支付页面的浏览器，请复制或手动打开：\n\n" + url)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("支付跳转失败")
                    .setMessage("支付链接：\n" + url + "\n\n错误：" + e.getMessage())
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }
    }

    private void openWechatPay() {
        // 优先用 Spay H5 页面（展示微信扫码二维码 + 手机端可复制链接到微信）
        // 不再使用 weixin://dl/business/?t= 协议（需要域名在微信注册，Spay 域名未注册会报"页面无法访问"）
        String payLink = currentH5Url;
        if (payLink == null || payLink.isEmpty()) payLink = currentPayUrl;
        if (payLink == null || payLink.isEmpty()) {
            tvStatus.setText("支付链接为空，请重试");
            return;
        }
        final String url = payLink;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("请打开支付链接")
                    .setMessage("当前设备没有可打开支付页面的浏览器，请复制或手动打开：\n\n" + url)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("支付跳转失败")
                    .setMessage("支付链接：\n" + url + "\n\n错误：" + e.getMessage())
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            if (currentOrderId == null || pollAttempts >= 60 || activatedThisOrder) {
                if (!activatedThisOrder) tvStatus.setText(getString(R.string.payment_timeout));
                return;
            }
            pollAttempts++;
            final int attempt = pollAttempts;

            PaymentApiHelper.queryOrder(currentOrderId, new PaymentApiHelper.QueryCallback() {
                @Override public void onResult(final boolean paid, final String status, final long expireAtSeconds) {
                    runOnUiThread(() -> {
                        if (activatedThisOrder) return;
                        if (paid) {
                            if (expireAtSeconds > 0) {
                                trialManager.markOrderActivated(currentOrderId);
                                trialManager.syncSubscriptionEndFromServer(expireAtSeconds);
                                activatedThisOrder = true;
                                handler.removeCallbacks(pollRunnable);
                                showPaymentSuccessDialog();
                            } else if (trialManager.activateSubscriptionOnce("monthly".equals(selectedPlan) ? 1 : 12, currentOrderId)) {
                                activatedThisOrder = true;
                                handler.removeCallbacks(pollRunnable);
                                showPaymentSuccessDialog();
                            } else {
                                activatedThisOrder = true;
                                handler.removeCallbacks(pollRunnable);
                                tvStatus.setText("该订单已处理过，不会重复增加会员时长。\n会员有效期：" + trialManager.getSubscriptionEndText());
                                Toast.makeText(PaymentActivity.this, "订单已处理过", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            tvStatus.setText(getString(R.string.waiting_payment) + attempt + "\n状态：" + status);
                            handler.postDelayed(pollRunnable, 3000);
                        }
                    });
                }

                @Override public void onError(final String error) {
                    runOnUiThread(() -> {
                        tvStatus.setText(getString(R.string.query_error) + "\n" + error);
                        handler.postDelayed(pollRunnable, 3000);
                    });
                }
            });
        }
    };

    private void showPaymentSuccessDialog() {
        tvStatus.setText(getString(R.string.payment_success) + "\n会员有效期：" + trialManager.getSubscriptionEndText());
        new AlertDialog.Builder(PaymentActivity.this)
                .setTitle(getString(R.string.payment_success))
                .setMessage(getString(R.string.payment_success_message) + "\n会员有效期：" + trialManager.getSubscriptionEndText())
                .setPositiveButton(getString(R.string.ok), (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollRunnable);
    }
}
