package com.frml.api.util;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeUtils {
    public static String unicodeToChinese(String str) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            char ch = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf(ch));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void convertUnicodeToChinese(ObjectNode jsonNode, String fieldName) {
        if (jsonNode.has(fieldName)) {
            String value = jsonNode.get(fieldName).asText();
            value = unicodeToChinese(value);
            jsonNode.put(fieldName, value);
        }
    }
}
