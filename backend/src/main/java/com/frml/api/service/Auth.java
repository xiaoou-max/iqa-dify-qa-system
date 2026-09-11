package com.frml.api.service;

import java.lang.annotation.*;

/**
 * 权限拦截注解
 * 权限分三级：
 * 1:不需要登录即可访问 注解auth, NotLogin=true
 * 2:登录即可操作，不需要额外授权,不用注解任何东西
 * 3：不光登录，还需要区分人员拥有的权限 注解auth,needHDID=活动ID;
 * @author wfq
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Auth {
    /**
     * 是否不需要登录认证
     * @return
     */
    public abstract boolean NotLogin()  default false;

}
