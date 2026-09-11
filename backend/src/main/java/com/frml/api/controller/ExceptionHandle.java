package com.frml.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.frml.api.exception.BaseException;
import com.frml.api.util.NullUtil;
import com.frml.api.vo.APIConstant;
import com.frml.api.vo.APIHttpStatus;
import com.frml.api.vo.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandle {
	private static final Logger logger = LoggerFactory.getLogger(ExceptionHandle.class); 

	@ExceptionHandler(value = Exception.class)
	@ResponseBody
	public ResponseEntity<Object> Handle(Exception e) {
		if (e instanceof BaseException) { // 用户自定义异常
			// 获取用户抛出的异常信息
			String message = e.getMessage();
			int statusCode = ((BaseException) e).getCode();// 异常响应码
			Throwable cause = e.getCause();
			if (cause != null) {
				String realReason = cause.getMessage();
				message += " 可能的原因是：" + realReason + "";
			}
			// 自定义异常返回实体bean类
			logger.error("BaseException："+message);
			if(!NullUtil.IsAllNotNullOfString(message)) {
				message += "API平台发生异常，请求处理失败，请联系系统管理员";
			}
			logger.error(e.getMessage(),e); //把异常信息打印到日志文件
			APIResponse adiResponse = new APIResponse(APIConstant.api_fail, message+"-002LX");
			logger.info("api响应内容：" + JSONObject.toJSONString(adiResponse));
			return new ResponseEntity<Object>(adiResponse, HttpStatus.valueOf(statusCode));
		} else {
			logger.error("OtherException:");
			logger.error(e.getMessage(), e); // 把异常信息打印到日志文件
			String message = "API平台发生异常，请求处理失败，请联系系统管理员";
			APIResponse adiResponse = new APIResponse(APIConstant.api_fail, message+"。002LX");
			logger.info("api响应内容：" + JSONObject.toJSONString(adiResponse));
			return new ResponseEntity<Object>(adiResponse, HttpStatus.valueOf(APIHttpStatus.INTERNAL_SERVER_ERROR));
		}

	}
}
