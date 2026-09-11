package com.frml.api.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonUtil {
    /**
     * 解析字符串为 List<String>：
     * 1. 若字符串是 JSON 数组（以 [ 开头且以 ] 结尾），则解析数组元素
     * 2. 若字符串是普通字符串（非 JSON 数组），则返回包含该字符串的单元素列表
     * 3. 支持非标准JSON数组（元素无引号）的解析
     */
    public static List<String> parseJsonArrayOrString(String jsonArrayStr) {
        // 入参为 null 或空字符串，返回空列表
        if (jsonArrayStr == null || (jsonArrayStr = jsonArrayStr.trim()).isEmpty()) {
            return new ArrayList<>();
        }

        // 判断是否为数组格式（以 [ 开头且以 ] 结尾）
        if (jsonArrayStr.startsWith("[") && jsonArrayStr.endsWith("]")) {
            try {
                // 尝试解析为标准JSON数组
                JSONArray jsonArray = JSON.parseArray(jsonArrayStr);
                List<String> resultList = new ArrayList<>(jsonArray.size());
                Iterator<Object> iterator = jsonArray.iterator();
                while (iterator.hasNext()) {
                    resultList.add(iterator.next().toString());
                }
                return resultList;
            } catch (Exception e) {
                // 标准JSON解析失败，尝试解析非标准数组（元素无引号）
                //System.err.printf("标准JSON数组解析失败，尝试解析非标准数组：%s%n", e.getMessage());
                // 去除首尾的括号
                String content = jsonArrayStr.substring(1, jsonArrayStr.length() - 1).trim();
                if (content.isEmpty()) {
                    return new ArrayList<>();
                }
                // 按逗号分割元素，并去除每个元素的前后空格
                String[] elements = content.split(",");
                List<String> resultList = new ArrayList<>(elements.length);
                for (String elem : elements) {
                    resultList.add(elem.trim());
                }
                return resultList;
            }
        } else {
            // 非数组格式，直接作为单元素列表返回
            List<String> singleList = new ArrayList<>();
            singleList.add(jsonArrayStr);
            return singleList;
        }
    }

    public static void main(String[] args) {
        // 场景1：JSON 无转义符（Java 直接写合法 JSON）
        String case1 = "[\"a\",\"b\",\"c,d\"]";
        // 实际 JSON 内容：["a","b","c,d"] → 合法
        System.out.println("场景1（无转义符）：" + parseJsonArrayOrString(case1)); // [a, b, c,d]

        // 场景2：带转义符的合法JSON（Java正确转义）
        // 目标JSON内容：["a", "b", "x\\y"]（JSON中用\\表示\）
        // Java中需将每个"转义为\"，每个\转义为\\ → 最终字符串：
        String case2 = "[\"a\", \"b\"]";
        System.out.println("场景2（带转义符）：" + parseJsonArrayOrString(case2));
        // 输出：[a, b, x\y]（正确解析）

        // 场景3：元素内包含特殊转义（JSON 合法格式）
        // 目标 JSON：["a\"b", "hello\\world"] → 合法（a"b 用 \" 转义引号，hello\world 用 \\ 转义反斜杠）
        String case3 = "[\"a\\\"b\", \"hello\\\\world\"]";
        System.out.println("场景3（元素含特殊转义）：" + parseJsonArrayOrString(case3)); // [a"b, hello\world]

        // 场景4：无效 JSON 格式（
        String case4 = "[cgbmzj,dsz]";
        System.out.println("场景4：" + parseJsonArrayOrString(case4));

    }
}
