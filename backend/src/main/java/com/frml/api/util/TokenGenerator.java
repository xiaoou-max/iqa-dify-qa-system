package com.frml.api.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * token生成器
 *
 * @author wfq
 *
 */
public class TokenGenerator {

	/**
	 *
	 */
	private static final String PWD = "bits_token_pwd";
	public final static Logger log = LoggerFactory.getLogger(TokenGenerator.class);

	/**
	 * 生成token
	 *
	 * @param str
	 * @return
	 */
	public static String createToken(String str) {
		if (StringUtils.isBlank(str)) {
			throw new RuntimeException("token不能为空");
		}
//		log.info("token生成："+str);
		byte[] byts;
		try {
			byts = AES.encrypt(str, PWD);
			String retToken = new String(Base64.encodeBase64(byts));
//			String retToken=Base64.encodeBase64URLSafeString(byts);
			//retToken=java.net.URLEncoder.encode(retToken,"UTF-8");
//			log.info("personcreateToken||"+retToken);
			return retToken;
		} catch (Exception e) {
			throw new RuntimeException("生成token出错");
		}
	}

	/**
	 * 解密token
	 *
	 * @param str
	 * @return
	 */
	public static String decodeToken(String str) {
		if (StringUtils.isBlank(str)) {
			throw new RuntimeException("token不能为空");
		}
		byte[] byts;
		try {
			log.debug("persondecodeToken||"+str);
			byts = Base64.decodeBase64(str.getBytes());
//			byts = Base64.decodeBase64(str);
			byts = AES.decrypt(byts, PWD);
			log.debug("解密之后:"+new String(byts));
			return new String(byts);
		} catch (Exception e) {
			throw new RuntimeException("解密token出错");
		}
	}



	/**
	 * 根据token获取userid
	 * @param token
	 * @return
	 */
	public static Long getUserIdByToken(String token) {
		String str = decodeToken(token);
		String[] ss = str.split("_");

		if (ss.length > 0) {
			try {
				Long userid = Long.parseLong(ss[0]);
				return userid;
			} catch (Exception e) {
				throw new RuntimeException("userid解析出错");
			}
		} else {
			throw new RuntimeException("解密token出错");
		}
	}

	public static void main(String[] args) throws Exception {
//		String s = createToken("hello world,李祥");
//		System.out.println(s);
//		System.out.println(decodeToken("q/zws+jl/ETTaoXQYT9h7CNlJCQW/Sp3nx9XtmXR73HHWWYTfcAAgaGh9Urd5ZivpBU5Zh10PWkY5sQhudHw95CiwIti3yFuydCNi5G/jgY="));
	}

}
