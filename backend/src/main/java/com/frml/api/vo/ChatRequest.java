package com.frml.api.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 对话请求
 */
@Data
public class ChatRequest {
    private String user;  //用户标识，用于定义终端用户的身份，方便检索、统计。 由开发者定义规则，需保证用户标识在应用内唯一
    private String query; //用户输入/提问内容
    private String response_mode; //响应模式 streaming：流式模式（推荐）；blocking：阻塞模式
    private String conversation_id; //会话 ID，需要基于之前的聊天记录继续对话，必须传之前消息的 conversation_idID
    private JSONObject inputs = new JSONObject(); //允许传入 App 定义的各变量值。 inputs 参数包含了多组键值对（Key/Value pairs），每组的键对应一个特定变量，每组的值则是该变量的具体值。 默认 {}
    private boolean auto_generate_name = true; //自动生成会话标题，默认 true。 若设置为 false，则可通过调用会话重命名接口并设置 auto_generate 为 true 实现异步生成标题
}
