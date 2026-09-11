package com.frml.api.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExcelUtil {
    // 支持的日期格式（可扩展）
    private static final List<SimpleDateFormat> DATE_FORMATS = new ArrayList<>();
    static {
        DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd"));
        DATE_FORMATS.add(new SimpleDateFormat("yyyy/MM/dd"));
        DATE_FORMATS.add(new SimpleDateFormat("MM-dd-yyyy"));
        DATE_FORMATS.add(new SimpleDateFormat("yyyy年MM月dd日"));
        DATE_FORMATS.add(new SimpleDateFormat("yyyy.MM.ddHH:mm:ss"));
        DATE_FORMATS.add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"));
    }


    /**
     * 获取单元格值，支持类型校验，并去除所有空白字符（空格、换行等）
     * @param cell 单元格对象
     * @param targetType 目标类型（可选："NUMERIC"、"DATE"、"STRING"、"BOOLEAN"）
     * @return 清洗后的单元格值（无空格、无换行）
     * @throws IllegalArgumentException 类型不匹配时抛出
     */
    public static String getCellValue(Cell cell, String targetType) {
        // 1. 获取单元格原始值（字符串形式）
        String originalValue = getRawCellValue(cell);

        // 2. 清洗原始值：去除所有空格、换行符、制表符等
        String cleanedValue = cleanString(originalValue);

        // 3. 若targetType为空，直接返回清洗后的值
        if (targetType == null || targetType.trim().isEmpty()) {
            return cleanedValue;
        }

        // 4. 根据targetType校验并转换（使用清洗后的值进行校验）
        switch (targetType.toUpperCase()) {
            case "NUMERIC":
                return validateAndConvertNumeric(cleanedValue);
            case "DATE":
                return validateAndConvertDate(cleanedValue);
            case "STRING":
                return cleanedValue; // 字符串直接返回清洗后的值
            case "BOOLEAN":
                return validateAndConvertBoolean(cleanedValue);
            default:
                throw new IllegalArgumentException("不支持的目标类型：" + targetType);
        }
    }

    /**
     * 清洗字符串：去除所有空白字符（空格、换行、制表符等）
     */
    private static String cleanString(String str) {
        if (str == null) {
            return "";
        }
        // 正则替换：去除所有空白字符（包括空格、\t、\n、\r等）
        return str.replaceAll("\\s+", "");
    }

//    /**
//     * 获取单元格值，并根据目标类型校验
//     * @param cell 单元格对象
//     * @param targetType 目标类型（可选："NUMERIC"、"DATE"、"STRING"、"BOOLEAN"，为空时不校验）
//     * @return 转换后的单元格值（字符串）
//     * @throws IllegalArgumentException 类型不匹配时抛出
//     */
//    public static String getCellValue(Cell cell, String targetType) {
//        // 1. 获取单元格原始值（字符串形式）
//        String originalValue = getRawCellValue(cell);
//
//        // 2. 若targetType为空，直接返回原始值（不校验）
//        if (targetType == null || targetType.trim().isEmpty()) {
//            return originalValue;
//        }
//
//        // 3. 根据targetType校验并转换
//        switch (targetType.toUpperCase()) {
//            case "NUMERIC":
//                return validateAndConvertNumeric(originalValue);
//            case "DATE":
//                return validateAndConvertDate(originalValue);
//            case "STRING":
//                return validateAndConvertString(originalValue);
//            case "BOOLEAN":
//                return validateAndConvertBoolean(originalValue);
//            default:
//                throw new IllegalArgumentException("不支持的目标类型：" + targetType);
//        }
//    }

    /**
     * 获取单元格原始值（转为字符串，保留原始内容）
     */
    private static String getRawCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    double value = cell.getNumericCellValue();
                    return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 校验并转换为数值类型（支持文本格式的数值）
     */
    private static String validateAndConvertNumeric(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("数值类型不能为空");
        }
        // 正则匹配整数或小数（支持正负号）
        if (!value.matches("^[-+]?\\d+(\\.\\d+)?$")) {
            throw new IllegalArgumentException("值[" + value + "]不是有效的数值");
        }
        return value;
    }

    /**
     * 校验并转换为日期类型（支持文本格式的日期字符串）
     */
    private static String validateAndConvertDate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("日期类型不能为空");
        }
        // 尝试解析多种日期格式
        for (SimpleDateFormat sdf : DATE_FORMATS) {
            try {
                sdf.setLenient(false); // 严格模式（拒绝无效日期）
                Date date = sdf.parse(value);
                return new SimpleDateFormat("yyyy-MM-dd").format(date); // 统一格式返回
            } catch (ParseException e) {
                continue;
            }
        }
        throw new IllegalArgumentException("值[" + value + "]不是有效的日期（支持格式：yyyy-MM-dd、yyyy/MM/dd等）");
    }

    /**
     * 校验并转换为字符串类型（非空校验可选，这里仅做基础转换）
     */
    private static String validateAndConvertString(String value) {
        // 可根据业务增加字符串校验（如长度限制等）
        return value; // 字符串直接返回
    }

    /**
     * 校验并转换为布尔类型（支持true/false、是/否、1/0等）
     */
    private static String validateAndConvertBoolean(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("布尔类型不能为空");
        }
        // 支持的布尔值格式
        if ("true".equalsIgnoreCase(value) || "是".equals(value) || "1".equals(value)) {
            return "true";
        } else if ("false".equalsIgnoreCase(value) || "否".equals(value) || "0".equals(value)) {
            return "false";
        } else {
            throw new IllegalArgumentException("值[" + value + "]不是有效的布尔值（支持：true/false、是/否、1/0）");
        }
    }
}
