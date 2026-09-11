package com.frml.api.util;

/**
 * 该类是用于验证的工具
 */
public class VerifyUtils {
    //登录类型的值
    public static String[] LOGIN_FROM_VALUES = {"Mobile","PC","dsf"};
    public static String LOGIN_FROM_APP = "Mobile";
    public static String LOGIN_FROM_PC = "PC";
    public static String LOGIN_FROM_DSF = "dsf";


    /**
     * 验证是否属于登录类型的值
     **/
    public static boolean isLoginFrom(String _from) {
        if(_from==null){
            return false;
        }

        for (String loginFromValue : LOGIN_FROM_VALUES) {
            if (loginFromValue.equals(_from)) {
                return true;
            }
        }

        return false;
    }
}
