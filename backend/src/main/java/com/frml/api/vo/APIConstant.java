package com.frml.api.vo;

public class APIConstant {
	public static final String api_success = "success";
	
	public static final String api_fail = "fail";

	public static final String api_needLogin = "needLogin";

	public static final String USER_KEY = "userinfo";//Session信息的人员对象信息
	public static final String USER_KEY_ID = "userid";//Session信息的人员对象信息

	public static final String USER_KEY_ACCOUNT = "useraccount";//Session信息的人员对象信息

	public static final String USER_KEY_NAME = "username";//Session信息的人员对象信息

	public static final String USER_KEY_CODE = "usercode";//Session信息的人员对象信息

	public static final String USER_COOKIE_NAME = "_token";//Session信息的人员对象信息

	public static final long TOKEN_EXPIRES_HOUR_APP_DSF =24*7;//登录过期时间,app和dsf登录的过期时间 7天

	public static final long TOKEN_EXPIRES_MINUTE_PC = 120;//登录过期时间,PC端的过期时间 120分钟

	//post请求，以表单方式提交数据
	public static final String contenttype_form = "application/x-www-form-urlencoded";

	//post请求，以json方式提交数据
	public static final String contenttype_json = "application/json";
}
