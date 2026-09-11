package com.frml.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChatRecord {
    private String messageId;
    private String conversationId;
    private String taskId;
    private Long userId;
    private String question;
    private String answer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date answerStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date answerEndTime;
}
