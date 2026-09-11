package com.frml.api.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenModel implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// 用户id
	private String userId;

	// 随机生成的uuid
	private String token;
	// 存储登录之后的用户信息
	private Map<String, Object> session = new HashMap<String, Object>();

	public Map<String, Object> getSession() {
		return session;
	}

	public void setSession(Map<String, Object> session) {
		this.session = session;
	}

	public TokenModel() {
	}
	private static final Logger log = LoggerFactory.getLogger(TokenModel.class);
	// 返回前台的token
	public String getFt() {
		String tt = userId + "_" + token;
		String ft = TokenGenerator.createToken(tt);
		return ft;
	}

	public TokenModel(String userId, String token) {
		this.userId = userId;
		this.token = token;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
