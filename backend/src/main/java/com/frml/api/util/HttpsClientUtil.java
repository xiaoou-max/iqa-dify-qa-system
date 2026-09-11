package com.frml.api.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.frml.api.vo.APIConstant;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpsClientUtil {

	private static final Logger logger = LoggerFactory.getLogger(HttpsClientUtil.class);

	/**
	 * 发送get请求
	 * 
	 * @param url
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static String httpGet(String url, Map<String, String> headers) throws Exception {
		String result = null;
		SSLClient closeableHttpClient = new SSLClient();
		HttpGet get = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60 * 1000).setConnectTimeout(10000).build();//设置请求和传输超时时间
		get.setConfig(requestConfig);
		if (headers != null && headers.size() > 0) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				get.setHeader(entry.getKey(), entry.getValue());
			}
		}
		CloseableHttpResponse response = null;
		try {
			response = closeableHttpClient.execute(get);
//			logger.info("响应的http状态码：" + response.getStatusLine().getStatusCode());
			result = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			logger.info("调用" + url + "出错!");
			throw new RuntimeException(e);
		} finally {
			// 关闭连接 ,释放资源
			try {
				if (null != response) {
					response.close();
				}
				if (null != closeableHttpClient) {
					closeableHttpClient.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * 获取文件二进制数据（核心方法：仅返回 byte[]，由上层处理保存）
	 * @param url 文件下载链接
	 * @param headers 请求头（可传 null）
	 * @return 文件的二进制字节数组
	 * @throws Exception 网络异常/响应异常
	 */
	public static byte[] getFileBytes(String url, Map<String, String> headers) throws Exception {
		// 校验参数
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("文件URL不能为空");
		}

		CloseableHttpClient httpClient = new SSLClient();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		InputStream inputStream = null;
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

		try {
			// 1. 设置超时配置（保留你的原有逻辑）
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60 * 1000).setConnectTimeout(10000).build();//设置请求和传输超时时间
			httpGet.setConfig(requestConfig);

			// 2. 设置请求头（保留你的原有逻辑）
			if (headers != null && !headers.isEmpty()) {
				headers.forEach(httpGet::setHeader);
			}

			// 3. 执行请求并校验响应状态
			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			logger.info("文件链接响应状态码：{}", statusCode);
			if (statusCode != 200) {
				throw new RuntimeException("文件请求失败，状态码：" + statusCode);
			}

			// 4. 读取二进制流到 ByteArrayOutputStream（核心：返回字节数组）
			inputStream = response.getEntity().getContent();
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				byteOut.write(buffer, 0, bytesRead);
			}
			byteOut.flush();

			// 5. 返回二进制数组
			return byteOut.toByteArray();

		} catch (Exception e) {
			logger.error("获取文件二进制数据失败，URL：{}", url, e);
			throw new RuntimeException("获取文件数据失败：" + e.getMessage(), e);
		} finally {
			// 6. 关闭所有资源（避免泄漏）
			try {
				if (byteOut != null) byteOut.close();
				if (inputStream != null) inputStream.close();
				if (response != null) {
					EntityUtils.consume(response.getEntity());
					response.close();
				}
				if (httpClient != null) httpClient.close();
			} catch (IOException e) {
				logger.error("关闭HttpClient资源失败", e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 发送post请求
	 *
	 * @param url
	 * @param requestJSON
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static String httpPost(String url, String contentType, String requestJSON, Map<String, String> headers) throws Exception {
//		logger.info("requestJSON="+requestJSON);
		String result = null;
		SSLClient closeableHttpClient = new SSLClient();
		HttpPost post = new HttpPost(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60 * 1000).setConnectTimeout(10000).build();//设置请求和传输超时时间
	    post.setConfig(requestConfig);
		// 设置header
		post.setHeader("Content-type", "application/json");
		if (headers != null && headers.size() > 0) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				post.setHeader(entry.getKey(), entry.getValue());
			}
		}
		if (APIConstant.contenttype_form.equalsIgnoreCase(contentType)) {//表单提交
			//处理json为表单形式：k-v、加入消息体
			post.setHeader("Content-type", "application/x-www-form-urlencoded;charset=UTF-8");
			List<BasicNameValuePair> pairList = new ArrayList<BasicNameValuePair>();
			JSONObject jsonObject = JSONObject.parseObject(requestJSON);
			Map<String, String> params = JSONObject.parseObject(jsonObject.toJSONString(), new TypeReference<Map<String, String>>() {
			});
			for (String str : params.keySet()) {
				pairList.add(new BasicNameValuePair(str, params.get(str)));
			}
			post.setEntity(new UrlEncodedFormEntity(pairList, "utf-8"));
		} else { //json提交
			// 设置header
			post.setHeader("Content-type", "application/json");
			StringEntity entity = new StringEntity(requestJSON, "utf-8");
			post.setEntity(entity);
		}
		CloseableHttpResponse response = null;
		try {
			response = closeableHttpClient.execute(post);
			//logger.info("响应的http状态码：" + response.getStatusLine().getStatusCode());
			HttpEntity httpEntity = response.getEntity();
			if(null != httpEntity) {
				result = EntityUtils.toString(httpEntity, "utf-8");
			}
		} catch (Exception e) {
			logger.error("调用" + url + "出错:" + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			// 关闭连接 ,释放资源
			try {
				if (null != response) {
					response.close();
				}
				if (null != closeableHttpClient) {
					closeableHttpClient.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}




	/**
	 * 判断ADI返回的结果是成功还是失败
	 * @param adiResult {String} 调用ADI返回的结果（字符串）
	 * @return true-成功；false-失败
	 */
	public static boolean isSuccess(String adiResult){
		JSONObject adiResultJSONObj = JSON.parseObject(adiResult);

//		logger.info("uuid="+adiResultJSONObj.getString("uuid"));
		return "success".equals(adiResultJSONObj.getString("type"));
	}

	/**
	 * 获取ADI返回的data中的内容
	 * @param adiResult adi响应结果解析成的JSONObject
	 * @return JSONObject
	 */
	public static JSONObject getJSONData(JSONObject adiResult){
		return adiResult.getJSONObject("data");
	}

	/**
	 * 获取ADI返回的data中的内容 String;一般用于失败的时候获取返回的错误信息
	 * @param adiResult adi响应结果解析成的JSONObject
	 * @return JSONObject
	 */
	public static String getStringData(JSONObject adiResult){
		return adiResult.getString("data");
	}
}
