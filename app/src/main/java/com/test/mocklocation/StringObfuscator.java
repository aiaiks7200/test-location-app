package com.test.mocklocation;

/**
 * 字符串混淆工具
 * 将敏感字符串拆分存储，运行时拼接，防止静态分析直接搜索到关键字符串
 */
public class StringObfuscator {
    
    // 服务器地址拆分存储
    private static final String[] S_PARTS = {
        "h", "t", "t", "p", "s", ":", "/", "/", 
        "d", "w", ".", "l", "o", "c", "a", "t", "i", ".", "x", "y", "z"
    };
    
    // API 路径拆分
    private static final String[] A_PARTS = {
        "/", "a", "p", "i", "/", "l", "i", "c", "e", "n", "s", "e", "/", "c", "h", "e", "c", "k"
    };
    
    private static final String[] B_PARTS = {
        "/", "a", "p", "i", "/", "p", "a", "y", "/", "c", "r", "e", "a", "t", "e"
    };
    
    private static final String[] C_PARTS = {
        "/", "a", "p", "i", "/", "p", "a", "y", "/", "q", "u", "e", "r", "y"
    };
    
    private static final String[] D_PARTS = {
        "/", "a", "p", "i", "/", "p", "l", "a", "n", "s"
    };
    
    /**
     * 获取服务器地址（运行时拼接）
     */
    public static String getServerBase() {
        StringBuilder sb = new StringBuilder(S_PARTS.length);
        for (String p : S_PARTS) sb.append(p);
        return sb.toString();
    }
    
    /**
     * 获取 API 路径
     */
    public static String getLicensePath() {
        StringBuilder sb = new StringBuilder(A_PARTS.length);
        for (String p : A_PARTS) sb.append(p);
        return sb.toString();
    }
    
    public static String getPayCreatePath() {
        StringBuilder sb = new StringBuilder(B_PARTS.length);
        for (String p : B_PARTS) sb.append(p);
        return sb.toString();
    }
    
    public static String getPayQueryPath() {
        StringBuilder sb = new StringBuilder(C_PARTS.length);
        for (String p : C_PARTS) sb.append(p);
        return sb.toString();
    }
    
    public static String getPlansPath() {
        StringBuilder sb = new StringBuilder(D_PARTS.length);
        for (String p : D_PARTS) sb.append(p);
        return sb.toString();
    }
    
    /**
     * 通用字符串拆分存储
     * 用于加密其他敏感字符串
     */
    public static String decode(char[] data) {
        return new String(data);
    }
    
    /**
     * 简单的 XOR 混淆
     */
    public static String xorDecode(byte[] encoded, byte key) {
        byte[] decoded = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ key);
        }
        return new String(decoded);
    }
}
