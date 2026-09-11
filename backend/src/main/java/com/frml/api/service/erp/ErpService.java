package com.frml.api.service.erp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.frml.api.exception.BusinessException;
import com.frml.api.util.ExcelUtil;
import com.frml.api.util.HttpsClientUtil;
import com.frml.api.util.NullUtil;
import com.frml.api.vo.APIConstant;
import com.frml.api.vo.APIResponse;
import com.frml.api.vo.erp.ErpConstant;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class ErpService {

    @Value("${api.root.url:your_api_root_url}")
    private String apiUrl;

    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor executor;



    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<LinkedHashMap<String, Object>> excelToPriceDatas(MultipartFile file,List<String> columns_table) {
        List<LinkedHashMap<String, Object>> priceDatas = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) { // 自动适配格式

            // 遍历所有工作表（此处仅处理第一个工作表）
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel中无工作表数据！");
            }

            // 获取总行数（物理行数，含空行）
            int rowCount = sheet.getPhysicalNumberOfRows();
            if (rowCount < 2) { // 至少需要1行表头 + 1行数据
                throw new BusinessException("Excel数据不足（需包含表头和至少1行数据）！");
            }

            List<String> headerList = new ArrayList<>(); // 存储表头（可选）
            // 1. 先读取表头（第一行，索引0）
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    headerList.add(ExcelUtil.getCellValue(cell,null));
                }
            }

            if(columns_table.size() != headerList.size()) {
                throw new BusinessException("Excel的列数["+headerList.size()+"]与数据库字段数["+columns_table.size()+"]不匹配！");
            }

            // 2. 读取数据行（从第二行开始，索引≥1）
            for (int rowNum = 1; rowNum < rowCount; rowNum++) { // 关键：rowNum从1开始
                Row dataRow = sheet.getRow(rowNum);
                if (dataRow == null) {
//                    continue; // 跳过空行
                    throw new BusinessException("第"+rowNum+"行是空行，请先核实Excel数据的完整性！");
                }

                LinkedHashMap<String,Object> priceData = new LinkedHashMap();
                // 按表头列数读取（避免数据行多列或少列）
                for (int cellNum = 0; cellNum < columns_table.size(); cellNum++) {
                    String columnName = columns_table.get(cellNum);
                    Cell cell = dataRow.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String targetType = null;
                    if ("wsdj".equals(columnName) || "hsdj".equals(columnName)) {
                        targetType = "NUMERIC";
                    } else if ("ywrq".equals(columnName)) {
                        targetType = "DATE";
                    }
                    // 其他字段不校验（targetType为null）
                    String cellValue = "";
                    try {
                        // 传入targetType进行校验
                        cellValue = ExcelUtil.getCellValue(cell, targetType);

                    } catch (IllegalArgumentException e) {
                        throw new BusinessException("第" + (rowNum + 1) + "行，字段[" + headerList.get(cellNum) + "]校验失败：" + e.getMessage());
                    }
                    if("lpfl".equals(columnName)) {
                        if(!NullUtil.IsAllNotNullOfString(cellValue)) {
                            throw new BusinessException("第"+(rowNum + 1)+"行，料品分类不能为空");
                        }
                    }
                    if("mc".equals(columnName)) {
                        if(!NullUtil.IsAllNotNullOfString(cellValue)) {
                            throw new BusinessException("第"+(rowNum + 1)+"行，名称不能为空");
                        }
                    }
                    priceData.put(columnName,cellValue);
                }
                priceDatas.add(priceData);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e); //把异常信息打印到日志文件
            throw new BusinessException("解析Excel失败：" + e.getMessage());
        }
        return priceDatas;
    }

    /**
     * 导入采购料品价格信息到数据库
     * @param file
     * @return
     */
    public APIResponse importPriceDatas(MultipartFile file, String tablename) {
        List<LinkedHashMap<String, Object>> priceDatas = new ArrayList<>();
        if(ErpConstant.table_name_summary.equals(tablename)) {
            priceDatas = this.excelToPriceDatas(file,ErpConstant.columns_summary);
        } else if(ErpConstant.table_name_gdgj.equals(tablename)) {
            priceDatas = this.excelToPriceDatas(file,ErpConstant.columns_gdgj);
        } else if(ErpConstant.table_name_fm.equals(tablename)) {
            priceDatas = this.excelToPriceDatas(file,ErpConstant.columns_fm);
        } else if(ErpConstant.table_name_yb.equals(tablename)) {
            priceDatas = this.excelToPriceDatas(file,ErpConstant.columns_yb);
        } else if(ErpConstant.table_name_dqkz.equals(tablename)) {
            priceDatas = this.excelToPriceDatas(file,ErpConstant.columns_dqkz);
        } else {
            throw new BusinessException("导入失败，无法识别的表类型");
        }
        Map<String,List<LinkedHashMap<String, Object>>> postData = new HashMap<>();
        postData.put(tablename,priceDatas); //cglpjgxx:采购料品价格信息
        String jsonData = JSONObject.toJSONString(postData);
        String api_url = apiUrl.endsWith("/") ? apiUrl+"bitapi/importPriceDatas" : apiUrl+"/bitapi/importPriceDatas";
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpPost(api_url, APIConstant.contenttype_json, jsonData, null);
        } catch (Exception e) {
            throw new BusinessException("导入失败，"+e.getMessage());
        }
        logger.info("importPriceDatas响应："+adiResult);
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            return new APIResponse(APIConstant.api_success, "导入成功，共导入"+priceDatas.size()+"条数据");
        } else {
            return new APIResponse(APIConstant.api_fail, HttpsClientUtil.getStringData(adiResultJSONObj));
        }
    }

    private List<LinkedHashMap<String, Object>> makeQueryParams4Fill(Sheet sheet, List<String> columns_table) {

        List<LinkedHashMap<String, Object>> queryParams = new ArrayList<>();

        // 获取总行数（物理行数，含空行）
        int rowCount = sheet.getPhysicalNumberOfRows();
        if (rowCount < 2) { // 至少需要1行表头 + 1行数据
            throw new BusinessException("Excel数据不足（需包含表头和至少1行数据）！");
        }

        List<String> headerList = new ArrayList<>(); // 存储表头（可选）
        // 1. 先读取表头（第一行，索引0）
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                headerList.add(ExcelUtil.getCellValue(cell,null));
            }
        }

        if(columns_table.size() != headerList.size()) {
            throw new BusinessException("Excel的列数["+headerList.size()+"]与数据库字段数["+columns_table.size()+"]不匹配！");
        }

        // 2. 读取数据行（从第二行开始，索引≥1）
        for (int rowNum = 1; rowNum < rowCount; rowNum++) { // 关键：rowNum从1开始
            Row dataRow = sheet.getRow(rowNum);
            if (dataRow == null) {
//                    continue; // 跳过空行
                throw new BusinessException("第"+rowNum+"行是空行，请先核实Excel数据的完整性！");
            }
            LinkedHashMap<String,Object> priceData = new LinkedHashMap();
            // 按表头列数读取（避免数据行多列或少列）
            boolean blankRowFlag = false;
            for (int cellNum = 0; cellNum < columns_table.size(); cellNum++) { //只需要读取“数据2”字段之前的列
                String columnName = columns_table.get(cellNum);
                if("wsdj".equals(columnName)) {
                    break;
                }
                Cell cell = dataRow.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String targetType = null;
                if ("wsdj".equals(columnName) || "hsdj".equals(columnName)) {
                    targetType = "NUMERIC";
                } else if ("ywrq".equals(columnName)) {
                    targetType = "DATE";
                }
                // 其他字段不校验（targetType为null）
                String cellValue = ExcelUtil.getCellValue(cell, targetType);
                if("lpfl".equals(columnName)) {
                    if(!NullUtil.IsAllNotNullOfString(cellValue)) {
//                        throw new BusinessException("第"+(rowNum + 1)+"行，料品分类不能为空");
                        blankRowFlag = true;
                        break;
                    }
                }
                if("mc".equals(columnName)) {
                    if(!NullUtil.IsAllNotNullOfString(cellValue)) {
                        throw new BusinessException("第"+(rowNum + 1)+"行，名称不能为空");
                    }
                }
                if("ggxh".equals(columnName)) {
                    if(!NullUtil.IsAllNotNullOfString(cellValue)) {
                        throw new BusinessException("第"+(rowNum + 1)+"行，规格型号不能为空");
                    }
                }
                if(NullUtil.IsAllNotNullOfString(cellValue)) { //不为空的字段值才能加入
                    priceData.put(columnName,cellValue);
                }
            }
            if(blankRowFlag) { //遇到空行了
                break;
            }
            queryParams.add(priceData);
        }
        return queryParams;
    }

    private void threadDeal(List<LinkedHashMap<String, Object>> current_excelParams, String tablename) {
        int subtaskcount = current_excelParams.size();
        CountDownLatch countDownLatch = new CountDownLatch(subtaskcount);
        for(LinkedHashMap<String, Object> excelParam : current_excelParams) {
            SubDealThread subDealThread = new SubDealThread(countDownLatch, excelParam, tablename);
            executor.execute(subDealThread);
        }
        try {
            countDownLatch.await(); //保证之前的所有的线程都执行完成，才会走下面的；
            // 这样就可以在下面拿到所有线程执行完的集合结果
        } catch (Exception e) {
            logger.error("阻塞异常");
        }
        try {
            Thread.sleep(1000); //单位毫秒
        } catch (Exception e) {
            logger.error("睡眠失败");
        }
    }

    public void dealForExcelParam(LinkedHashMap<String, Object> excelParam, String tablename) {
        String lpfl = excelParam.get("lpfl").toString(); //料品分类
        String mc = excelParam.get("mc").toString(); //名称
        String ggxh = excelParam.get("ggxh").toString(); //规格型号
        //对特殊符号进行转义，注意不同数据库的处理方式不同，这里是MySQL的处理方式
        String escapedMc = mc.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
        String escapedGgxh = ggxh.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
        String accurate_sql = "";
        String fuzzy_sql = "";
        String dynamic_sql = "(";
        if(ErpConstant.table_name_summary.equals(tablename)) { //总表依然使用以前的匹配逻辑
            //1. 第一次匹配用三个字段精确匹配；
            accurate_sql = " and lpfl='"+lpfl+"' and mc='"+mc+"' and ggxh='"+ggxh+"' "; //构建精准匹配sql
//        2. 如果没有匹配到，那么第二次匹配用名称和规格型号模糊匹配(或)，并且加一个字段用来标记是精确匹配还是模糊匹配
            fuzzy_sql = " and lpfl = '"+lpfl+"' and (mc like '%"+escapedMc+"%' or ggxh like '%"+escapedGgxh+"%') ";
//        3. 如果还是没有匹配到，就用其他字段"或"匹配（数据2之前的字段，模糊按顺序），并且加一个字段用来标记是精确匹配还是模糊匹配。
            Set<Map.Entry<String, Object>> entrySet = excelParam.entrySet();
            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();    // 获取key
                if("lpfl".equals(key) || "mc".equals(key) || "ggxh".equals(key)) {
                    continue;
                }
                String value = entry.getValue().toString(); // 获取value
                //对特殊符号进行转义，注意不同数据库的处理方式不同，这里是MySQL的处理方式
                value = value.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
                if(i == 0) {
                    dynamic_sql = dynamic_sql + (key +" like '%"+value+"%' ");
                } else {
                    dynamic_sql = dynamic_sql + ("or " + key +" like '%"+value+"%' " );
                }
                i++;
            }
            dynamic_sql = dynamic_sql + ")";
            if(dynamic_sql.length() == 2) { //说明没有其它字段有值
                dynamic_sql = "no_query"; //注意在adi中判断，当动态sql的值是no_query时不要执行sql语句
            } else {
                dynamic_sql = " and lpfl = '"+lpfl+"' and " + dynamic_sql;
            }
        } else {
            //20251218 新加的4张表使用3+1匹配模式
            //1、精准匹配：3+N字段，3（料品分类、名称、规格型号）N(剩余字段）；2、模糊匹配：3+1字段，3（料品分类、名称、规格型号）1(剩余字段中的一个）
            accurate_sql = " and lpfl='"+lpfl+"' and mc='"+mc+"' and ggxh='"+ggxh+"' ";
            fuzzy_sql = accurate_sql;
            String fuzzy_sql_temp = "(";
            dynamic_sql = "no_query";
            Set<Map.Entry<String, Object>> entrySet = excelParam.entrySet();
            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();    // 获取key
                if("lpfl".equals(key) || "mc".equals(key) || "ggxh".equals(key)) {
                    continue;
                }
                String value = entry.getValue().toString(); // 获取value
                //对特殊符号进行转义，注意不同数据库的处理方式不同，这里是MySQL的处理方式
                value = value.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
                accurate_sql = accurate_sql + " and "+key+"='"+value+"' ";
                if(i == 0) {
                    fuzzy_sql_temp = fuzzy_sql_temp + (key +" = '"+value+"' ");
                } else {
                    fuzzy_sql_temp = fuzzy_sql_temp + ("or " + key +" = '"+value+"' " );
                }
                i++;
            }
            fuzzy_sql_temp = fuzzy_sql_temp + ")";
            if(fuzzy_sql_temp.length() == 2) { //说明没有其它字段有值
                accurate_sql = accurate_sql + " and 1=2"; //只填了三个字段，视为模糊匹配
            } else {
                fuzzy_sql = fuzzy_sql + " and " + fuzzy_sql_temp;
            }
        }

        //准备调用ADI
        Map<String,String> postData = new HashMap<>();
        postData.put("log_track",lpfl+"_"+mc+"_"+ggxh);
        postData.put("tablename",tablename);
        postData.put("accurate_sql",accurate_sql);
        postData.put("fuzzy_sql",fuzzy_sql);
        postData.put("dynamic_sql",dynamic_sql);

        String jsonData = JSONObject.toJSONString(postData);
        String api_url = apiUrl.endsWith("/") ? apiUrl+"bitapi/fillPriceDatas" : apiUrl+"/bitapi/fillPriceDatas";
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpPost(api_url, APIConstant.contenttype_json, jsonData, null);
        } catch (Exception e) {
            throw new BusinessException("查询失败，"+e.getMessage());
        }
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            JSONObject adiData = HttpsClientUtil.getJSONData(adiResultJSONObj);
            if(NullUtil.hasValueOfJsonKey("accurate_result",adiData)) { //有精确匹配结果
                excelParam.put("_match_flag","1");
                excelParam.put("_accurate_flag","1");
                excelParam.putAll(adiData.getJSONObject("accurate_result").toJavaObject(Map.class));
            } else if(NullUtil.hasValueOfJsonKey("fuzzy_result",adiData)) { //有模糊匹配结果
                excelParam.put("_match_flag","1");
                excelParam.putAll(adiData.getJSONObject("fuzzy_result").toJavaObject(Map.class));
            } else if(NullUtil.hasValueOfJsonKey("dynamic_result",adiData)) { //有动态sql模糊匹配结果
                excelParam.put("_match_flag","1");
                excelParam.putAll(adiData.getJSONObject("dynamic_result").toJavaObject(Map.class));
            }
        } else {
            excelParam.put("_fail_msg",HttpsClientUtil.getStringData(adiResultJSONObj));
            throw new BusinessException(HttpsClientUtil.getStringData(adiResultJSONObj));
        }

