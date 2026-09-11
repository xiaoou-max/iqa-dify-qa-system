package com.frml.api.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 消息记录
 */
@Data
public class MessageVo {
    private String id; //消息id
    private String conversation_id; //会话id
    private String query; //用户输入 / 提问内容
    private JSONObject inputs = new JSONObject(); //用户输入参数
    private String answer; //回答消息内容
    private Long created_at; //创建时间
}
