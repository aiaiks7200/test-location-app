package com.test.mocklocation;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;

/**
 * APK 签名校验器
 * 防止 APK 被重签名后二次打包
 */
public class SignatureVerifier {
    
    // 发布签名的 SHA-256 指纹（从 keystore 读取）
    private static final String EXPECTED_SIGNATURE_SHA256 = "5C:8F:FA:05:52:66:8F:13:01:12:B6:DE:07:3F:BA:2A:95:69:4E:90:D0:92:5C:4D:49:37:58:A0:1C:D5:30:8E";
    
    // 简单的字符串混淆，避免被直接搜索到
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    
    /**
     * 验证 APK 签名是否匹配
     * @return true 签名匹配，false 签名不匹配或被篡改
     */
    public static boolean verify(Context context) {
        try {
            String currentSig = getSignatureSHA256(context);
            if (currentSig == null || currentSig.isEmpty()) {
                return false;
            }
            
            // 如果没有配置预期签名，首次运行时记录（开发模式）
            if (EXPECTED_SIGNATURE_SHA256.isEmpty()) {
                return true;
            }
            
            return currentSig.equals(EXPECTED_SIGNATURE_SHA256);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取当前 APK 签名的 SHA-256 指纹
     */
    public static String getSignatureSHA256(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return null;
            }
            
            Signature signature = packageInfo.signatures[0];
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signature.toByteArray());
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) sb.append(":");
                byte b = digest[i];
                sb.append(HEX[(b >> 4) & 0x0F]);
                sb.append(HEX[b & 0x0F]);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取签名的简短指纹（用于日志/显示）
     */
    public static String getShortFingerprint(Context context) {
        String full = getSignatureSHA256(context);
        if (full == null || full.length() < 10) return "UNKNOWN";
        return full.substring(0, 10) + "...";
    }
}