//        4. 如果最终匹配到了，我需要用匹配结果覆盖数据2之后的字段。
    }

    /**
     * 追加"匹配结果"表头，并复用原最后一列的样式
     * @return 新列的索引
     */
    private int appendMatchResultHeader(Workbook workbook, Sheet sheet, Row headerRow) {
        // 获取原最后一列的索引
        int lastColumnIndex = headerRow.getLastCellNum() - 1;
        if (lastColumnIndex < 0) {
            // 表头无列时，默认从0开始
            lastColumnIndex = 0;
        }

        // 复制原最后一列的样式
        Cell lastCell = headerRow.getCell(lastColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        CellStyle newColumnStyle = workbook.createCellStyle();
        newColumnStyle.cloneStyleFrom(lastCell.getCellStyle()); // 复用样式

        // 追加新列到表头最后
        int newColumnIndex = headerRow.getLastCellNum(); // 新列索引 = 原最后一列索引 + 1
        Cell newHeaderCell = headerRow.createCell(newColumnIndex);
        newHeaderCell.setCellValue("匹配结果"); // 新表头值
        newHeaderCell.setCellStyle(newColumnStyle); // 应用复用的样式

        // 调整新列的列宽（与原最后一列保持一致）
        int lastColumnWidth = sheet.getColumnWidth(lastColumnIndex);
        sheet.setColumnWidth(newColumnIndex, lastColumnWidth);

        return newColumnIndex;
    }

    public byte[] fillPriceDatas(MultipartFile file, String tablename) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) { // 自动适配格式
            // 遍历所有工作表（此处仅处理第一个工作表）
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel中无工作表数据！");
            }
            List<String> columns = new ArrayList<>();
            if(ErpConstant.table_name_summary.equals(tablename)) {
                columns = new ArrayList<>(ErpConstant.columns_summary);
            } else if(ErpConstant.table_name_gdgj.equals(tablename)) {
                columns = new ArrayList<>(ErpConstant.columns_gdgj);
                int pp_index = columns.indexOf("pp");
                columns.set(pp_index,"pp_temp");
                int wsdj_index = columns.indexOf("wsdj");
                columns.add(wsdj_index,"pp"); //插入要作为匹配条件的品牌字段
            } else if(ErpConstant.table_name_fm.equals(tablename)) {
                columns = new ArrayList<>(ErpConstant.columns_fm);
                int pp_index = columns.indexOf("pp");
                columns.set(pp_index,"pp_temp");
                int wsdj_index = columns.indexOf("wsdj");
                columns.add(wsdj_index,"pp"); //插入要作为匹配条件的品牌字段
            } else if(ErpConstant.table_name_yb.equals(tablename)) {
                columns = new ArrayList<>(ErpConstant.columns_yb);
                int pp_index = columns.indexOf("pp");
                columns.set(pp_index,"pp_temp");
                int wsdj_index = columns.indexOf("wsdj");
                columns.add(wsdj_index,"pp"); //插入要作为匹配条件的品牌字段
            } else if(ErpConstant.table_name_dqkz.equals(tablename)) {
                columns = new ArrayList<>(ErpConstant.columns_dqkz);
                int pp_index = columns.indexOf("pp");
                columns.set(pp_index,"pp_temp");
                int wsdj_index = columns.indexOf("wsdj");
                columns.add(wsdj_index,"pp"); //插入要作为匹配条件的品牌字段
            } else {
                throw new BusinessException("导入失败，无法识别的表类型");
            }
            List<LinkedHashMap<String, Object>> excelParams = this.makeQueryParams4Fill(sheet,columns);
            int i = 1;
            List<LinkedHashMap<String, Object>> deal_excelParams = new ArrayList<LinkedHashMap<String, Object>>(excelParams);
            while(deal_excelParams.size() > 10) { //每次开启10个线程处理10条记录
                logger.info("开始第"+i+"次批处理");
                List<LinkedHashMap<String, Object>> current_excelParams = deal_excelParams.subList(0,10);
                threadDeal(current_excelParams,tablename);
                for(LinkedHashMap<String, Object> current_excelParam : current_excelParams) {
                    if(current_excelParam.containsKey("_fail_msg")) {
                        throw new BusinessException(current_excelParam.get("_fail_msg").toString());
                    }
                }
                deal_excelParams = excelParams.subList(i*10,excelParams.size());
                i++;
            }
            if(deal_excelParams.size() > 0) {
                logger.info("开始第"+i+"次批处理");
                threadDeal(deal_excelParams,tablename);
                for(LinkedHashMap<String, Object> deal_excelParam : deal_excelParams) {
                    if(deal_excelParam.containsKey("_fail_msg")) {
                        throw new BusinessException(deal_excelParam.get("_fail_msg").toString());
                    }
                }
            }
            // 2. 处理表头：追加"匹配结果"列（复用原最后一列样式）
            Row headerRow = sheet.getRow(0);
            int newColumnIndex = appendMatchResultHeader(workbook, sheet, headerRow);
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            //回填数据
            for(int j = 0; j<excelParams.size(); j++) {
                Row row = sheet.getRow(j+1); //第二行开始才是数据，注意加1
                Map<String, Object> excelParam = excelParams.get(j);
                if("1".equals(excelParam.get("_match_flag"))) { //该条记录匹配到了数据
                    //填充匹配结果
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("wsdj"))) { //未税单价
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("wsdj"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(((Number)excelParam.get("wsdj")).doubleValue());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("hsdj"))) { //含税单价
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("hsdj"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(((Number)excelParam.get("hsdj")).doubleValue());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("zxsl"))) { //执行税率
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("zxsl"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(((Number)excelParam.get("zxsl")).doubleValue());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("ywrq"))) { //业务日期
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("ywrq"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(excelParam.get("ywrq").toString());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("pp_temp"))) { //品牌
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("pp_temp"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(excelParam.get("pp_temp").toString());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("gys"))) { //供应商
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("gys"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(excelParam.get("gys").toString());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("lh"))) { //料号
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("lh"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(excelParam.get("lh").toString());
                    }
                    if(NullUtil.IsAllNotNullOfObject(excelParam.get("pm"))) { //品名
                        // 创建或获取需要回填的单元格
                        Cell fillCell = row.getCell(columns.indexOf("pm"), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        // 设置回填值（确保索引不越界）
                        fillCell.setCellValue(excelParam.get("pm").toString());
                    }
                    Cell fillCell = row.getCell(newColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    fillCell.setCellStyle(borderStyle);
                    if("1".equals(excelParam.get("_accurate_flag"))) { //精确匹配
                        fillCell.setCellValue("精确");
                    } else { //模糊匹配
                        fillCell.setCellValue("模糊");
                    }
                } else { //没有匹配到数据
                    Cell fillCell = row.getCell(newColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    fillCell.setCellStyle(borderStyle);
                    fillCell.setCellValue("无匹配");
                }
            }
            //新增列的筛选功能
            sheet.setAutoFilter(new CellRangeAddress(0, excelParams.size(), 0, columns.size()+1));
            // 6. 生成处理后的Excel字节流
            return generateExcelBytes(workbook);
        } catch (IOException e) {
            logger.error(e.getMessage(),e); //把异常信息打印到日志文件
            throw new BusinessException("样本匹配失败：" + e.getMessage());
        }
    }

    /**
     * 将Workbook转换为字节数组（用于响应下载）
     */
    private byte[] generateExcelBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public APIResponse queryPriceDatas(JSONObject jsonObject,String tablename) {
        Map<String, Object> params = JSON.parseObject(
                jsonObject.toJSONString(),
                new TypeReference<LinkedHashMap<String, Object>>() {},
                Feature.OrderedField // 保持顺序
        );
        String lpfl = params.get("lpfl").toString().trim();
        String dynamic_sql = " and lpfl='"+lpfl+"' ";
        Set<Map.Entry<String, Object>> entrySet = params.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            String key = entry.getKey();    // 获取key
            if("lpfl".equals(key)) {
                continue;
            }
            if(!NullUtil.IsAllNotNullOfObject(entry.getValue()) || !NullUtil.IsAllNotNullOfString(entry.getValue().toString().trim())) {
                continue;
            }
            String value = entry.getValue().toString().trim(); // 获取value
            //对特殊符号进行转义，注意不同数据库的处理方式不同，这里是MySQL的处理方式
            value = value.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
            dynamic_sql = dynamic_sql + ("and " + key +" like '%"+value+"%' " );
        }


        //准备调用ADI
        Map<String,String> postData = new HashMap<>();
        postData.put("log_track",lpfl);
        postData.put("tablename",tablename);
        postData.put("dynamic_sql",dynamic_sql);

        String jsonData = JSONObject.toJSONString(postData);
        String api_url = apiUrl.endsWith("/") ? apiUrl+"bitapi/queryPriceDatas" : apiUrl+"/bitapi/queryPriceDatas";
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpPost(api_url, APIConstant.contenttype_json, jsonData, null);
        } catch (Exception e) {
            throw new BusinessException("查询失败，"+e.getMessage());
        }
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            JSONObject adiData = HttpsClientUtil.getJSONData(adiResultJSONObj);
            return new APIResponse(APIConstant.api_success,adiData);
        } else {
            return new APIResponse(APIConstant.api_fail,HttpsClientUtil.getStringData(adiResultJSONObj));
        }
    }

    public APIResponse getLpfls(String tablename) {
        String api_url = apiUrl.endsWith("/") ? apiUrl+"bitapi/getLpfls" : apiUrl+"/bitapi/getLpfls";
        api_url = api_url + "?tablename="+tablename;
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpGet(api_url,null);
        } catch (Exception e) {
            throw new BusinessException("获取料品分类列表失败，"+e.getMessage());
        }
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            JSONObject adiData = HttpsClientUtil.getJSONData(adiResultJSONObj);
            JSONArray lpfl_array = adiData.getJSONArray("result");
            int lpfl_count = lpfl_array.size();
            List<String> lpfls = new ArrayList<>();
            for(int i = 0; i < lpfl_count; i++) {
                JSONObject obj = lpfl_array.getJSONObject(i);
                lpfls.add(obj.getString("lpfl"));
            }
            return new APIResponse(APIConstant.api_success,lpfls);
        } else {
            return new APIResponse(APIConstant.api_fail,HttpsClientUtil.getStringData(adiResultJSONObj));
        }
    }
}
