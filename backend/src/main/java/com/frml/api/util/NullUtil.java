package com.frml.api.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

/**
 * 判断对象、字符串等是否为空的工具类
 * @author Administrator
 *
 */
public class NullUtil {
	
	/**
	 * 判断传入的对象是否全不为null
	 * @param objs
	 * @return
	 */
	public static boolean IsAllNotNullOfObject(Object... objs) {
		if(null == objs) {
			return false;
		}
		for (Object obj : objs) {
			if (null == obj) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 判断传入的字符串是否全不为null或空字符串
	 * @param strs
	 * @return
	 */
	public static boolean IsAllNotNullOfString(String... strs) {
		if(null == strs) {
			return false;
		}
		for (Object str : strs) {
			if (null == str
					|| "".equals(str.toString().trim().replaceAll("\t", "")
							.replaceAll("\r\n", ""))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断json对象中某个key的值是否存在：普通key不能为null，对象类型不能为{}，数组类型不能为[]，否则表示值不存在
	 * @param key，不能是多层
	 * @param jsonObj
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean hasValueOfJsonKey(String key, JSONObject jsonObj) {
		Object jsonStrObj = jsonObj.get(key);
		if (null == jsonStrObj || !NullUtil.IsAllNotNullOfString(jsonStrObj.toString())) {
			return false;
		}
		String jsonStr = jsonStrObj.toString();
		try {
			Object json = new JSONTokener(jsonStr).nextValue();
			if (json instanceof org.json.JSONObject) { //对象类型
				Map<String, Object> params = (Map) JSONObject.parse(jsonStr);
				if(params.isEmpty()) {
					return false;
				}
				return true;
			} else if (json instanceof org.json.JSONArray) { //数组类型
				JSONArray jsonArray = JSONArray.parseArray(jsonStr);
				if(jsonArray.size() == 0) {
					return false;
				}
				JSONObject obj = (JSONObject) jsonArray.get(0); //[{}]
				if(obj.isEmpty()) {
					return false;
				}
				return true;
			} else {
				return true;
			}
		} catch (Exception e) {
			return true;
		}

	}
}
