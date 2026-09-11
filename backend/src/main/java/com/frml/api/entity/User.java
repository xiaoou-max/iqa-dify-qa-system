package com.frml.api.entity;

import com.alibaba.fastjson.JSONObject;

import javax.persistence.*;

/**
 * @Author: lixiang
 * @Description: 用户
 * @Date: 6/16/016 14:56
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //序号

    private String name; //用户的姓名

    private String code; //用户的唯一编码

    private String account; //用户的登录账号

    private String password; //用户的登录密码

    @Transient
    private String dwbms; //单位编码

    @Transient
    private String jsbms; //角色编码

    @Transient
    private JSONObject extraData;

    public JSONObject getExtraData() {
        return extraData;
    }

    public void setExtraData(JSONObject extraData) {
        this.extraData = extraData;
    }

    public String getDwbms() {
        return dwbms;
    }

    public void setDwbms(String dwbms) {
        this.dwbms = dwbms;
    }

    public String getJsbms() {
        return jsbms;
    }

    public void setJsbms(String jsbms) {
        this.jsbms = jsbms;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
