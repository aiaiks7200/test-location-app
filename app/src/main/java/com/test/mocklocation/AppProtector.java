package com.test.mocklocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Debug;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * APP 综合保护类
 * 集成签名校验、反调试、反篡改等保护措施
 */
public class AppProtector {
    
    private static final String TAG = "AppProtector";
    private static boolean verified = false;
    
    /**
     * 执行所有保护检查
     * 应在 Application.onCreate() 或 MainActivity.onCreate() 中调用
     * @return true 通过检查，false 检测到异常
     */
    public static boolean performSecurityChecks(Context context) {
        // 1. 签名校验
        if (!SignatureVerifier.verify(context)) {
            Log.e(TAG, "Signature verification failed!");
            showAlertAndExit((Activity) context, 
                decodeString("\u7B7E\u540D\u9A8C\u8BC1\u5931\u8D25\uFF0C\u5E94\u7528\u53EF\u80FD\u88AB\u7BE1\u6539\u3002"));
            return false;
        }
        
        // 2. 反调试检测
        if (isDebuggerConnected() || isDebuggable(context)) {
            Log.e(TAG, "Debug mode detected!");
            showAlertAndExit((Activity) context,
                decodeString("\u68C0\u6D4B\u5230\u8C03\u8BD5\u6A21\u5F0F\uFF0C\u8BF7\u4F7F\u7528\u6B63\u5F0F\u7248\u672C\u3002"));
            return false;
        }
        
        // 3. Root 检测（可选，不强制退出，仅记录）
        if (isDeviceRooted()) {
            Log.w(TAG, "Rooted device detected, but allowing access");
        }
        
        // 4. Xposed/Magisk 检测（可选）
        if (isXposedDetected()) {
            Log.w(TAG, "Xposed framework detected");
        }
        
        verified = true;
        return true;
    }
    
    /**
     * 检测调试器是否已连接
     */
    private static boolean isDebuggerConnected() {
        return Debug.isDebuggerConnected();
    }
    
    /**
     * 检测是否为调试版本
     */
    private static boolean isDebuggable(Context context) {
        try {
            return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测设备是否已 Root
     */
    private static boolean isDeviceRooted() {
        // 检查常见 Root 文件
        String[] paths = {
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        };
        
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        
        // 检查 su 命令
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line != null && line.contains("su")) {
                return true;
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return false;
    }
    
    /**
     * 检测 Xposed 框架
     */
    private static boolean isXposedDetected() {
        try {
            // 检查 Xposed 相关文件
            String[] xposedPaths = {
                "/system/framework/XposedBridge.jar",
                "/system/bin/app_process.orig",
                "/system/lib/libxposed_art.so",
                "/system/lib64/libxposed_art.so"
            };
            
            for (String path : xposedPaths) {
                if (new File(path).exists()) {
                    return true;
                }
            }
            
            // 检查堆栈中是否有 Xposed
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("xposed") || 
                    element.getClassName().contains("Xposed")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return false;
    }
    
    /**
     * 显示警告并退出应用
     */
    private static void showAlertAndExit(Activity activity, String message) {
        if (activity == null || activity.isFinishing()) return;
        
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity)
                    .setTitle(decodeString("\u5B89\u5168\u8B66\u544A"))
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(decodeString("\u9000\u51FA"), (dialog, which) -> {
                        activity.finish();
                        System.exit(0);
                    })
                    .show();
            }
        });
    }
    
    /**
     * 简单的 Unicode 字符串解码（防止静态搜索）
     */
    private static String decodeString(String encoded) {
        try {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < encoded.length()) {
                if (i + 5 < encoded.length() && encoded.charAt(i) == '\\' && encoded.charAt(i + 1) == 'u') {
                    String hex = encoded.substring(i + 2, i + 6);
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                } else {
                    sb.append(encoded.charAt(i));
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Security Alert";
        }
    }
    
    /**
     * 获取当前验证状态
     */
    public static boolean isVerified() {
        return verified;
    }
    
    /**
     * 获取签名指纹（用于显示/调试）
     */
    public static String getSignatureInfo(Context context) {
        return SignatureVerifier.getShortFingerprint(context);
    }
}
