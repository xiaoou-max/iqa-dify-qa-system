package com.frml.api.interceptor;

import com.alibaba.fastjson.JSON;
import com.frml.api.service.Auth;
import com.frml.api.service.RedisTokenManager;
import com.frml.api.util.CookieUtils;
import com.frml.api.vo.APIConstant;
import com.frml.api.vo.APIResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisTokenManager tokenManager;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        // 预处理（返回true才会继续处理请求）
        try{
            this.checkAuth(req,res,handler);
            return true;
        } catch(Exception e){
            res.setCharacterEncoding("UTF-8");
            res.setContentType("application/json; charset=utf-8");
            PrintWriter out = res.getWriter();
            APIResponse apiResponse = new APIResponse(APIConstant.api_needLogin,"未登录");
            res.setStatus(401);
            out.append(JSON.toJSONString(apiResponse));
            return false;
        }
    }

    private void checkAuth(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            Auth na = hm.getMethodAnnotation(Auth.class);
            if ( na != null && na.NotLogin() ) { // 不需要登录即可访问
                logger.info("无需登录即可访问>>>{}",req.getRequestURI());
                return;
            } else {
                logger.info("准备进行登录状态校验>>>{}",req.getRequestURI());
            }
        }
        // 下面的都需要登录才能访问
        String _token = CookieUtils.getToken(req);
        if(StringUtils.isBlank(_token)){
            throw new NotLoginException("未检测到TOKEN");
        }
        Map<String, Object> info = tokenManager.getRedisSession(_token);

        if(null == info || info.isEmpty()) {
            throw new NotLoginException("认证失败");
        } else {
            tokenManager.extendToken(_token); //延长token有效期
        }

    }

    class NotLoginException extends RuntimeException{
        public NotLoginException(String message) {
            super(message);
        }
    }

}
