package com.frml.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapUtil {
	
	private final static Logger logger = LoggerFactory.getLogger(MapUtil.class);
	/**
	 * 将map转为对key大小写不明感的map
	 * @param map
	 * @return
	 */
	@SuppressWarnings({ "unchecked"})
	public static Map<String, Object> map2CaseInsensitiveMap(Map<String, Object> map) {
		if(null == map) {
			return null;
		}
		if(map instanceof CaseInsensitiveMap) {
			return map;
		}
		Map<String, Object> result = new CaseInsensitiveMap();
		for(Entry<String, Object> entry : map.entrySet()){
			result.put(entry.getKey(), entry.getValue());
		}   
		return result;
	}
	
	public static List<Map<String, Object>> map2CaseInsensitiveMap(List<Map<String, Object>> maps) {
		List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>(); 
		for(Map<String, Object> map : maps) {
			map = MapUtil.map2CaseInsensitiveMap(map);
			newMaps.add(map);
		}
		return newMaps;
	}
	
	/** 
	 * 将map转换成查询字符串，并对参数编码
	 * @param map 
	 * @return 
	 * @throws EncoderException 
	 */  
	public static String getUrlParamsByMap(Map<String, Object> map) throws EncoderException {  
	    if (map == null) {  
	        return "";  
	    }  
	    StringBuffer sb = new StringBuffer();  
	    org.apache.commons.codec.net.URLCodec codec = new org.apache.commons.codec.net.URLCodec();
	    String paramName = "";
	    String paramValue = "";
	    for (Entry<String, Object> entry : map.entrySet()) {
			paramName = entry.getKey();
	    	//20230319 如果值为null，则使用空字符串 start
//	    	if(null == entry.getValue()) {
//	    		continue;
//	    	}
			if(null == entry.getValue()) {
				paramValue = "";
			} else {
				paramValue = entry.getValue().toString();
			}
			//20230319 如果值为null，则使用空字符串 end


	    	logger.info(paramName+"参数编码前："+paramValue);
	    	paramValue = codec.encode(paramValue);
	    	logger.info(paramName+"参数编码后："+paramValue);
	        sb.append(paramName + "=" + paramValue);  
	        sb.append("&");  
	    }  
	    String s = sb.toString();  
	    if (s.endsWith("&")) {  
	        s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "&");
	    }  
	    return s;  
	}

	public static String getUrlParamsByStringMap(Map<String, String> map) throws EncoderException {
		if (map == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		org.apache.commons.codec.net.URLCodec codec = new org.apache.commons.codec.net.URLCodec();
		String paramName = "";
		String paramValue = "";
		for (Entry<String, String> entry : map.entrySet()) {
			if(null == entry.getValue()) {
				continue;
			}
			paramName = entry.getKey();
			paramValue = entry.getValue();
			logger.info(paramName+"参数编码前："+paramValue);
			paramValue = codec.encode(paramValue);
			logger.info(paramName+"参数编码后："+paramValue);
			sb.append(paramName + "=" + paramValue);
			sb.append("&");
		}
		String s = sb.toString();
		if (s.endsWith("&")) {
			s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "&");
		}
		return s;
	}
	
	
	/** 
	 * 将map转换成查询字符串,不对中文编码，用于adi日志 
	 * @param map 
	 * @return 
	 * @throws EncoderException 
	 */  
	public static String getUrlParamsByMapWithoutEncode(Map<String, Object> map) throws EncoderException {  
	    if (map == null) {  
	        return "";  
	    }  
	    StringBuffer sb = new StringBuffer();  
	    String paramName = "";
	    String paramValue = "";
	    for (Entry<String, Object> entry : map.entrySet()) {
			paramName = entry.getKey();
			//20230319 如果值为null，则使用空字符串 start
//	    	if(null == entry.getValue()) {
//	    		continue;
//	    	}
			if(null == entry.getValue()) {
				paramValue = "";
			} else {
				paramValue = entry.getValue().toString();
			}
			//20230319 如果值为null，则使用空字符串 end

//	    	paramName = entry.getKey();
//	    	paramValue = entry.getValue().toString();
	        sb.append(paramName + "=" + paramValue);  
	        sb.append("&");  
	    }  
	    String s = sb.toString();  
	    if (s.endsWith("&")) {  
	        s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "&");
	    }  
	    return s;  
	}
	
	/**
	 * SQL语句查询单个结果属性名大写转小写
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map changeToLowerMap(Map map) {
		Map mapreturn = new HashMap();
		Set set = map.entrySet();
		Iterator iterator = set.iterator();
		while (iterator.hasNext()) {
			Entry mapentry = (Entry) iterator.next();
			mapreturn.put(mapentry.getKey().toString().toLowerCase(), mapentry.getValue());
		}
		return mapreturn;
	}
	
	/**
	 * 把多层嵌套的map弄平整，使之成为一层
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> flatMap(Map<String, Object> datamap,List<String> rootkeys) {
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> testmap = new HashMap<>();// 取空map填充格式
		result.add(testmap);
		Set<String> keys = datamap.keySet();
		for (String rootKey : rootkeys) {
			if(keys.contains(rootKey)) {
				if (datamap.get(rootKey) instanceof Map) {
					List<Map<String, Object>> list = new ArrayList<>();
					list.add((Map<String, Object>) datamap.get(rootKey));
					result = insertParam(rootKey, list, result);
				} else if (datamap.get(rootKey) instanceof List) {
					result = insertParam(rootKey, (List<Map<String, Object>>) datamap.get(rootKey), result);
				} else {
					for (Map<String, Object> map : result) {
						map.put(rootKey, datamap.get(rootKey));
					}
				}
			} else {
				//可以抛异常 TODO
			}
		}
		return result;
	}
	
	/**
	 * 执行插入的时候把嵌套参数结构转换为单层笛卡尔结构的迭代方法
	 * 
	 * @param rootKey
	 * @param newParamList
	 * @param sqlParamsList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> insertParam(String rootKey, List<Map<String, Object>> newParamList,
			List<Map<String, Object>> sqlParamsList) {
		if(rootKey.startsWith("aiyo_")) {
			rootKey = "";
		} 
		// 如果待处理参数为空，就跳过处理
		if (newParamList.size() == 0)
			return sqlParamsList;

		// 定义一个保存返回值的列表，列表条目数应为已处理生成的列表条目数乘以待处理参数列表的条目数
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		// 遍历待处理参数列表
		for (Map<String, Object> newParamMap : newParamList) {
			if(newParamMap.isEmpty()) {
				continue;
			}
			// 复制已处理生成的列表
			List<Map<String, Object>> newList = new ArrayList<Map<String, Object>>();
			for (Map<String, Object> m : sqlParamsList) {
				Map<String, Object> t = new HashMap<>();
				t.putAll(m);
				newList.add(t);
			}

			// 遍历待处理参数的每一个Map的每一项，如果此项仍然是List，迭代调用此方法替换新的列表，如果不是，那么将值加入新的列表
			for (String key : newParamMap.keySet()) {
				String newKey = "."+key;
				if(key.startsWith("aiyo_")) {
					newKey = "";
				}
				if (newParamMap.get(key) instanceof List) {
					newList = insertParam(rootKey + newKey, (List<Map<String, Object>>) newParamMap.get(key),
							newList);
				} else if (newParamMap.get(key) instanceof Map) {
					List<Map<String, Object>> list = new ArrayList<>();
					list.add((Map<String, Object>) newParamMap.get(key));
					newList = insertParam(rootKey + newKey, list, newList);
				} else {
					for (Map<String, Object> map : newList) {
						map.put(rootKey + newKey, newParamMap.get(key));
					}
				}
			}
			resultList.addAll(newList);
		}
		return resultList;
	}
}
