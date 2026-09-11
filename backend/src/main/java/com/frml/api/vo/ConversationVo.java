package com.frml.api.vo;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 会话记录
 */
@Data
public class ConversationVo {
    private String id; //会话id
    private String name; //会话名称，默认为会话中用户最开始问题的截取
    private JSONObject inputs; //用户输入参数
    private Long created_at; //创建时间
    private Long updated_at; //更新时间
}
