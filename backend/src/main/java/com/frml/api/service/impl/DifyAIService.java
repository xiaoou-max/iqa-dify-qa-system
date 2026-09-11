package com.frml.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.frml.api.entity.Message;
import com.frml.api.exception.BusinessException;
import com.frml.api.service.AIService;
import com.frml.api.service.IqaService;
import com.frml.api.util.*;
import com.frml.api.vo.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("dify")
public class DifyAIService implements AIService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Lazy
    private IqaService iqaService;

    @Value("${app.describe:数据驱动决策，智能降本增效}")
    private String appDescribe;

    @Value("${dify.api.key}")
    private String apiKey;

    @Value("${cloud.aly.dify.api.key:}")
    private String cloudAlyApiKey; //阿里云服务的dify api key

    @Value("${dify.api.url}")
    private String apiUrl;

    @Value("${audio.savepath:}")
    private String audioSavepath;

    @Value("${audio.flag:0}")
    private int audioFlag; //是否需要展示音频播放按钮



    private OkHttpClient client;

    @PostConstruct
    public void init() {
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort));
//        OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 设置连接超时时间为30秒
        builder.connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS);
        // 设置读取超时时间为120秒
        builder.readTimeout(600, java.util.concurrent.TimeUnit.SECONDS);
        // 设置写入超时时间为60秒
        builder.writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS);

        this.client = builder.build();
    }


    @Override
    public AIAppParameter getAIAppParameters(List<String> userInputIds) {
        AIAppParameter aiAppParameter = new AIAppParameter();
        appDescribe = ChineseStringUtil.dealChineseString(appDescribe);
        aiAppParameter.setDescribe(appDescribe);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        logger.debug("cloudAlyApiKey:"+cloudAlyApiKey);
        if(NullUtil.IsAllNotNullOfString(cloudAlyApiKey)) {
            headers.put("DifyApiKey", cloudAlyApiKey);
        }
        String api_result = "";
        try {
            api_result = HttpsClientUtil.httpGet(apiUrl + "/info", headers);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BusinessException("AI平台接口异常，获取应用基本信息失败");
        }
        if (!NullUtil.IsAllNotNullOfString(api_result)) {
            throw new BusinessException("网络错误，获取应用基本信息失败");
        }
        logger.debug("dify应用基本信息："+api_result);
        JSONObject apiResult = JSONObject.parseObject(api_result);
        aiAppParameter.setName(apiResult.getString("name"));
        aiAppParameter.setChatdescribe(appDescribe);
        if(NullUtil.IsAllNotNullOfString(apiResult.getString("description"))) {
            aiAppParameter.setChatdescribe(apiResult.getString("description"));
        }

        api_result = "";

        try {
            api_result = HttpsClientUtil.httpGet(apiUrl + "/parameters", headers);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BusinessException("AI平台接口异常，获取开场白失败");
        }
        if (!NullUtil.IsAllNotNullOfString(api_result)) {
            throw new BusinessException("网络错误，获取开场白失败");
        }
        logger.debug("dify应用开场白信息："+api_result);
        apiResult = JSONObject.parseObject(api_result);
        if("400".equals(apiResult.getString("status"))) {
            throw new BusinessException("获取开场白失败："+apiResult.getString("message"));
        }
        aiAppParameter.setOpening_statement(apiResult.getString("opening_statement"));
        apiResult.getJSONArray("suggested_questions").forEach(suggested_question -> {
            aiAppParameter.getSuggested_questions().add(suggested_question.toString());
        }
        );
        JSONArray user_input_form = apiResult.getJSONArray("user_input_form");
        List<UserInputForm> inputForms = new ArrayList<>(); // 用于存储解析后的所有对象
        aiAppParameter.setInputForms(inputForms);
        if (user_input_form != null) {
            user_input_form.forEach(user_input -> {
                JSONObject user_input_json = JSONObject.parseObject(user_input.toString());
                // 1. 获取控件类型（JSON对象中唯一的key）
                String type = user_input_json.keySet().iterator().next();
                // 2. 获取控件详细配置
                JSONObject config = user_input_json.getJSONObject(type);
                if(userInputIds.contains(config.getString("variable"))) { //如果需要用户手动输入
                    UserInputForm userInputForm = new UserInputForm();
                    userInputForm.setType(type);
                    // 3. 解析基础属性
                    userInputForm.setLabel(config.getString("label"));
                    userInputForm.setVariable(config.getString("variable"));
                    userInputForm.setRequired(config.getBoolean("required"));
                    userInputForm.setMaxLength(config.getLong("max_length"));
                    userInputForm.setDefaultValue(config.getString("default"));

                    // 4. 解析下拉框选项（仅select类型需要）
                    if ("select".equals(type)) {
                        config.getJSONArray("options").forEach(option -> {
                                    userInputForm.getOptions().add(option.toString());
                                }
                        );
                    }
                    // 5. 添加到结果列表
                    inputForms.add(userInputForm);
                }
            });
        }
        return aiAppParameter;
    }

    /**
     * 从固定格式字符串中提取音频URL
     * @param audioStr 原始字符串
     * @return 提取到的URL，无匹配则返回null
     */
    private String extractAudioUrl(String audioStr) {
        // 正则表达式：匹配(/)开头到)结尾之间的内容
        // 正则解释：
        // \\( 匹配左括号 (
        // (/.*?) 匹配以/开头的任意字符（非贪婪模式）
        // \\) 匹配右括号 )
        String regex = "\\(/(.*?)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(audioStr);

        if (matcher.find()) {
            // 分组1即为/开头的URL部分（补充/前缀）
            return "/" + matcher.group(1);
        }
        return null;
    }

    private void downloadAudioFile(String audioStr,String filename) {
        if(!NullUtil.IsAllNotNullOfString(audioSavepath)) {
            throw new BusinessException("服务端尚未配置音频文件的存储路径，请联系系统管理员");
        }
        FilePathUtils.createDirIfNotExists(audioSavepath);
        String subUrl = extractAudioUrl(audioStr);

        // 找到最后一个/的索引
        int lastSlashIndex = apiUrl.lastIndexOf("/");
        String difyRootUrl = apiUrl;
        if (lastSlashIndex != -1) {
            difyRootUrl = difyRootUrl.substring(0, lastSlashIndex);
        }
        String fullUrl = difyRootUrl+subUrl;
        try {
            byte[] audioBytes = HttpsClientUtil.getFileBytes(fullUrl, null);
            Path fullPath = Paths.get(audioSavepath, filename);
            saveAudioBytes(audioBytes,fullPath.toString());
        } catch (Exception e) {
            throw new BusinessException("下载或保存音频文件失败，请联系系统管理员");
        }
    }

    /**
     * 保存二进制数据到本地文件
     * @param audioBytes 音频二进制数组
     * @param savePath 保存路径
     * @throws Exception 文件操作异常
     */
    private void saveAudioBytes(byte[] audioBytes, String savePath) throws Exception {
        // 2.1 创建保存目录（若不存在）
        File saveFile = new File(savePath);
        File parentDir = saveFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new RuntimeException("创建保存目录失败：" + parentDir.getAbsolutePath());
        }

        // 2.2 写入二进制数据到文件
        try (OutputStream outputStream = new FileOutputStream(saveFile)) {
            outputStream.write(audioBytes);
            outputStream.flush();
        }
    }

    @Override
    public SseEmitter chat(ChatRequest chatRequest) throws Exception {
        logger.info("进入DifyAIService-chat");
        SseEmitter emitter = new SseEmitter(-1L); // 无超时
        // 创建聊天记录对象
        ChatRecord chatRecord = new ChatRecord();
        chatRecord.setConversationId(chatRequest.getConversation_id());
        chatRecord.setUserId(Long.parseLong(chatRequest.getUser()));
        chatRecord.setQuestion(chatRequest.getQuery());
        chatRecord.setCreateTime(new Date());
        final ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyStr = objectMapper.writeValueAsString(chatRequest);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),requestBodyStr);

        Request httpRequest = new Request.Builder()
                .url(apiUrl+"/chat-messages")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("DifyApiKey",cloudAlyApiKey)
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("error", "请求失败: " + e.getMessage());
                    emitter.send(JSON.toJSONString(errorJson));
                    emitter.complete();
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                    emitter.completeWithError(ex);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                chatRecord.setAnswerStartTime(new Date());
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        try {
                            JSONObject errorJson = new JSONObject();
                            errorJson.put("error", "请求AI平台失败: " + response.code());
                            errorJson.put("event","message_end");
                            logger.info("发送异常消息: " + errorJson);
                            emitter.send(JSON.toJSONString(errorJson));
                            emitter.complete();
                            return;
                        } catch (IOException ex) {
                            logger.error("发送错误消息失败", ex);
                            emitter.completeWithError(ex);
                            return;
                        }
                    }
                    StringBuilder fullAnswer = new StringBuilder();
                    StringBuilder audioAnswer = new StringBuilder();
                    boolean hasAudioFlag = false;
                    boolean audioIsEnd = false;
                    boolean echartsFlag = false;
                    try (InputStream inputStream = responseBody.byteStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                        String line;
                        boolean validAnswerFlag = false;
                        String conversation_id = "";
                        String message_id = "";
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String jsonData = line.substring(6);
                                // 解析JSON数据
                                JsonNode jsonNode = objectMapper.readTree(jsonData);
                                if (jsonNode instanceof ObjectNode) {
                                    ObjectNode objectNode = (ObjectNode) jsonNode;
                                    if(objectNode.has("event") && "message".equals(objectNode.get("event").asText())) { //chatflow返回的event是message
                                        objectNode.put("event","agent_message"); //这里统一改成agent返回的格式
                                    }

                                    if(!(objectNode.has("event") && ("agent_message".equals(objectNode.get("event").asText()) || "message_end".equals(objectNode.get("event").asText()) || "error".equals(objectNode.get("event").asText())))) {
                                        continue;
                                    }



                                    if (objectNode.has("conversation_id")) {
                                        if(!NullUtil.IsAllNotNullOfString(conversation_id)) {
                                            conversation_id = objectNode.get("conversation_id").asText();
                                        }
                                        chatRecord.setConversationId(objectNode.get("conversation_id").asText());
                                    }

                                    if (objectNode.has("message_id")) {
                                        if(!NullUtil.IsAllNotNullOfString(message_id)) {
                                            message_id = objectNode.get("message_id").asText();
                                        }
                                        chatRecord.setMessageId(objectNode.get("message_id").asText());
                                    }

                                    // 转换answer和thought字段中的Unicode为中文
                                    UnicodeUtils.convertUnicodeToChinese(objectNode, "answer");
                                    //UnicodeUtils.convertUnicodeToChinese(objectNode, "thought");

                                    //如果是回车符，且之前还没有输出过有效(非回车符)回复
                                    if(!validAnswerFlag && objectNode.has("answer") && objectNode.get("answer").asText().matches("^[\\r\\n]+$")) {
                                        logger.info("发现换行符");
                                        continue;
                                    }

                                    validAnswerFlag = true;

                                    // 收集完整的答案
                                    if (objectNode.has("answer")) {
                                        fullAnswer.append(objectNode.get("answer").asText());
                                        if(fullAnswer.toString().contains("```echarts")) { //因为```echarts有可能被分割输出，所以要用fullAnswer判断
                                            echartsFlag = true;
                                        }
                                        if((1==audioFlag) && !audioIsEnd) {
                                            if(objectNode.get("answer").asText().contains("<audio_start>")) {
                                                hasAudioFlag = true;
                                                audioAnswer.append(objectNode.get("answer").asText());
                                                continue;
                                            } else if(hasAudioFlag && !audioIsEnd && !objectNode.get("answer").asText().contains("</audio_end>")) {
                                                audioAnswer.append(objectNode.get("answer").asText());
                                                continue;
                                            }
                                            if(objectNode.get("answer").asText().contains("</audio_end>")) {
                                                audioIsEnd = true;
                                                audioAnswer.append(objectNode.get("answer").asText());
                                                logger.info("音频数据："+audioAnswer);
                                                String filename = message_id + ".wav";
                                                downloadAudioFile(audioAnswer.toString(),filename);
                                                continue;
                                            }
                                        }

                                    }


                                    if(objectNode.has("event") && objectNode.has("metadata") && "message_end".equals(objectNode.get("event").asText())) {
                                        objectNode.remove("metadata");
                                    }
                                    // 更新聊天记录信息


                                    if (objectNode.has("task_id")) {
                                        chatRecord.setTaskId(objectNode.get("task_id").asText());
                                    }

                                    if(objectNode.has("event") && "error".equals(objectNode.get("event").asText())) { //dify应用执行出现未知异常，收到异常事件后即结束
                                        logger.error("AI应用执行失败，报错："+objectNode.get("message").asText()); //把异常信息打印到日志文件
                                        try {
                                            JSONObject errorJson = new JSONObject();
                                            errorJson.put("error", "dify应用执行时发生未知异常，请尝试重新发起提问，同时建议您联系系统管理员根据dify应用的日志排查问题原因！");
                                            errorJson.put("event","message_end");
                                            logger.info("发送异常消息: " + errorJson);
                                            emitter.send(JSON.toJSONString(errorJson));
                                            emitter.complete();
                                            return;
                                        } catch (IOException ex) {
                                            logger.error("发送错误消息失败", ex);
                                            emitter.completeWithError(ex);
                                            return;
                                        }
                                    } else {
                                        // 重新序列化为JSON字符串
                                        jsonData = objectMapper.writeValueAsString(objectNode);
                                        logger.info("发送正常消息:"+jsonData);
                                        emitter.send(jsonData);
                                        if(hasAudioFlag && objectNode.has("answer") && !echartsFlag) {
                                            long delay = calculateDynamicDelay(objectNode.get("answer").asText());
                                            Thread.sleep(delay);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(),e); //把异常信息打印到日志文件
                        try {
                            JSONObject errorJson = new JSONObject();
                            errorJson.put("error", "处理响应时发生错误: " + e.getMessage());
                            errorJson.put("event","message_end");
                            logger.info("发送异常消息: " + errorJson);
                            emitter.send(JSON.toJSONString(errorJson));
                            emitter.complete();
                            return;
                        } catch (IOException ex) {
                            logger.error("发送错误消息失败", ex);
                            emitter.completeWithError(ex);
                            return;
                        }
                    }

                    // 保存完整的聊天记录
//                    logger.info("聊天记录: " + fullAnswer.toString());
                    chatRecord.setAnswer(fullAnswer.toString());
                    chatRecord.setAnswerEndTime(new Date());
                    // 保存完整聊天记录到数据库
                    try {
                        Message currentMessage = iqaService.saveChatRecord(chatRecord);
                        // 在最后一条消息中添加 zgxh 信息
                        JSONObject successJson = new JSONObject();
                        successJson.put("event", "message");
//                        successJson.put("id", chatRecord.getMessageId());
                        successJson.put("conversation_id", chatRecord.getConversationId());
                        successJson.put("message_id", chatRecord.getMessageId());
                        emitter.send(JSON.toJSONString(successJson));
                        emitter.complete();
                    } catch (Exception e) {
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("error", "保存聊天记录时发生错误: " + e.getMessage());
                        errorJson.put("event","message_end");
                        emitter.send(JSON.toJSONString(errorJson));
                        emitter.complete();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(),e); //把异常信息打印到日志文件
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("error", "处理响应时发生错误: " + e.getMessage());
                    errorJson.put("event","message_end");
                    emitter.send(JSON.toJSONString(errorJson));
                    emitter.complete();
                }
            }
        });

        // 设置超时处理器
        emitter.onTimeout(() -> {
            logger.error("SSE连接超时");
            try {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "SSE连接超时");
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(JSON.toJSONString(errorJson)));
                emitter.complete();
            } catch (IOException ex) {
                logger.error("发送超时错误消息失败", ex);
            }
        });

        // 设置完成处理器
        emitter.onCompletion(() -> {
            logger.info("SSE连接已完成");
        });

        return emitter;
    }

    /**
     * 按「单元长度+标点类型」计算动态延迟（贴近人声的核心）
     */
    private static long calculateDynamicDelay(String chunk) {
        int length = chunk.length();
        long baseDelay;

        // 1. 按标点类型确定基础延迟（句末标点停顿更长）
        if (chunk.contains("。") || chunk.contains("！") || chunk.contains("？")) {
            // 句末停顿：700-1200ms（根据长度补偿）
            baseDelay = 700 + Math.min(length * 20, 500); // 最长不超过 1200ms
        } else if (chunk.contains("，") || chunk.contains("、") || chunk.contains("；") || chunk.contains("：")) {
            // 分句停顿：300-700ms（根据长度补偿）
            baseDelay = 300 + Math.min(length * 15, 400); // 最长不超过 700ms
        } else {
            // 无标点短语：150-400ms（短短语快节奏）
            baseDelay = 150 + Math.min(length * 10, 250); // 最长不超过 400ms
        }

        // 2. 额外优化：强调内容（包含“特别”“真的”“很”等词）增加停顿
        if (chunk.contains("特别") || chunk.contains("真的") || chunk.contains("很") || chunk.contains("太")) {
            baseDelay += 200;
        }

        return baseDelay;
    }

    @Override
    public ConversationsResponse getConversations(ConversationsRequest conversationsRequest) {
        try {
            // 构建URL和查询参数
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiUrl + "/conversations").newBuilder();
            urlBuilder.addQueryParameter("user", conversationsRequest.getUser());
            if (NullUtil.IsAllNotNullOfString(conversationsRequest.getLast_id())) {
                urlBuilder.addQueryParameter("last_id", conversationsRequest.getLast_id());
            }
            urlBuilder.addQueryParameter("limit", conversationsRequest.getLimit());

            Request httpRequest = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("" + response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new BusinessException("响应体为空");
                }
                JSONObject apiResult = JSONObject.parseObject(responseBody.string());
                ConversationsResponse conversationsResponse = new ConversationsResponse();
                conversationsResponse.setLimit(apiResult.getInteger("limit"));
                conversationsResponse.setHasMore(apiResult.getBoolean("has_more"));
                apiResult.getJSONArray("data").forEach(item -> {
                    JSONObject json = (JSONObject) item;
                    ConversationVo vo = new ConversationVo();
                    vo.setId(json.getString("id"));
                    vo.setName(json.getString("name"));
                    vo.setInputs(json.getJSONObject("inputs"));
                    vo.setCreated_at(json.getLong("created_at"));
                    vo.setUpdated_at(json.getLong("updated_at"));
                    conversationsResponse.getConversations().add(vo);
                });
                return conversationsResponse;
            }
        } catch (Exception e) {
            logger.error("获取会话列表失败", e);
            throw new BusinessException("获取会话列表失败: " + e.getMessage());
        }

    }

    @Override
    public MessagesResponse getMessages(String user, String conversationId, String firstId, String limit) {
        try {
            // 构建URL和查询参数
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiUrl + "/messages").newBuilder();
            urlBuilder.addQueryParameter("user", user);
            if (NullUtil.IsAllNotNullOfString(firstId)) {
                urlBuilder.addQueryParameter("first_id", firstId);
            }
            urlBuilder.addQueryParameter("limit", limit);
            urlBuilder.addQueryParameter("conversation_id", conversationId);

            Request httpRequest = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(""+response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new BusinessException("响应体为空");
                }
                JSONObject apiResult = JSONObject.parseObject(responseBody.string());
                MessagesResponse messagesResponse = new MessagesResponse();
                messagesResponse.setLimit(apiResult.getInteger("limit"));
                messagesResponse.setHasMore(apiResult.getBoolean("has_more"));

                apiResult.getJSONArray("data").forEach(item -> {
                    JSONObject json = (JSONObject) item;
                    MessageVo vo = new MessageVo();
                    vo.setId(json.getString("id"));
                    vo.setConversation_id(json.getString("conversation_id"));
                    vo.setQuery(json.getString("query"));
                    vo.setAnswer(iqaService.cleanAnswer(json.getString("answer")));
                    vo.setInputs(json.getJSONObject("inputs"));
                    vo.setCreated_at(json.getLong("created_at"));
                    messagesResponse.getMessageVos().add(vo);
                });
                return messagesResponse;
            }
        } catch (Exception e) {
            logger.error("获取会话历史消息失败", e);
            throw new BusinessException("获取会话历史消息失败: " + e.getMessage());
        }
    }

    @Override
    public String stop(String user, String taskId) {
        try {
            String stopUrl = apiUrl + "/chat-messages/" + taskId + "/stop";
            final ObjectMapper objectMapper = new ObjectMapper();
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("user", user);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(requestBody)
            );

            Request httpRequest = new Request.Builder()
                    .url(stopUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("停止聊天时发生错误: " + response.code());
                }
                return "success";
            }
        } catch (Exception e) {
            logger.error("停止聊天时发生错误", e);
            throw new BusinessException("停止聊天时发生错误: " + e.getMessage());
        }
    }

    @Override
    public String deleteConversation(String user, String conversationId) {
        try {
            String deleteUrl = apiUrl + "/conversations/" + conversationId;
            final ObjectMapper objectMapper = new ObjectMapper();
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("user", user);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(requestBody)
            );

            Request httpRequest = new Request.Builder()
                    .url(deleteUrl)
                    .delete(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("删除会话时发生错误: " + response.code());
                }
                return "success";
            }
        } catch (Exception e) {
            logger.error("删除会话时发生错误", e);
            throw new BusinessException("删除会话时发生错误: " + e.getMessage());
        }
    }

    @Override
    public String renameConversation(String user, JSONObject params) {
        String conversation_id = params.getString("conversation_id");
        String name = params.getString("name");
        try {
            String renameUrl = apiUrl + "/conversations/" + conversation_id + "/name";
            final ObjectMapper objectMapper = new ObjectMapper();
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("user", user);
            requestBody.put("name", name);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(requestBody)
            );

            Request httpRequest = new Request.Builder()
                    .url(renameUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("重命名会话时发生错误: " + response.code());
                }
                return "success";
            }
        } catch (Exception e) {
            logger.error("重命名会话时发生错误", e);
            throw new BusinessException("重命名会话时发生错误: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestedQuestions(String user, String messageId) {
        try {
            List<String> dataList = new ArrayList<>();
            // 构建URL和查询参数
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiUrl + "/messages/"+messageId+"/suggested").newBuilder();
            urlBuilder.addQueryParameter("user", user);
            Request httpRequest = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("DifyApiKey",cloudAlyApiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("" + response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new BusinessException("响应体为空");
                }
                JSONObject apiResult = JSONObject.parseObject(responseBody.string());
                if(apiResult.containsKey("result") && "success".equals(apiResult.getString("result"))) {
                    JSONArray dataArray = apiResult.getJSONArray("data");
                    // 提取data字段并转换为List<String>
                    if (dataArray != null) {
                        for (int i = 0; i < dataArray.size(); i++) {
                            // 直接获取字符串（JSON中本身就是字符串类型）
                            String item = dataArray.getString(i);
                            dataList.add(item);
                        }
                    }
                }
                return dataList;
            }
        } catch (Exception e) {
            logger.error("获取下一步问题建议失败", e);
            throw new BusinessException("获取下一步问题建议失败: " + e.getMessage());
        }
    }
}
