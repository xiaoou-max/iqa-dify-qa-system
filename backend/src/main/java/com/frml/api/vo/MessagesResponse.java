package com.frml.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取会话历史的响应
 */
@Data
public class MessagesResponse {
    private int limit; //返回条数，若传入超过系统限制，返回系统限制数量
    private boolean hasMore; //是否有下一页
    private List<MessageVo> messageVos = new ArrayList<>(); //会话列表
}
