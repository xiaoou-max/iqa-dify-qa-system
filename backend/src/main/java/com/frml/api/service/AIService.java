package com.frml.api.service;

import com.alibaba.fastjson.JSONObject;
import com.frml.api.vo.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI应用核心接口
 */
public interface AIService {

    /**
     * 1. 获取AI应用的基本参数，例如预设问题
     * @return
     */
    AIAppParameter getAIAppParameters(List<String> userInputIds);

    /**
     * 2. 发送对话消息给AI应用
     * @param chatRequest
     * @return
     * @throws Exception
     */
    SseEmitter chat(ChatRequest chatRequest) throws Exception;

    /**
     * 3. 获取历史会话列表
     * @param conversationsRequest
     * @return
     */
    ConversationsResponse getConversations(ConversationsRequest conversationsRequest);

    /**
     * 4. 获取某个会话的历史聊天记录
     * @param user
     * @param conversationId
     * @param firstId
     * @param limit
     * @return
     */
    MessagesResponse getMessages(String user, String conversationId, String firstId, String limit);

    /**
     * 5. 让AI应用停止回复
     * @param user
     * @param taskId
     * @return
     */
    String stop(String user, String taskId);

    /**
     * 6. 获取下一轮建议问题列表
     * @param user
     * @param messageId
     * @return
     */
    List<String> getSuggestedQuestions(String user, String messageId);

    /**
     * 7. 删除会话
     * @param user
     * @param conversationId
     * @return
     */
    String deleteConversation(String user, String conversationId);

    /**
     * 8. 会话重命名
     * @param user
     * @param params
     * @return
     */
    String renameConversation(String user, JSONObject params);
}
