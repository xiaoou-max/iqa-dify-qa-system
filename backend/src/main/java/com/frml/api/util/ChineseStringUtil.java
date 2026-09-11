package com.frml.api.util;

import java.nio.charset.StandardCharsets;

public class ChineseStringUtil {
    public static String dealChineseString(String s) {
        try {
            // 关键：判断原始字符串是否能通过 ISO-8859-1 → UTF-8 还原为有效中文
            // 逻辑：将字符串按 ISO-8859-1 转字节，再按 UTF-8 解码，若解码后与原字符串不同，说明是乱码需修复
            String decodedString = new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            // 额外校验：解码后的字符串是否包含有效中文（UTF-8 中文的字节范围是 0xE4~0xED 等）
            if (isUtf8Chinese(decodedString) && !decodedString.equals(s)) {
                s = decodedString;
            }
        } catch (Exception e) {
            // 转码失败时使用原始值，避免问号乱码
            return s;
        }
        return s;
    }

    // 辅助方法：判断字符串是否包含 UTF-8 中文（避免误转码纯英文/数字）
    private static boolean isUtf8Chinese(String str) {
        for (char c : str.toCharArray()) {
            // UTF-8 中文的 Unicode 范围：0x4E00（一）~0x9FA5（龥）
            if (c >= 0x4E00 && c <= 0x9FA5) {
                return true;
            }
        }
        return false;
    }
}
