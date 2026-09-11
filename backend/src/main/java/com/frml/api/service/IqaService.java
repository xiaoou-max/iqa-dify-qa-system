package com.frml.api.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.frml.api.dao.MessageRepository;
import com.frml.api.dao.ShareMessageRelationshipRepository;
import com.frml.api.dao.ShareRecordRepository;
import com.frml.api.dao.UserRepository;
import com.frml.api.entity.Message;
import com.frml.api.entity.ShareMessageRelationship;
import com.frml.api.entity.ShareRecord;
import com.frml.api.entity.User;
import com.frml.api.exception.BusinessException;
import com.frml.api.util.HttpsClientUtil;
import com.frml.api.util.NullUtil;
import com.frml.api.util.TokenModel;
import com.frml.api.util.VerifyUtils;
import com.frml.api.vo.*;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Transactional
public class IqaService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${user.input.ids:wtlb}")
    private List<String> userInputIds;

    @Value("${api.auth.lybm:your_api_auth_lybm}")
    private String authLybm;

    @Value("${api.root.url:your_api_root_url}")
    private String apiUrl;

    @Value("${sso.token.login.url:your_api_root_url}")
    private String ssoTokenLoginUrl;

    @Value("${login.repetitive:0}")
    private String repetitiveLogin;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ShareRecordRepository shareRecordRepository;

    @Autowired
    private ShareMessageRelationshipRepository shareMessageRelationshipRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private RedisTokenManager tokenManager;

    @Value("${audio.savepath:}")
    private String audioSavepath;

    /**
     * 用户登录
     * @param account 账号
     * @param password 密码
     * @return 用户信息
     * @throws BusinessException 账号或密码错误
     */
    public User login(String account, String password) throws BusinessException {
        User user = userRepository.findByAccountAndPassword(account, password);
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        return user;
    }

    public User apiLogin(String account, String password) throws BusinessException {
        JSONObject body = new JSONObject();
        body.put("auth_lybm",authLybm);
        body.put("account",account);
        body.put("password",password);
        String login_url = apiUrl.endsWith("/") ? apiUrl+"apiconfig/aiLogin" : apiUrl+"/apiconfig/aiLogin";
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpPost(login_url, APIConstant.contenttype_form, JSONObject.toJSONString(body), null);
        } catch (Exception e) {
            throw new BusinessException("登录失败"+e.getMessage());
        }
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            JSONObject businessGrObj = HttpsClientUtil.getJSONData(adiResultJSONObj);
            String grmc = businessGrObj.getString("grmc");
            String grbm = businessGrObj.getString("grbm");
            String dlzh = businessGrObj.getString("dlzh");
            String dlmm = businessGrObj.getString("dlmm");
            String dwbms = businessGrObj.getString("dwbms");
            String jsbms = businessGrObj.getString("jsbms");
            //根据用户编码查询该用户是否已经存在
            User user = userRepository.findByCode(grbm);
            if (user == null) {
                user = new User();
                user.setCode(grbm);
            }
            user.setName(grmc);
            user.setAccount(dlzh);
            user.setPassword(dlmm);
            user.setDwbms(dwbms);
            user.setJsbms(jsbms);
            userRepository.save(user); //新增或更新
            return user;
        } else {
            throw new BusinessException(HttpsClientUtil.getStringData(adiResultJSONObj));
        }
    }

    public User ssoTokenLogin(String ssoToken) throws BusinessException {
        JSONObject body = new JSONObject();
        body.put("ssoToken",ssoToken);
        String login_url = ssoTokenLoginUrl;
        String adiResult = "";
        try {
            adiResult = HttpsClientUtil.httpPost(login_url, APIConstant.contenttype_json, JSONObject.toJSONString(body), null);
        } catch (Exception e) {
            throw new BusinessException("登录失败"+e.getMessage());
        }
        logger.info("ssoTokenLogin响应："+adiResult);
        JSONObject adiResultJSONObj = JSON.parseObject(adiResult);
        boolean success = HttpsClientUtil.isSuccess(adiResult);
        if(success) {
            JSONObject businessGrObj = HttpsClientUtil.getJSONData(adiResultJSONObj);
            String grmc = businessGrObj.getString("user_name");
            String grbm = businessGrObj.getString("user_uid");
            String dlzh = businessGrObj.getString("user_account");
            String dlmm = "<PASSWORD>";
            String dwbms = businessGrObj.getString("org_uids");
            String jsbms = businessGrObj.getString("role_uids");
            String extra_data_str = businessGrObj.getString("extra_data");
            JSONObject extra_data_json = new JSONObject();
            if(NullUtil.IsAllNotNullOfString(extra_data_str)) {
                Object json = new JSONTokener(extra_data_str).nextValue();
                if (json instanceof org.json.JSONObject) { //对象类型
                    extra_data_json = JSONObject.parseObject(extra_data_str, Feature.OrderedField);
                }
            }
            //根据用户编码查询该用户是否已经存在
            User user = userRepository.findByCode(grbm);
            if (user == null) {
                user = new User();
                user.setCode(grbm);
            }
            user.setName(grmc);
            user.setAccount(dlzh);
            user.setPassword(dlmm);
            user.setDwbms(dwbms);
            user.setJsbms(jsbms);
            userRepository.save(user); //新增或更新
            user.setExtraData(extra_data_json);
            return user;
        } else {
            throw new BusinessException(HttpsClientUtil.getStringData(adiResultJSONObj));
        }
    }

    public String setUserInfoIntoRedis(User user,String _from) {
        if(!VerifyUtils.isLoginFrom(_from)){
            throw new SecurityException("没有获得登录来源参数，或者登录来源参数不正确。_from="+_from);
        }
        String userIdStr = String.valueOf(user.getId());
        if("1".equals(repetitiveLogin)) { //允许在不同的设备上重复登录同一个账号
            String old_ft_token = tokenManager.getFtToken(userIdStr,_from);
            if(NullUtil.IsAllNotNullOfString(old_ft_token)) { //说明该用户在别的同类终端中已经登录了
                logger.info("该用户之前在["+_from+"]已登录，直接返回之前的token");
                return old_ft_token;
            }
        }
        try {
            //为了不透露密码，把密码清空
            User user_redis = new User();
            user_redis.setId(user.getId());

            user_redis.setCode(user.getCode());
            user_redis.setAccount(user.getAccount());
            user_redis.setName(user.getName());
            user_redis.setDwbms(user.getDwbms());
            user_redis.setJsbms(user.getJsbms());
            user_redis.setExtraData(user.getExtraData());
            logger.info("将登录信息设置到redis中>>>>");

            TokenModel tm = tokenManager.createToken(userIdStr,_from);
            tokenManager.setSession(userIdStr, APIConstant.USER_KEY, user_redis,_from);
            tokenManager.setSession(userIdStr, APIConstant.USER_KEY_ID, userIdStr,_from);
            tokenManager.setSession(userIdStr, APIConstant.USER_KEY_ACCOUNT, user.getAccount(),_from);
            tokenManager.setSession(userIdStr, APIConstant.USER_KEY_NAME, user.getName(),_from);
            tokenManager.setSession(userIdStr, APIConstant.USER_KEY_CODE, user.getCode(),_from);
            String frontToken = tm.getFt();
            logger.info("frontToken="+frontToken);
            return frontToken;
        } catch (Exception e) {
            logger.error("将登录信息设置到redis中失败", e);
            throw new BusinessException("将登录信息设置到redis中失败");
        }

    }

    public Long getLoginUserIDByToken(String token) {
        Map<String, Object> session = tokenManager.getRedisSession(token);
        return Long.parseLong(session.get(APIConstant.USER_KEY_ID).toString());
    }

    public User getLoginUserByToken(String token) {
        Map<String, Object> session = tokenManager.getRedisSession(token);
        JSONObject userJson = (JSONObject) session.get(APIConstant.USER_KEY);
        User currentUser = userJson.toJavaObject(User.class);
        return currentUser;
    }

    public APIResponse parameters() {
        return new APIResponse(APIConstant.api_success, aiService.getAIAppParameters(userInputIds));
    }

    public Message saveChatRecord(ChatRecord chatRecord) {
        JSONObject retObject = new JSONObject();
        try {
            // 保存问题记录
            Message message = new Message();
            Long userId = chatRecord.getUserId();
            User user = userRepository.findById(userId).get();
            message.setUser(user);
            message.setConversationId(chatRecord.getConversationId());
            message.setMessageId(chatRecord.getMessageId());
            message.setCreateTime(chatRecord.getCreateTime());
            message.setQuestion(chatRecord.getQuestion());
            message.setAnswerStartTime(chatRecord.getAnswerStartTime());
            message.setAnswer(chatRecord.getAnswer());
            message.setAnswerEndTime(chatRecord.getAnswerEndTime());
            message = messageRepository.save(message);
            return message;
        } catch (Exception e) {
            logger.error("保存聊天记录时发生错误", e);
            throw new BusinessException("保存聊天记录时发生错误");
        }
    }



    public SseEmitter chat(ChatRequest chatRequest) throws Exception {
        logger.info("进入IqaService-chat");
        return aiService.chat(chatRequest);
    }

    public APIResponse getConversations(ConversationsRequest conversationsRequest) {
        return new APIResponse(APIConstant.api_success, aiService.getConversations(conversationsRequest));
    }

    public APIResponse getMessages(String user, String conversationId, String firstId, String limit) {
        return new APIResponse(APIConstant.api_success, aiService.getMessages(user, conversationId, firstId, limit));
    }

    public APIResponse stop(String user, String taskId) {
        return new APIResponse(APIConstant.api_success, aiService.stop(user, taskId));
    }

    private String generateByUUID() {
        // 生成UUID并去除连字符
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 截取前12位十六进制字符
        return uuid.substring(0, 12);
    }

    public APIResponse createShare(ShareRequest shareRequest, String userId) {
        // 1. 先保存ShareRecord，生成ID
        String code = this.generateByUUID();
        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setCode(code);
        User user = userRepository.findById(Long.parseLong(userId)).get();
        shareRecord.setUser(user);
        shareRecord.setCreateTime(new Date());
        shareRecord = shareRecordRepository.save(shareRecord);

        // 2. 创建关联关系
        List<ShareMessageRelationship> relationships = new ArrayList<>();
        for (String messageId : shareRequest.getMessageIds()) {
            Message message = messageRepository.findByMessageId(messageId);
            if(!NullUtil.IsAllNotNullOfObject(message)) {
                throw new BusinessException("消息不存在");
            }
            ShareMessageRelationship relationship = new ShareMessageRelationship();
            relationship.setShareRecord(shareRecord);
            relationship.setMessage(message);
            relationships.add(relationship);
        }
        // 3. 批量保存关联关系
        shareMessageRelationshipRepository.saveAll(relationships);
        return new APIResponse(APIConstant.api_success, code);
    }

    public APIResponse getShare(String code) {
        ShareRecord shareRecord = shareRecordRepository.findByCode(code);
        if(NullUtil.IsAllNotNullOfObject(shareRecord)) {
            List<ShareMessageRelationship> relationships = shareRecord.getMessageRelationships();
            ShareResponse shareResponse = new ShareResponse();
            List<MessageVo> messageVos = new ArrayList<>();
            shareResponse.setMessages(messageVos);
            List<String> messageIds = new ArrayList<>();
            String answer = "";
            for (ShareMessageRelationship relationship : relationships) {
                Message message = relationship.getMessage();
                if(messageIds.contains(message.getMessageId())) {
                    continue;
                }
                MessageVo messageVo = new MessageVo();
                messageVo.setId(message.getMessageId());
                messageVo.setConversation_id(message.getConversationId());
                messageVo.setCreated_at(message.getCreateTime().getTime());
                messageVo.setQuery(message.getQuestion());
                answer = message.getAnswer();
                answer = this.cleanAnswer(answer);
                messageVo.setAnswer(answer);
                messageVos.add(messageVo);
                messageIds.add(message.getMessageId());
            }
            // 使用Comparator按created_at升序排序
            Collections.sort(messageVos, Comparator.comparingLong(MessageVo::getCreated_at));
            return new APIResponse(APIConstant.api_success, shareResponse);
        } else {
            throw new BusinessException("分享不存在");
        }
    }

    public User autoRegister() {
        String account = UUID.randomUUID().toString();
        String name = "Guest_" + System.currentTimeMillis();
        String password = "<PASSWORD>";
        User user = new User();
        user.setCode(account);
        user.setAccount(account);
        user.setName(name);
        user.setPassword(password);
        userRepository.save(user);
        return user;
    }

    public User getUserInfo(String userId) {
        Long id = Long.parseLong(userId);
        return userRepository.findById(id).orElse(null);
    }

    public APIResponse getSuggestedQuestions(String user, String messageId) {
        APIResponse apiResponse = new APIResponse(APIConstant.api_success, aiService.getSuggestedQuestions(user, messageId));
        logger.info("apiResponse："+JSONObject.toJSONString(apiResponse));
        return apiResponse;
    }

    public APIResponse deleteConversation(String user, String conversationId) {
        return new APIResponse(APIConstant.api_success, aiService.deleteConversation(user, conversationId));
    }

    public APIResponse renameConversation(String user, JSONObject params) {
        return new APIResponse(APIConstant.api_success, aiService.renameConversation(user, params));
    }

    public String getFileBase64(String filename) {
        byte[] buffer = this.getFileByPath(filename);
        return new BASE64Encoder().encode(buffer);
    }

    private byte[] getFileByPath(String filename) {
        Path fullPath = Paths.get(audioSavepath, filename);
        String filepath = fullPath.toString();
        logger.info("文件路径:{}",filepath);
        File f = new File(filepath);
        if(!f.exists()) {
            throw new RuntimeException("file does not exist");
        }
        if (!f.isFile()) {
            throw new RuntimeException("file does not exist");
        } else {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
                byte[] b = new byte[1000];
                int n;
                logger.info("开始读取目标文件:{}",filepath);
                while ((n = is.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                logger.info("完成读取目标文件:{}",filepath);
                return bos.toByteArray();
            } catch(Exception e) {
                logger.error("文件读取错误！",e);
                throw new RuntimeException("文件读取错误！请联系系统管理员！");
            }finally {
                if (is != null){
                    try {
                        is.close();
                    } catch(Exception e) {
                        throw new RuntimeException("文件流关闭失败！请联系系统管理员！");
                    }
                }
            }
        }
    }


    /**
     * 过滤字符串：
     * 1. 去掉</audio_end>和它之前的所有内容
     * 2. 去掉<suggested_start>和它之后的所有内容
     * @param originalStr 原始字符串
     * @return 处理后的字符串
     */
    public String cleanAnswer(String originalStr) {
        // 空值判断
        if (originalStr == null || originalStr.isEmpty()) {
            return "";
        }

        String tempStr = originalStr;
        // 第一步：去掉</audio_end>和它之前的所有内容
        String audioEndTag = "</audio_end>";
        int audioEndIndex = tempStr.indexOf(audioEndTag);
        if (audioEndIndex != -1) {
            // 截取</audio_end>之后的内容
            tempStr = tempStr.substring(audioEndIndex + audioEndTag.length());
        }

        // 第二步：去掉<suggested_start>和它之后的所有内容
        String suggestedStartTag = "<suggested_start>";
        int suggestedStartIndex = tempStr.indexOf(suggestedStartTag);
        if (suggestedStartIndex != -1) {
            // 截取<suggested_start>之前的内容
            tempStr = tempStr.substring(0, suggestedStartIndex);
        }
        // 去除首尾空白字符（可选，根据实际需求调整）
        return tempStr.trim();
    }
}
