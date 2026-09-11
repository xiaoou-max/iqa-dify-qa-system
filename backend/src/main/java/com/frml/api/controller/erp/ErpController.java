package com.frml.api.controller.erp;

import com.alibaba.fastjson.JSONObject;
import com.frml.api.exception.BusinessException;
import com.frml.api.service.erp.ErpService;
import com.frml.api.util.NullUtil;
import com.frml.api.vo.APIConstant;
import com.frml.api.vo.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
@RestController
@RequestMapping("/api/jwgk")
public class ErpController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 定义时间戳格式：年月日时分秒毫秒
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    @Autowired
    private ErpService erpService;

    /**
     * 导入价格数据到数据库
     * @param file
     * @return
     */
    @PostMapping("/importPriceDatas")
    public APIResponse importPriceDatas(@RequestParam("file") MultipartFile file,HttpServletRequest request) {
        if (file.isEmpty()) {
            return new APIResponse(APIConstant.api_fail,"上传失败，请选择文件");
        }
        // 2. 校验文件格式（.xls 或 .xlsx）
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            return new APIResponse(APIConstant.api_fail,"请上传Excel文件（.xls 或 .xlsx格式）！");
        }
        String tablename = request.getParameter("tablename");
        if(!NullUtil.IsAllNotNullOfString(tablename)) {
            throw new BusinessException("请传入参数tablename");
        }
        return erpService.importPriceDatas(file, tablename);
    }

//    /**
//     * 导入管道管件价格数据到数据库
//     * @param file
//     * @return
//     */
//    @PostMapping("/importPriceDatas4gdgj")
//    public APIResponse importPriceDatas4gdgj(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return new APIResponse(APIConstant.api_fail,"上传失败，请选择文件");
//        }
//        // 2. 校验文件格式（.xls 或 .xlsx）
//        String fileName = file.getOriginalFilename();
//        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
//            return new APIResponse(APIConstant.api_fail,"请上传Excel文件（.xls 或 .xlsx格式）！");
//        }
//        return erpService.importPriceDatas(file, ErpConstant.table_name_gdgj);
//    }
//
//    /**
//     * 导入阀门价格数据到数据库
//     * @param file
//     * @return
//     */
//    @PostMapping("/importPriceDatas4fm")
//    public APIResponse importPriceDatas4fm(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return new APIResponse(APIConstant.api_fail,"上传失败，请选择文件");
//        }
//        // 2. 校验文件格式（.xls 或 .xlsx）
//        String fileName = file.getOriginalFilename();
//        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
//            return new APIResponse(APIConstant.api_fail,"请上传Excel文件（.xls 或 .xlsx格式）！");
//        }
//        return erpService.importPriceDatas(file, ErpConstant.table_name_fm);
//    }
//
//    /**
//     * 导入仪表价格数据到数据库
//     * @param file
//     * @return
//     */
//    @PostMapping("/importPriceDatas4yb")
//    public APIResponse importPriceDatas4yb(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return new APIResponse(APIConstant.api_fail,"上传失败，请选择文件");
//        }
//        // 2. 校验文件格式（.xls 或 .xlsx）
//        String fileName = file.getOriginalFilename();
//        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
//            return new APIResponse(APIConstant.api_fail,"请上传Excel文件（.xls 或 .xlsx格式）！");
//        }
//        return erpService.importPriceDatas(file, ErpConstant.table_name_yb);
//    }
//
//    /**
//     * 导入电气控制价格数据到数据库
//     * @param file
//     * @return
//     */
//    @PostMapping("/importPriceDatas4dqkz")
//    public APIResponse importPriceDatas4dqkz(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return new APIResponse(APIConstant.api_fail,"上传失败，请选择文件");
//        }
//        // 2. 校验文件格式（.xls 或 .xlsx）
//        String fileName = file.getOriginalFilename();
//        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
//            return new APIResponse(APIConstant.api_fail,"请上传Excel文件（.xls 或 .xlsx格式）！");
//        }
//        return erpService.importPriceDatas(file, ErpConstant.table_name_dqkz);
//    }

    /**
     * 回填样表数据
     * @param file
     * @return
     */
    @PostMapping("/fillPriceDatas")
    public ResponseEntity<ByteArrayResource> fillPriceDatas(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws UnsupportedEncodingException {
        if (file.isEmpty()) {
            throw new BusinessException("上传失败，请选择文件");
        }
        // 2. 校验文件格式（.xls 或 .xlsx）
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            throw new BusinessException("请上传Excel文件（.xls 或 .xlsx格式）！");
        }

        String tablename = request.getParameter("tablename");
        if(!NullUtil.IsAllNotNullOfString(tablename)) {
            throw new BusinessException("请传入参数tablename");
        }

        //调用Service处理核心逻辑
        byte[] filledExcelBytes = erpService.fillPriceDatas(file, tablename);
        //生成「原始文件名_时间戳.后缀」的下载文件名
        String originalFileName = file.getOriginalFilename();
        // 处理文件名为空的默认情况
        if (originalFileName == null || originalFileName.isEmpty()) {
            originalFileName = "unknown";
        }

        // 拆分原始文件名的「名称部分」和「后缀部分」
        String fileNamePrefix; // 文件名（不含后缀）
        String fileSuffix = ""; // 文件后缀（如.xls/.xlsx）
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            fileNamePrefix = originalFileName.substring(0, lastDotIndex); // 原始文件名（不含后缀）
            fileSuffix = originalFileName.substring(lastDotIndex); // 后缀（含.）
        } else {
            fileNamePrefix = originalFileName; // 无后缀的文件名
        }

        // 生成时间戳（例如：20231015_143025_123）
        String timestamp = TIMESTAMP_FORMAT.format(new Date());

        // 构建最终文件名：原始名称_时间戳.后缀（例：员工信息_20231015_143025_123.xlsx）
        String downloadFileName = "filled_" + fileNamePrefix + "_" + timestamp + fileSuffix;
        logger.info("返回匹配后的文件："+downloadFileName);
        HttpHeaders headers = new HttpHeaders();
        // 关键：使用 URLEncoder 对文件名进行 UTF-8 编码（处理中文）
        String encodedFileName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20"); // 替换空格为 %20（URL 编码规范）

