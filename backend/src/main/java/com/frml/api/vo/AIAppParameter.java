package com.frml.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI应用的参数配置
 */
@Data
public class AIAppParameter {
    private String name; // 应用名称
    private String describe; //子标题
    private String opening_statement; //开场白
    private String chatdescribe; //对话页面的描述信息
    private List<String> suggested_questions = new ArrayList<>(); //开场推荐问题列表
    private List<UserInputForm> inputForms = new ArrayList<>(); //用户输入表单配置
}
