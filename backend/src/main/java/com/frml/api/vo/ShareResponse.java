package com.frml.api.vo;

import lombok.Data;

import java.util.List;

@Data
public class ShareResponse {
    private List<MessageVo> messages;
}