//        logger.info("文件名编码后：" + encodedFileName);
        // 构建响应头：使用 RFC 5987 标准格式（filename*=UTF-8''编码值）
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=utf-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filledExcelBytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new ByteArrayResource(filledExcelBytes));
    }

    /**
     * 获取所有的料品分类，供前端下拉选择使用
     * @param request
     * @return
     */
    @GetMapping("/getLpfls")
    public APIResponse getLpfls(HttpServletRequest request) {
        String tablename = request.getParameter("tablename");
        if(!NullUtil.IsAllNotNullOfString(tablename)) {
            throw new BusinessException("请传入参数tablename");
        }
        return erpService.getLpfls(tablename);
    }

    /**
     * 根据前端输入的查询条件，查询价格数据，查最近的5条数据即可
     * @param params
     * @param request
     * @return
     */
    @PostMapping("/queryPriceDatas")
    public APIResponse queryPriceDatas(@RequestBody JSONObject params, HttpServletRequest request) {
        if(!NullUtil.IsAllNotNullOfObject(params)) {
            throw new BusinessException("没有收到任何的查询条件");
        }
        if(!params.containsKey("lpfl") || !NullUtil.IsAllNotNullOfString(params.getString("lpfl"))) {
            throw new BusinessException("请选择要查询的料品分类");
        }
        String tablename = request.getParameter("tablename");
        if(!NullUtil.IsAllNotNullOfString(tablename)) {
            throw new BusinessException("请传入参数tablename");
        }
        return erpService.queryPriceDatas(params,tablename);
    }
}
