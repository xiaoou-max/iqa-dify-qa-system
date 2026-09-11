package com.frml.api.vo;

import com.alibaba.fastjson.annotation.JSONType;

@JSONType(orders={"type","data","uuid"})
public class APIResponse {
	private String uuid;
	private String type; //success,fail
	private Object data;

	public APIResponse() {
		
	}

	public APIResponse(String type, Object response_data) {
		this.type = type;
		this.data = response_data;
	}
	
	public APIResponse(String type, Object response_data, String uuid) {
		this.type = type;
		this.data = response_data;
		this.uuid = uuid;
	}


	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String request_uuid) {
		this.uuid = request_uuid;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	
	
}
