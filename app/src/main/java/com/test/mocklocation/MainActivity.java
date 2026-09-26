package com.test.mocklocation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 1001;

    // Exact Chinese confirmation text
    private static final String CN_PREFIX = "\u6211\u5DF2\u9605\u8BFB\u548C\u7406\u89E3\u4E0A\u8FF0";
    private static final String CN_SUFFIX = "\uFF0C\u5E76\u627F\u8BFA\u4F7F\u7528\u672C\u8F6F\u4EF6\u671F\u95F4\u4E25\u683C\u9075\u5B88\u8BE5\u6761\u6B3E\uFF0C\u82E5\u6709\u8FDD\u53CD\uFF0C\u8F6F\u4EF6\u4F5C\u8005\u53CA\u6743\u76CA\u53D7\u635F\u65B9\u6709\u6743\u5411\u6211\u8FFD\u7A76\u8D23\u4EFB\u3002";

    // Exact English confirmation text
    private static final String EN_TEXT = "I have read and understood the above \"Terms of Service\", and I promise to strictly abide by these terms during the use of this Software. If I violate them, the Software author and affected parties have the right to hold me accountable.";

    private TrialManager trialManager;
    private TextView tvStatusTitle;
    private TextView tvTrialInfo;
    private ProgressBar progressBar;
    private TextView tvMockStatus;
    private TextView tvRecent1;
    private TextView tvRecent2;
    private TextView tvRecent3;
    private Button btnStartRecent1;
    private Button btnStartRecent2;
    private Button btnStartRecent3;
    private Button btnStopRecentMock;
    private RecentLocationManager recentLocationManager;
    private boolean expiredDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // === 安全保护检查 ===
        // 签名校验 + 反调试 + 反篡改
        if (!AppProtector.performSecurityChecks(this)) {
            return; // 检查失败会弹窗并退出
        }
        
        setContentView(R.layout.activity_main);

        trialManager = new TrialManager(this);
        recentLocationManager = new RecentLocationManager(this);
        trialManager.recordFirstLaunch();

        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvTrialInfo = findViewById(R.id.tvTrialInfo);
        progressBar = findViewById(R.id.progressBar);
        tvMockStatus = findViewById(R.id.tvMockStatus);
        tvRecent1 = findViewById(R.id.tvRecent1);
        tvRecent2 = findViewById(R.id.tvRecent2);
        tvRecent3 = findViewById(R.id.tvRecent3);
        btnStartRecent1 = findViewById(R.id.btnStartRecent1);
        btnStartRecent2 = findViewById(R.id.btnStartRecent2);
        btnStartRecent3 = findViewById(R.id.btnStartRecent3);
        btnStopRecentMock = findViewById(R.id.btnStopRecentMock);

        Button btnOpenMap = findViewById(R.id.btnOpenMap);
        Button btnPayment = findViewById(R.id.btnPayment);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnLanguage = findViewById(R.id.btnLanguage);

        btnOpenMap.setOnClickListener(v -> checkPermissionsAndOpenMap());
        btnPayment.setOnClickListener(v -> startActivity(new Intent(this, PaymentActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnLanguage.setOnClickListener(v -> showLanguageDialog());
        btnStartRecent1.setOnClickListener(v -> startRecentLocation(0));
        btnStartRecent2.setOnClickListener(v -> startRecentLocation(1));
        btnStartRecent3.setOnClickListener(v -> startRecentLocation(2));
        btnStopRecentMock.setOnClickListener(v -> stopRecentMock());

        requestAllPermissions();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("agreed_terms", false)) {
            showTermsDialog();
        }

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        checkServerLicense(false);
        if (!canUseByServerOrLocal() && !expiredDialogShown) {
            expiredDialogShown = true;
            showExpiredDialog(false);
        }
    }

    /**
     * Check if user input matches either Chinese or English confirmation text
     * Uses normalized comparison to handle different quote styles and whitespace
     */
    private String normalizeStr(String s) {
        return s.replace("\u201C", "\"").replace("\u201D", "\"")
            .replace("\u2018", "'").replace("\u2019", "'")
            .replace("\u3000", " ").replace("\t", " ")
            .replaceAll("\\s+", " ").trim();
    }

    private boolean matchesTerms(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return false;
        String cnFull = CN_PREFIX + "\u300A\u4F7F\u7528\u6761\u6B3E\u300B" + CN_SUFFIX;
        String normInput = normalizeStr(trimmed);
        String normCn = normalizeStr(cnFull);
        String normEn = normalizeStr(EN_TEXT);
        return normInput.equals(normCn) || normInput.equals(normEn);
    }

    private void showTermsDialog() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("\u4F7F\u7528\u6761\u6B3E");
        tvTitle.setTextSize(20);
        tvTitle.setTextIsSelectable(true);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextColor(Color.parseColor("#1976D2"));
        container.addView(tvTitle);

        // Intro paragraph with clickable links
        String introBase = "test Location (\u4EE5\u4E0B\u7B80\u79F0\"\u672C\u8F6F\u4EF6\") \u662F\u7531 test Location \u5F00\u53D1\u56E2\u961F\n" +
                "(\u8054\u7CFB\u65B9\u5F0F: aiaiks720@gmail.com \uFF0C\u4EE5\u4E0B\u7B80\u79F0\"\u672C\u8F6F\u4EF6\u4F5C\u8005\"\u6216\"\u6211\u4EEC\") \u5F00\u53D1\u7684\u4E00\u6B3EAndroid\u5E73\u53F0\u4E0A\u5E2E\u52A9\u5F00\u53D1\u4EBA\u5458\u7B49\u4E13\u4E1A\u4EBA\u58EB\u8FDB\u884C\u8F6F\u4EF6\u5F00\u53D1\u3001\u6E38\u620F\u5F00\u53D1\u3001\u529F\u80FD\u8C03\u8BD5\u3001\u5E94\u7528\u517C\u5BB9\u6027\u6D4B\u8BD5\u7B49\u4F4D\u7F6E\u4FE1\u606F\u76F8\u5173\u529F\u80FD\u7684\u5F00\u53D1\u8C03\u8BD5\u5DE5\u5177\u3002\n\n" +
                "\u60A8\u5728\u4F7F\u7528\u672C\u8F6F\u4EF6\u524D, \u8BF7\u52A1\u5FC5\u5148\u901A\u8FC7\u300A\u7528\u6237\u534F\u8BAE\u300B\u548C\u300A\u9690\u79C1\u653F\u7B56\u300B\u4E86\u89E3\u6211\u4EEC\u6536\u96C6\u3001\u4F7F\u7528\u3001\u5B58\u50A8\u548C\u5171\u4EAB\u4E2A\u4EBA\u4FE1\u606F\u7684\u60C5\u51B5\uFF0C\u4EE5\u53CA\u60A8\u6240\u4EAB\u6709\u7684\u76F8\u5173\u6743\u5229\uFF0C\u5E76\u8BA4\u771F\u9605\u8BFB\u548C\u7406\u89E3\u4E0B\u5217\u6458\u53D6\u81EA\u300A\u7528\u6237\u534F\u8BAE\u300B\u91CC\u7684\u727B\u522B\u7740\u91CD\u63D0\u9192\u60A8\u7684\u6761\u6B3E(\u7BC7\u5E45\u8F83\u957F \uFF0C\u8BF7\u6ED1\u52A8\u9875\u9762\u8FDB\u884C\u9605\u8BFB)\uFF0C\u70B9\u51FB\u5F39\u6846\u5E95\u90E8\u7684\"\u540C\u610F\" \u6309\u94AE\u5373\u4EE3\u8868\u60A8\u63A5\u53D7\u5168\u90E8\u534F\u8BAE\u548C\u6761\u6B3E\uFF0C\u5E76\u53EF\u4EE5\u5F00\u59CB\u4F7F\u7528\u672C\u8F6F\u4EF6\u3002";

        SpannableString spanIntro = new SpannableString(introBase);

        // Make 《用户协议》 clickable
        int uaIdx = introBase.indexOf("\u300A\u7528\u6237\u534F\u8BAE\u300B");
        if (uaIdx >= 0) {
            spanIntro.setSpan(new ClickableSpan() {
                @Override public void onClick(View w) { showUserAgreement(); }
                @Override public void updateDrawState(TextPaint ds) {
                    ds.setColor(Color.parseColor("#1976D2")); ds.setUnderlineText(true); ds.setFakeBoldText(true);
                }
            }, uaIdx, uaIdx + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Make 《隐私政策》 clickable
        int ppIdx = introBase.indexOf("\u300A\u9690\u79C1\u653F\u7B56\u300B");
        if (ppIdx >= 0) {
            spanIntro.setSpan(new ClickableSpan() {
                @Override public void onClick(View w) { showPrivacyPolicy(); }
                @Override public void updateDrawState(TextPaint ds) {
                    ds.setColor(Color.parseColor("#1976D2")); ds.setUnderlineText(true); ds.setFakeBoldText(true);
                }
            }, ppIdx, ppIdx + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView tvIntro = new TextView(this);
        tvIntro.setText(spanIntro);
        tvIntro.setTextSize(13);
        tvIntro.setTextIsSelectable(true);
        tvIntro.setLineSpacing(4, 1.2f);
        tvIntro.setTextColor(Color.parseColor("#333333"));
        tvIntro.setMovementMethod(LinkMovementMethod.getInstance());
        LinearLayout.LayoutParams introP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introP.topMargin = 20;
        tvIntro.setLayoutParams(introP);
        container.addView(tvIntro);

        // Highlighted terms (exact content from the file)
        TextView tvTerms = new TextView(this);
        tvTerms.setText(getTermsContent());
        tvTerms.setTextSize(13);
        tvTerms.setTextIsSelectable(true);
        tvTerms.setLineSpacing(4, 1.2f);
        tvTerms.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams termsP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        termsP.topMargin = 15;
        tvTerms.setLayoutParams(termsP);
        container.addView(tvTerms);

        // Separator
        TextView tvSep = new TextView(this);
        tvSep.setText("\n\u8BF7\u8F93\u5165\u4E0B\u5217\u6587\u5B57\uFF08\u4E2D\u6587\u6216\u82F1\u6587\u4E8C\u9009\u4E00\uFF09:");
        tvSep.setTextSize(14);
        tvSep.setTextIsSelectable(true);
        tvSep.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sepP.topMargin = 20;
        tvSep.setLayoutParams(sepP);
        container.addView(tvSep);

        // Chinese text to type
        String cnDisplay = CN_PREFIX + "\u300A\u4F7F\u7528\u6761\u6B3E\u300B" + CN_SUFFIX;
        TextView tvCn = new TextView(this);
        tvCn.setText("\n\u4E2D\u6587\u7248:\n" + cnDisplay);
        tvCn.setTextSize(12);
        tvCn.setTextIsSelectable(true);
        tvCn.setTextColor(Color.parseColor("#1976D2"));
        tvCn.setPadding(0, 8, 0, 8);
        container.addView(tvCn);

        // English text to type
        TextView tvEn = new TextView(this);
        tvEn.setText("English:\n" + EN_TEXT);
        tvEn.setTextSize(12);
        tvEn.setTextIsSelectable(true);
        tvEn.setTextColor(Color.parseColor("#1976D2"));
        tvEn.setPadding(0, 8, 0, 8);
        container.addView(tvEn);

        // Input field
        EditText etInput = new EditText(this);
        etInput.setHint("\u8BF7\u5728\u6B64\u8F93\u5165\u4E0A\u65B9\u6587\u5B57 / Type here");
        etInput.setTextSize(13);
        etInput.setTextIsSelectable(true);
        etInput.setSingleLine(false);
        etInput.setLines(3);
        etInput.setGravity(Gravity.TOP | Gravity.START);
        etInput.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams inputP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputP.topMargin = 15;
        etInput.setLayoutParams(inputP);
        container.addView(etInput);

        scrollView.addView(container);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollView);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "\u4E0D\u540C\u610F", (d, w) -> finish());
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "\u540C\u610F", (d, w) -> {});

        dialog.show();

        Button agreeBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        agreeBtn.setEnabled(false);
        agreeBtn.setTextColor(Color.GRAY);

        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean ok = matchesTerms(s.toString());
                agreeBtn.setEnabled(ok);
                agreeBtn.setTextColor(ok ? Color.parseColor("#1976D2") : Color.GRAY);
            }
        });

        agreeBtn.setOnClickListener(v -> {
            if (matchesTerms(etInput.getText().toString())) {
                getSharedPreferences("settings", MODE_PRIVATE)
                        .edit().putBoolean("agreed_terms", true).apply();
                dialog.dismiss();
                Toast.makeText(this, "\u5DF2\u540C\u610F\u4F7F\u7528\u6761\u6B3E", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "\u8BF7\u8F93\u5165\u6B63\u786E\u7684\u786E\u8BA4\u6587\u5B57", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Exact terms content from 使用条款.txt */
    private String getTermsContent() {
        return "\u727B\u522B\u63D0\u9192\u60A8\u7684\u6761\u6B3E :\n\n" +
                "1. \u60A8\u5728\u4F7F\u7528\u672C\u8F6F\u4EF6\u5404\u9879\u529F\u80FD\u524D\u52A1\u5FC5\u5145\u5206\u5BA1\u614E\u8BC4\u4F30Android\u7CFB\u7EDF\u7684\u5404\u9879\u6743\u9650\u53CA\u5176\u5229\u5F0A (\u5E94\u727B\u522B\u6CE8\u610F\u672C\u8F6F\u4EF6\u5185 NOROOT \u6A21\u5F0F\u6240\u4F9D\u8D56\u7684\u5F00\u53D1\u8005\u9009\u9879\uFF0C\u4EE5\u53CA\"ROOT\u6A21\u5F0F\"\u6240\u4F9D\u8D56\u7684ROOT\u6743\u9650\u548C\u5BBD\u5BB9SELinux) \uFF0C\u5F53\u60A8\u6388\u4E88\u672C\u8F6F\u4EF6\u76F8\u5E94\u6743\u9650\u540E\uFF0C\u8868\u793A\u60A8\u5141\u8BB8\u672C\u8F6F\u4EF6\u884C\u4F7F\u76F8\u5E94\u529F\u80FD\u884C\u4E3A\uFF0C\u60A8\u5C06\u72EC\u7ACB\u627F\u62C5\u56E0\u6B64\u4EA7\u751F\u7684\u4E00\u5207\u98CE\u9669\u548C\u540E\u679C\u3002(\u6458\u81EA\u7B2C5.2.2\u6761)\n\n" +
                "2. \u60A8\u786E\u4FDD\u4E0D\u5F97\u901A\u8FC7\u672C\u8F6F\u4EF6\u4EE5\u4EFB\u4F55\u65B9\u5F0F\u8FDB\u884C\u975E\u6CD5\u3001\u6B3A\u8BC8\u3001\u4FB5\u72AF\u7B2C\u4E09\u65B9\u6743\u76CA\u3001\u8FDD\u53CD\u5176\u4ED6\u5E94\u7528\u670D\u52A1\u6761\u6B3E\u7684\u884C\u4E3A\uFF0C\u5305\u62EC\u4F46\u4E0D\u9650\u4E8E\u7528\u4E8E\u529E\u516C\u6253\u5361\u3001\u7B7E\u5230\u3001\u7F51\u7EA6\u8F66\u3001\u6821\u56ED\u8DD1\u3001\u914D\u9001\u670D\u52A1\u76F8\u5173\u7684\u884C\u4E3A\u3002\u6211\u4EEC\u4FDD\u7559\u5411\u76D1\u7BA1\u90E8\u95E8\u4E3E\u62A5\u60A8\u8FDD\u6CD5\u8FDD\u89C4\u884C\u4E3A\u7684\u6743\u5229\u3002(\u6458\u81EA\u7B2C5.2.3\u6761)\n\n" +
                "3. \u60A8\u77E5\u6089\u5E76\u540C\u610F\u672C\u8F6F\u4EF6\u4EC5\u9650\u60A8\u7528\u4E8E\u5408\u6CD5\u7684\u8F6F\u4EF6\u5F00\u53D1\u3001\u529F\u80FD\u8C03\u8BD5\u3001\u5E94\u7528\u517C\u5BB9\u6027\u6D4B\u8BD5\u7B49\u4E13\u4E1A\u76EE\u7684\u3002\u5E76\u4E14\u672A\u7ECF\u6211\u4EEC\u540C\u610F\uFF0C\u60A8\u4E0D\u5F97\u57FA\u4E8E\u672C\u8F6F\u4EF6\u8FDB\u4E00\u6B65\u5BF9\u5916\u63D0\u4F9B\u670D\u52A1\u3002(\u6458\u81EA\u7B2C5.2.1/5.2.6\u6761)\n\n" +
                "4. \u60A8\u786E\u4FDD\u4E0D\u5F97\u901A\u8FC7\u4EFB\u4F55\u65B9\u5F0F\u653B\u51FB\u672C\u8F6F\u4EF6\uFF0C\u4E0D\u5F97\u4FEE\u6539\u3001\u6539\u7F16\u3001\u7FFB\u8BD1\u672C\u8F6F\u4EF6\u6240\u4F7F\u7528\u7684\u8F6F\u4EF6\u3001\u6280\u672F\u3001\u6570\u636E\u3001\u6750\u6599\u7B49\uFF0C\u4E0D\u5F97\u901A\u8FC7\u53CD\u5411\u5DE5\u7A0B\u3001\u53CD\u7F16\u8BD1\u3001\u53CD\u6C47\u7F16\u6216\u5176\u4ED6\u7C7B\u4F3C\u884C\u4E3A\u83B7\u5F97\u672C\u8F6F\u4EF6\u6D89\u53CA\u7684\u6E90\u4EE3\u7801\uFF0C\u4E0D\u5F97\u5728\u672A\u7ECF\u6211\u4EEC\u4E66\u9762\u6388\u6743\u7684\u60C5\u51B5\u4E0B\u6253\u5305\u3001\u53D1\u5E03\u672C\u8F6F\u4EF6\u6216\u672C\u8F6F\u4EF6\u7684\u53D8\u4F53\uFF0C\u5426\u5219\u7531\u6B64\u5F15\u8D77\u7684\u4E00\u5207\u6CD5\u5F8B\u540E\u679C\u7531\u60A8\u8D1F\u8D23\uFF0C\u6211\u4EEC\u6709\u6743\u4F9D\u6CD5\u8FFD\u7A76\u60A8\u7684\u6CD5\u5F8B\u8D23\u4EFB\u3002(\u6458\u81EA\u7B2C5.2.4\u6761)\n\n" +
                "5. \u60A8\u4F7F\u7528\u672C\u8F6F\u4EF6\u4E0B\u7684\u5404\u9879\u529F\u80FD\u65F6\uFF0C\u987B\u4E86\u89E3\u548C\u9075\u5FAA\u7531\u6211\u4EEC\u5728\u8F6F\u4EF6\u670D\u52A1\u754C\u9762\u3001\u8F6F\u4EF6\u6587\u6863\u5185\u660E\u786E\u7684\u4F7F\u7528\u89C4\u5219; \u82E5\u7531\u4E8E\u60A8\u8FDD\u53CD\u4F7F\u7528\u89C4\u5219\u6216\u672A\u4E86\u89E3\u4F7F\u7528\u89C4\u5219 \uFF0C\u4EE5\u81F4\u4EA7\u751F\u6743\u76CA\u635F\u5BB3\u7684\uFF0C\u987B\u7531\u60A8\u81EA\u884C\u627F\u62C5\u3002(\u6458\u81EA\u7B2C5.2.8\u6761)\n\n" +
                "6. \u6211\u4EEC\u6709\u6743\u4E3B\u52A8\u8BC6\u522B\u5E76\u5BF9\u5DF2\u77E5\u7684\u3001\u6613\u88AB\u7528\u4E8E\u975E\u6CD5\u3001\u6B3A\u8BC8\u3001\u4FB5\u72AF\u7B2C\u4E09\u65B9\u6743\u76CA\u6216\u8FDD\u53CD\u5176\u4ED6\u5E94\u7528\u670D\u52A1\u6761\u6B3E\u7684\u9AD8\u98CE\u9669\u5E94\u7528 (APP) \uFF0C\u91C7\u53D6\u6280\u672F\u63AA\u65BD\u8FDB\u884C\u529F\u80FD\u4F5C\u7528\u5C4F\u853D\uFF0C\u5E76\u540C\u6B65\u66F4\u65B0\u300A\u5C4F\u853D\u4F5C\u7528\u7684APP\u540D\u5355\u300B\uFF0C\u6B64\u540D\u5355\u5C06\u6839\u636E\u98CE\u9669\u8BC4\u4F30\u52A8\u6001\u8C03\u6574\uFF0C\u6211\u4EEC\u8FDB\u884C\u5C4F\u853D\u64CD\u4F5C\u65E8\u5728\u5C65\u884C\u5E73\u53F0\u8D23\u4EFB\uFF0C\u65E0\u9700\u53E6\u884C\u901A\u77E5\u60A8\uFF0C\u60A8\u5BF9\u6B64\u8868\u793A\u7406\u89E3\u548C\u540C\u610F\uFF0C\u60A8\u53EF\u4EE5\u968F\u65F6\u67E5\u770B\u5DF2\u7ECF\u88AB\u5C4F\u853D\u4F5C\u7528\u7684APP\u540D\u5355\u3002\u60A8\u4E0D\u5F97\u5C1D\u8BD5\u7ED5\u8FC7\u6216\u7834\u574F\u6211\u4EEC\u7684\u5C4F\u853D\u63AA\u65BD\uFF0C\u5426\u5219\u5C06\u627F\u62C5\u672C\u534F\u8BAE\u7B2C\u516B\u6761\u6240\u8FF0\u7684\u4E25\u91CD\u8FDD\u7EA6\u8D23\u4EFB\u3002(\u6458\u81EA\u7B2C5.3\u6761)\n\n" +
                "* \u672C\u8F6F\u4EF6\u4F5C\u8005\u4ECE\u672A\u5728\u4EFB\u4F55\u6E20\u9053\u548C\u5E73\u53F0\u53D1\u5E03\u8FC7\u6709\u5173\u672C\u8F6F\u4EF6\u7684\u9488\u5BF9\u6027\u5BA3\u4F20\u548C\u6559\u7A0B\uFF0C\u4E5F\u672A\u6388\u6743\u8FC7\u4EFB\u4F55\u4EE3\u7406\u5546\uFF0C\u8BF7\u6CE8\u610F\u7504\u522B\u8C28\u9632\u4E0A\u5F53\u53D7\u9A97\u3002\n\n" +
                "* \u544A\u7B2C\u4E09\u65B9: \u4E3A\u9632\u6B62\u4ED6\u4EBA\u8FDD\u53CD\u672C\u534F\u8BAE\u4E0D\u5F53\u4F7F\u7528\u672C\u8F6F\u4EF6\u5BFC\u81F4\u60A8(\u4E2A\u4EBA\u6216\u4F01\u4E1A)\u7684\u6743\u76CA\u53D7\u635F\uFF0C\u60A8\u53EF\u4EE5\u53C2\u8003\u6211\u4EEC\u62AB\u9732\u7684\u300A\u4E3B\u52A8\u9632\u8303\u63AA\u65BD\u300B\u8FDB\u884C\u4E3B\u52A8\u89C4\u907F\u3002\n\n" +
                "——————————————————\n\n" +
                "Specially Highlighted Terms:\n\n" +
                "1. Before using any features of this Software, you must fully and carefully evaluate the permissions of the Android system and their pros and cons (pay special attention to the Developer Options on which NOROOT mode depends, and the ROOT permissions and permissive SELinux on which ROOT mode depends). When you grant this Software the corresponding permissions, you allow this Software to exercise the corresponding functional behaviors, and you will independently bear all risks and consequences arising therefrom. (Excerpted from Article 5.2.2)\n\n" +
                "2. You must not use this Software for any illegal, fraudulent, third-party rights-infringing activities, or activities violating other applications' terms of service, including but not limited to office clock-in, check-in, ride-hailing, campus running, and delivery services. We reserve the right to report your illegal activities to regulatory authorities. (Excerpted from Article 5.2.3)\n\n" +
                "3. You acknowledge and agree that this Software is limited to your use for legitimate software development, feature debugging, app compatibility testing, and other professional purposes. Without our consent, you may not further provide services to others based on this Software. (Excerpted from Articles 5.2.1/5.2.6)\n\n" +
                "4. You must not attack this Software in any way, modify, adapt, or translate the software, technology, data, materials used by this Software, or obtain the source code through reverse engineering, decompilation, disassembly, or similar means. You may not package or distribute this Software or variants thereof without our written authorization. All legal consequences arising therefrom shall be borne by you, and we reserve the right to pursue your legal liability. (Excerpted from Article 5.2.4)\n\n" +
                "5. When using the features of this Software, you must understand and follow the usage rules clearly stated by us in the software service interface and software documentation. If you violate the usage rules or fail to understand them, resulting in damage to your rights and interests, you must bear the consequences yourself. (Excerpted from Article 5.2.8)\n\n" +
                "6. We reserve the right to proactively identify high-risk applications (APPs) known to be used for illegal, fraudulent, third-party rights-infringing activities or violating other applications' terms of service, and take technical measures to block their functionality while updating the \"Blocked APP List\". This list will be dynamically adjusted based on risk assessment. Our blocking operations are intended to fulfill platform responsibilities and do not require separate notification to you. You may not attempt to circumvent or undermine our blocking measures; otherwise, you will bear the severe breach of contract liability described in Article 8 of this Agreement. (Excerpted from Article 5.3)\n\n" +
                "* The Software author has never published any targeted promotion or tutorials about this Software on any channel or platform, nor authorized any agents. Please be vigilant against fraud.\n\n" +
                "* To third parties: To prevent others from improperly using this Software in violation of this Agreement, causing damage to your (individual or enterprise) rights and interests, you may refer to our disclosed \"Active Prevention Measures\" for proactive avoidance.";
    }

    private void showPrivacyPolicy() {
        String content = "test Location \u9690\u79C1\u653F\u7B56\n\n" +
                "\u6B22\u8FCE\u60A8\u4F7F\u7528 test Location \u8F6F\u4EF6\uFF01\n\n" +
                "\u6211\u4EEC\u6DF1\u77E5\u4E2A\u4EBA\u4FE1\u606F\u5BF9\u60A8\u7684\u91CD\u8981\u6027\uFF0C\u60A8\u7684\u4FE1\u8D56\u5BF9\u6211\u4EEC\u975E\u5E38\u91CD\u8981\uFF0C\u6211\u4EEC\u5C06\u4E25\u683C\u9075\u5B88\u6CD5\u5F8B\u6CD5\u89C4\u8981\u6C42\u91C7\u53D6\u76F8\u5E94\u7684\u5B89\u5168\u4FDD\u62A4\u63AA\u65BD\u3001\u4FDD\u62A4\u60A8\u7684\u4E2A\u4EBA\u4FE1\u606F\u5B89\u5168\u6027\u3002\n\n" +
                "\u5982\u5BF9\u672C\u653F\u7B56\u5185\u5BB9\u6709\u4EFB\u4F55\u7591\u95EE\uFF0C\u53EF\u901A\u8FC7\u90AE\u7BB1 aiaiks720@gmail.com \u4E0E\u6211\u4EEC\u8054\u7CFB\u3002";
        showDialog("\u9690\u79C1\u653F\u7B56 / Privacy Policy", content);
    }

    private void showUserAgreement() {
        String content = "test Location \u7528\u6237\u534F\u8BAE\n\n" +
                "\u6B22\u8FCE\u60A8\u4F7F\u7528 test Location \u8F6F\u4EF6\uFF01\n\n" +
                "\u8054\u7CFB\u90AE\u7BB1\uFF1Aaiaiks720@gmail.com\n\n" +
                "\u4E00\u3001\u5B9A\u4E49\ntest Location \u662F\u4E00\u6B3E\u5E2E\u52A9\u5F00\u53D1\u4EBA\u5458\u8FDB\u884C\u8F6F\u4EF6\u5F00\u53D1\u3001\u529F\u80FD\u8C03\u8BD5\u3001\u5E94\u7528\u517C\u5BB9\u6027\u6D4B\u8BD5\u7B49\u4F4D\u7F6E\u4FE1\u606F\u76F8\u5173\u529F\u80FD\u7684\u5F00\u53D1\u8C03\u8BD5\u5DE5\u5177APP\u3002\n\n" +
                "\u4E8C\u3001\u4F7F\u7528\u89C4\u8303\n\u60A8\u5E94\u9075\u5B88\u76F8\u5173\u6CD5\u5F8B\u6CD5\u89C4\uFF0C\u4E0D\u5F97\u5229\u7528\u672C\u8F6F\u4EF6\u4ECE\u4E8B\u8FDD\u6CD5\u8FDD\u89C4\u6D3B\u52A8\u3002\n\n" +
                "\u4E09\u3001\u514D\u8D23\u58F0\u660E\n\u672C\u8F6F\u4EF6\u4EC5\u4F9B\u5F00\u53D1\u6D4B\u8BD5\u7528\u9014\uFF0C\u7528\u6237\u5E94\u81EA\u884C\u627F\u62C5\u4F7F\u7528\u672C\u8F6F\u4EF6\u4EA7\u751F\u7684\u4E00\u5207\u540E\u679C\u3002\n\n" +
                "\u56DB\u3001\u8054\u7CFB\u65B9\u5F0F\n\u5982\u6709\u4EFB\u4F55\u95EE\u9898\uFF0C\u8BF7\u901A\u8FC7\u90AE\u7BB1 aiaiks720@gmail.com \u4E0E\u6211\u4EEC\u8054\u7CFB\u3002";
        showDialog("\u7528\u6237\u534F\u8BAE / User Agreement", content);
    }

    private void showDialog(String title, String content) {
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setPadding(50, 30, 50, 20);
        tv.setLineSpacing(6, 1.3f);
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle(title).setView(sv)
                .setPositiveButton("\u8FD4\u56DE", null).show();
    }

    private void requestAllPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            perms = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        requestPermissions(perms, REQUEST_PERMISSIONS);
    }

    private void checkPermissionsAndOpenMap() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "\u8BF7\u5148\u6388\u4E88\u5B9A\u4F4D\u6743\u9650", Toast.LENGTH_SHORT).show();
            requestAllPermissions();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("agreed_terms", false)) {
            Toast.makeText(this, "\u8BF7\u5148\u540C\u610F\u4F7F\u7528\u6761\u6B3E", Toast.LENGTH_SHORT).show();
            showTermsDialog();
            return;
        }
        if (!canUseByServerOrLocal()) {
            showExpiredDialog(false);
            return;
        }
        checkServerLicense(true);
    }

    private void updateUI() {
        if (!canUseByServerOrLocal()) {
            try {
                Intent intent = new Intent(this, MockLocationService.class);
                intent.setAction(MockLocationService.ACTION_STOP);
                startService(intent);
            } catch (Exception ignored) {}
            tvMockStatus.setText("会员已到期，停止模拟状态");
            tvMockStatus.setTextColor(0xFFF44336);
        } else if (MockLocationService.isServiceRunning) {
            tvMockStatus.setText("\u6A21\u62DF\u8FD0\u884C\u4E2D: " + String.format("%.4f, %.4f",
                    MockLocationService.mockLatitude, MockLocationService.mockLongitude));
            tvMockStatus.setTextColor(0xFF4CAF50);
        } else {
            tvMockStatus.setText("\u672A\u542F\u52A8");
            tvMockStatus.setTextColor(0xFFF44336);
        }
        if (trialManager.hasActiveSubscription()) {
            tvStatusTitle.setText("\u4F1A\u5458\u5DF2\u6FC0\u6D3B");
            tvStatusTitle.setTextColor(0xFF4CAF50);
            tvTrialInfo.setText("会员状态正常；有效期请在 设置 → 会员有效期 查看");
            progressBar.setVisibility(View.GONE);
        } else if (trialManager.isTrialExpired()) {
            tvStatusTitle.setText("\u8BD5\u7528\u5DF2\u8FC7\u671F");
            tvStatusTitle.setTextColor(0xFFF44336);
            tvTrialInfo.setText("会员/试用已到期，打开地图和开始模拟功能已停用，请开通会员");
            progressBar.setVisibility(View.GONE);
        } else {
            int remaining = trialManager.getRemainingTrialDays();
            tvStatusTitle.setText("\u514D\u8D39\u8BD5\u7528\u4E2D");
            tvStatusTitle.setTextColor(0xFF1976D2);
            tvTrialInfo.setText("\u5269\u4F59\u8BD5\u7528\u5929\u6570\uFF1A" + remaining + "\u5929");
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMax(trialManager.getTotalTrialDays());
            progressBar.setProgress(remaining);
        }
        updateRecentLocationsUI();
    }

    private void updateRecentLocationsUI() {
        List<RecentLocationManager.LocationItem> items = recentLocationManager.getRecentLocations();
        bindRecentLocation(0, items.size() > 0 ? items.get(0) : null, tvRecent1, btnStartRecent1);
        bindRecentLocation(1, items.size() > 1 ? items.get(1) : null, tvRecent2, btnStartRecent2);
        bindRecentLocation(2, items.size() > 2 ? items.get(2) : null, tvRecent3, btnStartRecent3);
    }

    private void bindRecentLocation(int index, RecentLocationManager.LocationItem item, TextView textView, Button button) {
        if (item == null) {
            String empty = index == 0 ? "暂无最近地点" : (index == 1 ? "暂无第二个最近地点" : "暂无第三个最近地点");
            textView.setText(empty);
            textView.setTextColor(0xFF999999);
            button.setEnabled(false);
            button.setAlpha(0.45f);
            return;
        }
        textView.setText(item.label + "  " + String.format("%.6f, %.6f", item.lat, item.lng));
        textView.setTextColor(0xFF333333);
        boolean enabled = canUseByServerOrLocal();
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private boolean isMockLocationAppSet() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
            lm.addTestProvider(android.location.LocationManager.GPS_PROVIDER,
                    false, false, false, false, true, true, true,
                    android.location.Criteria.POWER_LOW, android.location.Criteria.ACCURACY_FINE);
            lm.removeTestProvider(android.location.LocationManager.GPS_PROVIDER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showMockLocationGuide() {
        new AlertDialog.Builder(this)
                .setTitle("请设置模拟位置应用")
                .setMessage("请在开发者选项中，把 test location 设为'选择模拟位置信息应用'，然后再启动模拟。")
                .setPositiveButton("去设置", (d, w) -> {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
                    catch (Exception e) { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
                })
                .setNegativeButton("稍后", null)
                .show();
    }

    private void startRecentLocation(int index) {
        List<RecentLocationManager.LocationItem> items = recentLocationManager.getRecentLocations();
        if (index < 0 || index >= items.size()) {
            Toast.makeText(this, "暂无可启动的最近地点", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予定位权限", Toast.LENGTH_SHORT).show();
            requestAllPermissions();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("agreed_terms", false)) {
            Toast.makeText(this, "请先同意使用条款", Toast.LENGTH_SHORT).show();
            showTermsDialog();
            return;
        }
        if (!canUseByServerOrLocal()) {
            showExpiredDialog(false);
            return;
        }
        if (!isMockLocationAppSet()) {
            showMockLocationGuide();
            return;
        }
        RecentLocationManager.LocationItem item = items.get(index);
        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_SET_LOCATION);
        intent.putExtra("lat", item.lat);
        intent.putExtra("lng", item.lng);
        startService(intent);
        recentLocationManager.saveRecent(item.lat, item.lng);
        tvMockStatus.setText("模拟运行中: " + String.format("%.4f, %.4f", item.lat, item.lng));
        tvMockStatus.setTextColor(0xFF4CAF50);
        updateRecentLocationsUI();
        Toast.makeText(this, "已启动最近地点模拟", Toast.LENGTH_SHORT).show();
        checkServerLicense(false);
    }

    private void stopRecentMock() {
        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_STOP);
        startService(intent);
        tvMockStatus.setText("未启动");
        tvMockStatus.setTextColor(0xFFF44336);
        Toast.makeText(this, "已停止模拟", Toast.LENGTH_SHORT).show();
    }


    private boolean canUseByServerOrLocal() {
        return trialManager.canUseApp() || LicenseApiHelper.canUseCached(this);
    }

    private void checkServerLicense(final boolean openMapAfterOk) {
        LicenseApiHelper.checkLicense(this, trialManager.getDeviceCode(), (canUse, mustUpgrade, message) -> runOnUiThread(() -> {
            if (mustUpgrade) {
                showUpgradeDialog();
                return;
            }
            updateUI();
            if (openMapAfterOk) {
                if (canUseByServerOrLocal()) startActivity(new Intent(this, MapActivity.class));
                else showExpiredDialog(false);
            }
        }));
    }

    private void showUpgradeDialog() {
        String msg = "发现新版本" + LicenseApiHelper.getLatestVersion(this) + "，请升级后继续使用。\n\n" + LicenseApiHelper.getChangelog(this);
        new AlertDialog.Builder(this)
                .setTitle("需要升级")
                .setMessage(msg)
                .setPositiveButton("立即下载", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LicenseApiHelper.getDownloadUrl(this)))))
                .setCancelable(false)
                .show();
    }

    private void showExpiredDialog(boolean forceExit) {
        new AlertDialog.Builder(this)
                .setTitle("请购买增加使用时间")
                .setMessage("会员/试用已到期，打开地图和开始模拟功能已停用。\n\n月度会员：¥19.8/月\n年度会员：¥168/年\n\n您可以关闭弹窗，也可以进入订阅界面。")
                .setPositiveButton("订阅", (d, w) -> startActivity(new Intent(this, PaymentActivity.class)))
                .setNegativeButton("关闭", null)
                .setCancelable(true).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "\u9700\u8981\u5B9A\u4F4D\u6743\u9650", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }
}
