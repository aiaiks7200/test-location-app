package com.test.mocklocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private TrialManager trialManager;
    private TextView tvMembershipStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        trialManager = new TrialManager(this);
        tvMembershipStatus = (TextView) findViewById(R.id.tv_membership_status);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_subscribe).setOnClickListener(v -> startActivity(new Intent(this, PaymentActivity.class)));
        findViewById(R.id.btn_privacy).setOnClickListener(v -> showPrivacyDialog());
        findViewById(R.id.btn_agreement).setOnClickListener(v -> showAgreementDialog());
        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btn_about).setOnClickListener(v -> showAboutDialog());
        updateMembershipStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        updateMembershipStatus();
    }

    private void updateMembershipStatus() {
        String text = "会员有效期：" + trialManager.getSubscriptionEndText() + "\n设备码：" + trialManager.getDeviceCode();
        if (trialManager.hasActiveSubscription()) text += "\n状态：会员可用";
        else if (trialManager.isTrialExpired()) text += "\n状态：已到期，请增加使用时间";
        else text += "\n状态：试用剩余 " + trialManager.getRemainingTrialDays() + " 天";
        tvMembershipStatus.setText(text);
    }

    private void showPrivacyDialog() {
        String content = "test Location 隐私政策\n\n" +
                "欢迎您使用 test Location 软件！\n\n" +
                "我们深知个人信息对您的重要性，您的信赖对我们非常重要，我们将严格遵守法律法规要求采取相应的安全保护措施、保护您的个人信息安全性。\n\n" +
                "基于此，我们制定本《test Location 隐私政策》，帮助您充分了解在您参与使用本软件的过程中，我们会如何收集、使用、共享、存储和保护您的个人信息以及您可以如何管理您的个人信息。\n\n" +
                "本版本会读取 Android ID 生成设备码，用于会员与设备绑定。设备码示例：" + trialManager.getDeviceCode() + "\n\n" +
                "如对本政策内容有任何疑问、意见或建议，您可通过邮箱 aiaiks720@gmail.com 与我们联系。";
        showScrollDialog("隐私政策", content);
    }

    private void showAgreementDialog() {
        String content = "test Location 用户协议\n\n" +
                "欢迎您使用 test Location 软件！\n\n" +
                "为使用 test Location 软件，您应当阅读和遵守本用户协议。\n\n" +
                "联系邮箱：aiaiks720@gmail.com\n\n" +
                "一、定义\ntest Location 是一款帮助开发人员进行软件开发、功能调试、应用兼容性测试等位置信息相关功能的开发调试工具APP。\n\n" +
                "二、会员与设备绑定\n会员权益与本机设备码绑定，同一订单只能处理一次，重复点击订阅或重复查询已支付订单不会重复增加时长。\n\n" +
                "三、使用规范\n您应遵守相关法律法规，不得利用本软件从事违法违规活动。\n\n" +
                "四、免责声明\n本软件仅供开发测试用途，用户应自行承担使用本软件产生的一切后果。\n\n" +
                "五、联系方式\n如有任何问题，请通过邮箱 aiaiks720@gmail.com 与我们联系。";
        showScrollDialog("用户协议", content);
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "中文", "日本語", "한국어", "Русский", "Français", "Deutsch"};
        final String[] locales = {"en", "zh", "ja", "ko", "ru", "fr", "de"};
        new AlertDialog.Builder(this)
                .setTitle("选择语言 / Select Language")
                .setItems(languages, (dialog, which) -> setLocale(locales[which]))
                .show();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAboutDialog() {
        String content = "test Location\n\n" +
                "版本：1.13\n\n" +
                "一款专业的模拟定位开发调试工具\n\n" +
                "会员有效期：" + trialManager.getSubscriptionEndText() + "\n" +
                "设备码：" + trialManager.getDeviceCode() + "\n\n" +
                "联系邮箱 / Contact Email:\naiaiks720@gmail.com\n\n" +
                "© 2026 test Location. All rights reserved.";
        showScrollDialog("关于", content);
    }

    private void showScrollDialog(String title, String content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setTextSize(14);
        textView.setTextColor(Color.parseColor("#333333"));
        textView.setPadding(60, 40, 60, 20);
        textView.setLineSpacing(6, 1.3f);
        scrollView.addView(textView);
        new AlertDialog.Builder(this).setTitle(title).setView(scrollView).setPositiveButton("确定", null).show();
    }
}
