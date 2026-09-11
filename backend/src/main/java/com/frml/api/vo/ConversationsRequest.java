package com.frml.api.vo;

import lombok.Data;

@Data
public class ConversationsRequest {
    private String user;  //用户标识，用于定义终端用户的身份，方便检索、统计。 由开发者定义规则，需保证用户标识在应用内唯一
    private String limit; //返回条数，若传入超过系统限制，返回系统限制数量  dify:一次请求返回多少条记录，默认 20 条，最大 100 条，最小 1 条
    private String last_id; //上一次请求返回的最后一条记录的 id，第一次请求时可不传
}
