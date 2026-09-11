package com.frml.api.service;

import com.alibaba.fastjson.JSON;
import com.frml.api.util.NullUtil;
import com.frml.api.util.TokenGenerator;
import com.frml.api.util.TokenModel;
import com.frml.api.util.VerifyUtils;
import com.frml.api.vo.APIConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通过Redis存储和验证token的实现类
 *
 * @author wfq
 *
 */
@Service
public class RedisTokenManager {
	private static final Logger log = LoggerFactory.getLogger(RedisTokenManager.class);
	@SuppressWarnings("rawtypes")
	@Resource(name = "adiRedisTemplate")
	private RedisTemplate redis;

	public TokenModel createToken(String userId, String _from) {
		// 使用uuid作为源token
		String token = UUID.randomUUID().toString().replace("-", "");
		TokenModel model = new TokenModel(userId, token);
		// 存储到redis并设置过期时间
		if (VerifyUtils.LOGIN_FROM_APP.equals(_from) || VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录token为7天
			redis.boundValueOps(userId + "_" + _from).set(model, APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
		}else {//pc登录时间为30分钟
			redis.boundValueOps(userId + "_" + _from).set(model, APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
		}
//		Object obj = redis.boundValueOps(userId + "_" + _from).get();
		return model;
	}

	public boolean checkToken(TokenModel model,String _from) {
		if(!VerifyUtils.isLoginFrom(_from)){
			throw new SecurityException("没有获得登录来源参数，或者登录来源参数不正确。_from="+_from);
		}
		if (model == null || model.getUserId() == null) {
			return false;
		}
		Object token = redis.boundValueOps(model.getUserId() + "_" + _from).get();
		if (token == null) {
			return false;
		} else {
			TokenModel mytoken = JSON.parseObject(JSON.toJSONString(token),TokenModel.class);
			if (!mytoken.getToken().equals(model.getToken())) {
				return false;
			}
		}
		// 如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
		if (VerifyUtils.LOGIN_FROM_APP.equals(_from)||VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录的延长token为7天
			redis.boundValueOps(model.getUserId() + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
		}else {//pc延长30分钟
			redis.boundValueOps(model.getUserId() + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
		}
		return true;
	}

	public void deleteToken(String userId,String _from) {
		redis.delete(userId + "_" + _from);
	}

	/**
	 * 删除某个用户的全部登录信息，用于密码重置等等
	 **/
	public void deleteTokenByUserid(String userId){
		for (String loginFromValue : VerifyUtils.LOGIN_FROM_VALUES) {
			deleteToken(userId,loginFromValue);
		}
	}

	public Map<String, Object> getRedisSession(String userid,String _from) {
		String _fromTemp=_from;
		Object obj=null;
		//获取session时，如果登录的时候是dsf，但是后面的接口可能传入或者pc或者app
		if(VerifyUtils.LOGIN_FROM_PC.equals(_from)||VerifyUtils.LOGIN_FROM_APP.equals(_from)){
			_fromTemp=VerifyUtils.LOGIN_FROM_DSF;
			obj = redis.boundValueOps(userid + "_" + _fromTemp).get();
			if(obj!=null){//确实是dsf登录的
				_from = VerifyUtils.LOGIN_FROM_DSF;
			}
		}
		obj = redis.boundValueOps(userid + "_" + _from).get();

		if (obj != null) {
			log.info("获取到session");
			TokenModel tm = JSON.parseObject(JSON.toJSONString(obj),TokenModel.class);
			// 如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
			if (VerifyUtils.LOGIN_FROM_APP.equals(_from)||VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录的延长token为7天
				redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
			}else {//pc延长30分钟
				redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
			}
			return tm.getSession();
		}
		return null;
	}

	/**
	 * 获取之前给前端返回的token
	 * @param userid
	 * @param _from
	 * @return
	 */
	public String getFtToken(String userid,String _from) {
		String _fromTemp=_from;
		Object obj=null;
		//获取session时，如果登录的时候是dsf，但是后面的接口可能传入或者pc或者app
		if(VerifyUtils.LOGIN_FROM_PC.equals(_from)||VerifyUtils.LOGIN_FROM_APP.equals(_from)){
			_fromTemp=VerifyUtils.LOGIN_FROM_DSF;
			obj = redis.boundValueOps(userid + "_" + _fromTemp).get();
			if(obj!=null){//确实是dsf登录的
				_from = VerifyUtils.LOGIN_FROM_DSF;
			}
		}
		obj = redis.boundValueOps(userid + "_" + _from).get();

		if (obj != null) {
			log.info("获取到session");
			TokenModel tm = JSON.parseObject(JSON.toJSONString(obj),TokenModel.class);
			// 如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
			if (VerifyUtils.LOGIN_FROM_APP.equals(_from)||VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录的延长token为7天
				redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
			}else {//pc延长30分钟
				redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
			}
			return tm.getFt();
		}
		return null;
	}

	/**
	 * 从redis里获取用户信息
	 * @return
	 */
	public Map<String,Object> getRedisSessionByUserIdAndToken(String userid,String token){
		for (String loginFromValue : VerifyUtils.LOGIN_FROM_VALUES) {
			Object obj = redis.boundValueOps(userid + "_" + loginFromValue).get();
			if(obj != null){
				TokenModel tm = JSON.parseObject(JSON.toJSONString(obj),TokenModel.class);
				String redis_token = tm.getToken();
				String handle_qianduan_token = TokenGenerator.decodeToken(token).replace(userid+"_","");
				if(handle_qianduan_token.equals(redis_token)){
					return tm.getSession();
				}
			}
		}

		return null;
	}

	public void setSession(String userid, String key, Object value,String _from) {
		if(!VerifyUtils.isLoginFrom(_from)){
			throw new SecurityException("没有获得登录来源参数，或者登录来源参数不正确。_from="+_from);
		}
		Object obj = redis.boundValueOps(userid + "_" + _from).get();
		if (obj != null) {
			TokenModel tm = JSON.parseObject(JSON.toJSONString(obj),TokenModel.class);
			tm.getSession().put(key, value);
			if (VerifyUtils.LOGIN_FROM_APP.equals(_from)||VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录的token为7天
				redis.boundValueOps(userid + "_" + _from).set(tm, APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
			}else {//pc的为30分钟
				redis.boundValueOps(userid + "_" + _from).set(tm, APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
			}
		}

	}

	public Map<String, Object> getRedisSession(String token) {
		TokenModel tm = getTokenModel(token);
		if (tm != null) {
			return this.getRedisSessionByUserIdAndToken(tm.getUserId(),token);
		}
		return null;
	}



	public TokenModel getTokenModel(String token) {
		if (token == null || token.length() == 0) {
			return null;
		}
		String str = TokenGenerator.decodeToken(token);
		String[] param = str.split("_");
		if (param.length != 2) {
			return null;
		}
//		Long userId = Long.parseLong(param[0]);
		String userId = param[0];
		String uuid = param[1];
		TokenModel tm = new TokenModel(userId, uuid);
		return tm;
	}

	/**
	 * 根据字符串token 获取登录终端_from的值
	 * @param token
	 * @return
	 */
	public String getTokenFrom(String token){
		TokenModel tm = getTokenModel(token);
		String userid = tm.getUserId();
		String _from = null;
		for (String loginFromValue : VerifyUtils.LOGIN_FROM_VALUES) {
			Object obj = redis.boundValueOps(userid + "_" + loginFromValue).get();
			if(obj != null){
				TokenModel tm2 = JSON.parseObject(JSON.toJSONString(obj),TokenModel.class);
				String redis_token = tm2.getToken();
				String handle_qianduan_token = TokenGenerator.decodeToken(token).replace(userid+"_","");
				if(handle_qianduan_token.equals(redis_token)){
					return loginFromValue;
				}
			}
		}
		throw new SecurityException("无效的token");
	}

	public boolean setRedisValue(String key, String value, Long expiresHour) {
		redis.boundValueOps(key).set(value, expiresHour, TimeUnit.HOURS);
		return true;
	}

	public String getRedisValue(String key) {
		Object obj = redis.boundValueOps(key).get();
		if (obj != null) {
			return (String) obj;
		}
		return null;
	}

	/**
	 * 延长token到期时间
	 */
	public void extendToken(String token){
		String _from = getTokenFrom(token);
		TokenModel tm = getTokenModel(token);
		String userid = tm.getUserId();
		// 如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
		if (VerifyUtils.LOGIN_FROM_APP.equals(_from)||VerifyUtils.LOGIN_FROM_DSF.equals(_from)) { //如果是app登录的延长token为7天
			redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_HOUR_APP_DSF, TimeUnit.HOURS);
		}else {//pc延长30分钟
			redis.boundValueOps(userid + "_" + _from).expire(APIConstant.TOKEN_EXPIRES_MINUTE_PC, TimeUnit.MINUTES);
		}
	}
}
