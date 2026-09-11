package com.frml.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.frml.api.config.MenuListConfig;
import com.frml.api.entity.User;
import com.frml.api.exception.BusinessException;
import com.frml.api.service.Auth;
import com.frml.api.service.IqaService;
import com.frml.api.service.RedisTokenManager;
import com.frml.api.util.*;
import com.frml.api.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sun.misc.BASE64Decoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;

/**
 * @Author: 用智能拥抱幸福
 * @Description: controller
 * @Date: 6/16/016 10:11
 */
@RestController
@RequestMapping("/api")
public class IqaController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${login.mode:noLogin}")
    private String loginMode;

    @Value("${audio.flag:0}")
    private int audioFlag; //是否需要展示音频播放按钮

    @Autowired
    private MenuListConfig menuListConfig;

    @Autowired
    private IqaService iqaService;

    @Autowired
    private RedisTokenManager tokenManager;


    /**
     * 获取全局配置，包括：登录模式
     * @return
     */
    @GetMapping("/globalConfig")
    @Auth(NotLogin = true)
    public APIResponse getGlobalConfig() {
        JSONObject globalConfig = new JSONObject();
        globalConfig.put("loginMode",loginMode);
        globalConfig.put("audioFlag",audioFlag);
        return new APIResponse("success",globalConfig);
    }

    /**
     * 用户登录接口（暂未启用）
     * @param params 登录参数
     * @return APIResponse
     */
    @PostMapping("/login")
    @Auth(NotLogin = true)
    public APIResponse login(@RequestBody JSONObject params, HttpServletRequest request, HttpServletResponse response) {
        try {
            if(!"oriLogin".equals(loginMode)) {
                return new APIResponse(APIConstant.api_needLogin,"登录模式不匹配");
            }
            String account = params.getString("account");
            String password = params.getString("password");
            if(!NullUtil.IsAllNotNullOfString(account, password)) {
                return new APIResponse("fail", "账号或密码不能为空");
            }
            User user = iqaService.login(account, password);
            String _from = request.getHeader("_from");
            // 设置redis信息并获取front token
            String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
            List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
            JSONObject res = new JSONObject();
            res.put("_token",frontToken);
            res.put("name",user.getName());
            res.put("menus",menus);
            return new APIResponse("success", res);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse("fail", e.getMessage());
        }
    }

    private List<MenuListConfig.Menu> initMenus(String jsbms) {
        logger.info("准备初始化当前用户的菜单...");
        logger.info("当前用户的角色："+jsbms);
        List<MenuListConfig.Menu> result = new ArrayList<>();
        List<String> menuRoutes = new ArrayList<>();
        List<MenuListConfig.Menu> allMenus = menuListConfig.getMenuList();
        List<String> roleList = JsonUtil.parseJsonArrayOrString(jsbms);
        logger.info("roleList："+JSONObject.toJSONString(roleList));
        if(roleList.size() == 0) { //当前用户没有角色编码
            for(MenuListConfig.Menu menu : allMenus) {
                logger.info("menu.getRoleUidList()："+JSONObject.toJSONString(menu.getRoleUidList()));
                if(menu.getRoleUidList().size() == 0) { //当前菜单不需要权限控制
                    logger.info("菜单名称："+menu.getName());
                    if(!menuRoutes.contains(menu.getRoute())) {
                        result.add(menu);
                        menuRoutes.add(menu.getRoute());
                    }
                }
            }
        } else {
            for(String role : roleList) {
                logger.info("role："+role);
                for(MenuListConfig.Menu menu : allMenus) {
                    logger.info("menu.getRoleUidList()："+JSONObject.toJSONString(menu.getRoleUidList()));
                    if(menu.getRoleUidList().size() == 0 || menu.getRoleUidList().contains(role)) {
                        logger.info("菜单名称："+menu.getName());
                        if(!menuRoutes.contains(menu.getRoute())) {
                            result.add(menu);
                            menuRoutes.add(menu.getRoute());
                        }
                    }
                }
            }
        }
        // 排序：先按 sort 升序，sort 相同则按 name 升序
        result.sort(
                Comparator.comparingInt(MenuListConfig.Menu::getSort) // 第一排序键：sort 升序
                        .thenComparing(MenuListConfig.Menu::getName) // 第二排序键：name 升序
        );
        logger.info("当前用户的菜单："+JSONObject.toJSONString(result));
        return result;
    }

    /**
     * 调用"问数坊-API低代码平台的登录接口"，当loginMode=apiLogin时使用
     * @param params
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/apiLogin")
    @Auth(NotLogin = true)
    public APIResponse apiLogin(@RequestBody JSONObject params, HttpServletRequest request, HttpServletResponse response) {
        try {
            if(!"apiLogin".equals(loginMode)) {
                return new APIResponse(APIConstant.api_needLogin,"登录模式不匹配");
            }
            String account = params.getString("account");
            String password = params.getString("password");
            if(!NullUtil.IsAllNotNullOfString(account, password)) {
                return new APIResponse("fail", "账号或密码不能为空");
            }
            User user = iqaService.apiLogin(account, password);
            String _from = request.getHeader("_from");
            // 设置redis信息并获取front token
            String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
            List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
            JSONObject res = new JSONObject();
            res.put("_token",frontToken);
            res.put("name",user.getName());
            res.put("menus",menus);
            return new APIResponse("success", res);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse("fail", e.getMessage());
        }
    }

    /**
     * 调用"问数坊-API低代码平台的登录接口"，当loginMode=apiLogin时使用
     * @param params
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/ssoLogin")
    @Auth(NotLogin = true)
    public APIResponse ssoLogin(@RequestBody JSONObject params, HttpServletRequest request, HttpServletResponse response) {
        try {
            if(!"ssoTokenLogin".equals(loginMode)) {
                return new APIResponse(APIConstant.api_needLogin,"登录模式不匹配");
            }
            String ssoToken = params.getString("ssoToken");
            if(!NullUtil.IsAllNotNullOfString(ssoToken)) {
                return new APIResponse("fail", "ssoToken不能为空");
            }
            User user = iqaService.ssoTokenLogin(ssoToken);
            String _from = request.getHeader("_from");
            // 设置redis信息并获取front token
            String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
            List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
            JSONObject res = new JSONObject();
            res.put("_token",frontToken);
            res.put("name",user.getName());
            res.put("menus",menus);
            return new APIResponse("success", res);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse("fail", e.getMessage());
        }
    }

    @PostMapping("/logout")
    public APIResponse logout(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtils.getToken(request);
        if(NullUtil.IsAllNotNullOfString(token)) {
            TokenModel tm = tokenManager.getTokenModel(token);
            String _from = request.getHeader("_from");
            if(!VerifyUtils.isLoginFrom(_from)){
                throw new SecurityException("没有获得登录来源参数，或者登录来源参数不正确。_from="+_from);
            }
            tokenManager.deleteToken(tm.getUserId(),_from);
            return new APIResponse("success","操作成功");
        }
        return new APIResponse("fail","操作失败");
    }

    /**
     * 自动登录接口，当loginMode=noLogin时使用（进入聊天页面必调用）：
     * 1没传_token，自动创建uuid用户，自动登录，给前端返回_token
     * 2传了_token，判断_token是否有效，
     * 21有效则延长有效期
     * 22无效则从_token解析到userId并从数据库查询用户是否有效（如果解析不到userId则报错）
     * 221有效则自动登录，给前端返回新_token
     * 222无效则创建uuid用户，自动登录，给前端返回_token
     * @param request
     * @return
     */
    @PostMapping("/autoLogin")
    @Auth(NotLogin = true)
    public APIResponse autoLogin(HttpServletRequest request) {
        logger.info("进入autoLogin");
        String _token = request.getHeader("_token");
        String _from = request.getHeader("_from");
        if(NullUtil.IsAllNotNullOfString(_token)) { //前端传了token
            logger.info("前端传入_token："+_token);
            TokenModel tm = tokenManager.getTokenModel(_token);
            if(tm != null) { //前端传了token，且token符合规则
                logger.info("前端传入的_token格式正确");
                String userId = tm.getUserId();
                User user = iqaService.getUserInfo(userId);
                if(null == user) { //token符合规则，但用户不存在
                    if(!"noLogin".equals(loginMode)) {
                        return new APIResponse(APIConstant.api_needLogin,"未登录");
                    }
                    logger.info("_token格式符合规则，但用户不存在，将自动注册新用户");
                    user = iqaService.autoRegister(); //自动注册新用户
                    // 设置redis信息并获取front token
                    String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
                    List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
                    logger.info("响应_token："+frontToken);
                    JSONObject res = new JSONObject();
                    res.put("_token",frontToken);
                    res.put("name",user.getName());
                    res.put("menus",menus);
                    return new APIResponse("success", res);
                } else {
                    logger.info("_token格式符合规则，且数据库用户存在，准备判断_token是否过期");
                    Map<String, Object> info = tokenManager.getRedisSession(_token);
                    if(null == info || info.isEmpty()) { //token已经超过有效期
                        logger.info("_token已过期，准备生成新的_token");
                        // 设置redis信息并获取front token
                        String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
                        List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
                        logger.info("响应_token："+frontToken);
                        JSONObject res = new JSONObject();
                        res.put("_token",frontToken);
                        res.put("name",user.getName());
                        res.put("menus",menus);
                        return new APIResponse("success", res);
                    } else {
                        logger.info("_token在有效期内，准备延长有效期");
                        tokenManager.extendToken(_token); //延长token有效期
                        List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
                        JSONObject res = new JSONObject();
                        res.put("_token",_token);
                        res.put("name",user.getName());
                        res.put("menus",menus);
                        logger.info("响应_token："+_token);
                        return new APIResponse("success", res);
                    }
                }
            } else {
                return new APIResponse(APIConstant.api_fail, "token无效");
            }
        } else { //前端没传token，首次访问，创建新用户
            if(!"noLogin".equals(loginMode)) {
                return new APIResponse(APIConstant.api_needLogin,"未登录");
            }
            logger.info("前端没传_token，首次访问，创建新用户");
            User user = iqaService.autoRegister();
            // 设置redis信息并获取front token
            String frontToken = iqaService.setUserInfoIntoRedis(user,_from);
            List<MenuListConfig.Menu> menus = initMenus(user.getJsbms());
            logger.info("响应_token："+frontToken);
            JSONObject res = new JSONObject();
            res.put("_token",frontToken);
            res.put("name",user.getName());
            res.put("menus",menus);
            return new APIResponse("success", res);
        }
    }

    /**
     * 获取AI应用的基本参数，包括开场白和预设问题
     * @return
     */
    @GetMapping("/parameters")
    @Auth(NotLogin = true)
    public APIResponse parameters() {
        return this.iqaService.parameters();
    }

    @GetMapping("/suggested-questions")
    public APIResponse getSuggestedQuestions(HttpServletRequest request,String message_id) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            return iqaService.getSuggestedQuestions(userID.toString(),message_id);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }
    }


    @GetMapping("/conversations")
    public APIResponse getConversations(@RequestParam(defaultValue = "20") String limit, @RequestParam(required = false) String last_id, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            ConversationsRequest conversationsRequest = new ConversationsRequest();
            conversationsRequest.setUser(userID.toString());
            conversationsRequest.setLimit(limit);
            conversationsRequest.setLast_id(last_id);
            return this.iqaService.getConversations(conversationsRequest);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }

    }

    /**
     * 删除会话
     * @param params
     * @param request
     * @return
     */
    @PostMapping("/deleteConversation")
    public APIResponse deleteConversation(@RequestBody JSONObject params, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            String conversation_id = params.getString("conversation_id");
            if(!NullUtil.IsAllNotNullOfString(conversation_id) ) {
                throw new BusinessException("conversation_id不能为空");
            }
            return this.iqaService.deleteConversation(userID.toString(),conversation_id);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }
    }

    /**
     * 重命名会话
     * @param params
     * @param request
     * @return
     */
    @PostMapping("/renameConversation")
    public APIResponse renameConversation(@RequestBody JSONObject params, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            String conversation_id = params.getString("conversation_id");
            if(!NullUtil.IsAllNotNullOfString(conversation_id) ) {
                throw new BusinessException("conversation_id不能为空");
            }
            String name = params.getString("name");
            if(!NullUtil.IsAllNotNullOfString(name) ) {
                throw new BusinessException("name不能为空");
            }
            return this.iqaService.renameConversation(userID.toString(),params);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }
    }

    @GetMapping("/messages")
    public APIResponse getMessages(@RequestParam(defaultValue = "20") String limit, @RequestParam(required = false) String first_id, @RequestParam(required = false) String conversation_id, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            return this.iqaService.getMessages(userID.toString(),conversation_id,first_id,limit);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }

    }

    /**
     * 下载WAV音频文件
     *
     * @param message_id      消息ID
     * @param response       HTTP响应对象
     * @return API响应结果
     */
    @GetMapping("/download_audio")
    public Object download_audio(String message_id, HttpServletResponse response) {
        if (!NullUtil.IsAllNotNullOfString(message_id)) {
            return new APIResponse(APIConstant.api_fail, "缺失必要参数");
        }

        String fileName = message_id + ".wav";
        try {
            String fileContent = iqaService.getFileBase64(fileName);
            // 2. 校验文件内容非空
            if (fileContent == null || fileContent.trim().isEmpty()) {
                return new APIResponse(APIConstant.api_fail, "音频文件不存在");
            }

            // 3. 修正Content-Type（WAV专用）+ 修复拼写错误（conent-type→Content-Type）
            response.setHeader("Content-Type", "audio/wav");
            response.setContentType("audio/wav");

            // 4. 处理文件名乱码（用URLEncoder，兼容中文）
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

            OutputStream os = response.getOutputStream();
            // 5. 保留你原来的decoder方法，若没引入StandardCharsets就不用它
            decoderBase64OutputStream(fileContent, os);
            os.flush();
            os.close();
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new APIResponse(APIConstant.api_fail, "下载失败：" + e.getMessage());
        }
    }

    /**
     * @param base64Code
     * @param out
     * @throws Exception
     */
    private static void decoderBase64OutputStream(String base64Code, OutputStream out)
            throws Exception {
        byte[] buffer = new BASE64Decoder().decodeBuffer(base64Code);
        out.write(buffer);
        out.flush();
        out.close();
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        logger.info("进入controller-chat");
        SseEmitter emitter = new SseEmitter(0L);

        try {
            String token = CookieUtils.getToken(request);
            Long userID = iqaService.getLoginUserIDByToken(token);
            User currentUser = iqaService.getLoginUserByToken(token);
            if(!"noLogin".equals(loginMode)) {
                JSONObject inputs = chatRequest.getInputs();
                if(NullUtil.IsAllNotNullOfString(currentUser.getCode())) {
                    inputs.put("user_uid",currentUser.getCode());
                }
                if(NullUtil.IsAllNotNullOfString(currentUser.getDwbms())) {
                    inputs.put("org_uids",currentUser.getDwbms());
                }
                if(NullUtil.IsAllNotNullOfString(currentUser.getJsbms())) {
                    inputs.put("role_uids",currentUser.getJsbms());
                }
                if(NullUtil.IsAllNotNullOfObject(currentUser.getExtraData())) {
                    inputs.put("extra_data",JSONObject.toJSONString(currentUser.getExtraData(), SerializerFeature.WriteMapNullValue));
                }
                chatRequest.setInputs(inputs);
            }
            chatRequest.setUser(userID.toString());
            if(!NullUtil.IsAllNotNullOfString(chatRequest.getQuery())) {
                throw new BusinessException("query不能为空");
            }
            if(!NullUtil.IsAllNotNullOfString(chatRequest.getResponse_mode())) {
                chatRequest.setResponse_mode("streaming");
            }
            if(!NullUtil.IsAllNotNullOfObject(chatRequest.getInputs())) {
                chatRequest.setInputs(new JSONObject());
            }
            chatRequest.setAuto_generate_name(true);
            // 设置超时处理
            emitter.onTimeout(() -> {
                try {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("error", "请求超时");
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(errorJson.toJSONString()));
                    emitter.complete();
                    logger.error("请求超时: conversationId={}", chatRequest.getConversation_id());
                } catch (IOException e) {
                    logger.error(e.getMessage(),e); //把异常信息打印到日志文件
                    logger.error("发送超时消息失败", e);
                }
            });

            return iqaService.chat(chatRequest);
        } catch (Exception e) {
            try {
                logger.error(e.getMessage(),e); //把异常信息打印到日志文件
                // 发送错误消息，使用正确的 SSE 格式
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", e.getMessage());
                errorJson.put("event","message_end");
                logger.info("发送异常消息: " + errorJson);
                emitter.send(errorJson.toJSONString());
                emitter.complete();
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
            return emitter;
        }
    }

    @PostMapping("/stop")
    public APIResponse stop(@RequestBody JSONObject params, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            String task_id = params.getString("task_id");
            if(!NullUtil.IsAllNotNullOfString(task_id) ) {
                throw new BusinessException("task_id不能为空");
            }
            return this.iqaService.stop(userID.toString(),task_id);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }

    }

    @PostMapping("/share")
    public APIResponse createShare(@RequestBody ShareRequest shareRequest, HttpServletRequest request) {
        try {
            Long userID = iqaService.getLoginUserIDByToken(CookieUtils.getToken(request));
            if(!NullUtil.IsAllNotNullOfObject(shareRequest.getMessageIds()) || shareRequest.getMessageIds().size()==0) {
                throw new BusinessException("messageIds不能为空");
            }
            return this.iqaService.createShare(shareRequest,userID.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }
    }

    @GetMapping("/share")
    @Auth(NotLogin = true)
    public APIResponse getShare(@RequestParam("code") String code, HttpServletRequest request) {
        try {
            return this.iqaService.getShare(code);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new APIResponse(APIConstant.api_fail, e.getMessage());
        }

    }

}
