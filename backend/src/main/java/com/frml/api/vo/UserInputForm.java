package com.frml.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserInputForm {
    private String type; //控件类型
    private String label; //控件展示标签名
    private String variable; //控件id
    private boolean required; //是否必填
    private Long maxLength; //最大长度
    private String defaultValue; //默认值
    private List<String> options = new ArrayList<>(); //下拉框的选择值列表
}
