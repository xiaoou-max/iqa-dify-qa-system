package com.frml.api.vo.erp;

import java.util.ArrayList;
import java.util.List;

public class ErpConstant {

    public static final String table_name_summary = "cglpjgxx";

    public static final String table_name_gdgj = "cglpjgxx_gdgj"; //采购料品(管道管件)价格信息

    public static final String table_name_fm = "cglpjgxx_fm"; //采购料品(阀门)价格信息

    public static final String table_name_yb = "cglpjgxx_yb"; //采购料品(仪表)价格信息

    public static final String table_name_dqkz = "cglpjgxx_dqkz"; //采购料品(电气控制)价格信息

    public static final List<String> columns_summary = new ArrayList<>(); //表头
    static {
        columns_summary.add("lpfl"); //料品分类
        columns_summary.add("lh"); //料号
        columns_summary.add("pm"); //品名
        columns_summary.add("mc"); //名称
        columns_summary.add("ggxh"); //规格型号
        columns_summary.add("cz"); //材质
        columns_summary.add("yldj"); //压力等级
        columns_summary.add("gl"); //功率
        columns_summary.add("sj1"); //数据1
        columns_summary.add("sj2"); //数据2
        columns_summary.add("wsdj"); //未税单价  数值类型
        columns_summary.add("hsdj"); //含税单价  数值类型
        columns_summary.add("zxsl"); //执行税率  数值类型
        columns_summary.add("ywrq"); //业务日期  日期类型 yyyy-mm-dd
        columns_summary.add("pp"); // 品牌
        columns_summary.add("gys"); //供应商
    }

    public static final List<String> columns_gdgj = new ArrayList<>(); //表头_管道管件
    static {
        columns_gdgj.add("lpfl"); //料品分类
        columns_gdgj.add("mc"); //名称
        columns_gdgj.add("ggxh"); //规格型号
        columns_gdgj.add("cz"); //材质
        columns_gdgj.add("yldj"); //压力等级
        columns_gdgj.add("ljfs"); //连接方式
        columns_gdgj.add("bmcl"); //表面处理
        columns_gdgj.add("jgcc"); //结构尺寸
        columns_gdgj.add("cdbj"); //长短半径/直边
        columns_gdgj.add("jd"); //角度
        columns_gdgj.add("fllx"); //法兰类型
        columns_gdgj.add("mfmxs"); //密封面形式
        columns_gdgj.add("spgbh"); //适配管壁厚
        columns_gdgj.add("lwxs"); //螺纹形式
        columns_gdgj.add("zxbz"); //执行标准
        columns_gdgj.add("wsdj"); //未税单价  数值类型
        columns_gdgj.add("hsdj"); //含税单价  数值类型
        columns_gdgj.add("zxsl"); //执行税率  数值类型
        columns_gdgj.add("ywrq"); //业务日期  日期类型 yyyy-mm-dd
        columns_gdgj.add("pp"); // 品牌
        columns_gdgj.add("gys"); //供应商
        columns_gdgj.add("lh"); //料号
        columns_gdgj.add("pm"); //品名
    }

    public static final List<String> columns_fm = new ArrayList<>(); //表头_阀门
    static {
        columns_fm.add("lpfl"); //料品分类
        columns_fm.add("mc"); //名称
        columns_fm.add("ggxh"); //规格型号
        columns_fm.add("ljfs"); //连接方式
        columns_fm.add("mfmlx"); //密封面类型
        columns_fm.add("yldj"); //压力等级
        columns_fm.add("gzwd"); //工作温度
        columns_fm.add("zdgbyc"); //最大关闭压差
        columns_fm.add("xldj"); //泄露等级
        columns_fm.add("jz"); //介质
        columns_fm.add("ftcz"); //阀体材质
        columns_fm.add("fbxcz"); //阀板/芯材质
        columns_fm.add("mfcz"); //密封材质
        columns_fm.add("zyfs"); //作用方式
        columns_fm.add("sffb"); //是否防爆
        columns_fm.add("zxqyq"); //执行器要求
        columns_fm.add("wsdj"); //未税单价  数值类型
        columns_fm.add("hsdj"); //含税单价  数值类型
        columns_fm.add("zxsl"); //执行税率  数值类型
        columns_fm.add("ywrq"); //业务日期  日期类型 yyyy-mm-dd
        columns_fm.add("pp"); // 品牌
        columns_fm.add("gys"); //供应商
        columns_fm.add("lh"); //料号
        columns_fm.add("pm"); //品名
    }

    public static final List<String> columns_yb = new ArrayList<>(); //表头_仪表
    static {
        columns_yb.add("lpfl"); //料品分类
        columns_yb.add("mc"); //名称
        columns_yb.add("ggxh"); //规格型号
        columns_yb.add("jz"); //介质
        columns_yb.add("md"); //密度
        columns_yb.add("gzwd"); //工作温度
        columns_yb.add("gzyl"); //工作压力
        columns_yb.add("lcfw"); //量程范围
        columns_yb.add("jycz"); //接液材质
        columns_yb.add("ljxs"); //连接型式
        columns_yb.add("gddy"); //供电电压
        columns_yb.add("hartxy"); //HART协议
        columns_yb.add("jdxs"); //就地显示
        columns_yb.add("fbdj"); //防爆等级
        columns_yb.add("fhdj"); //防护等级
        columns_yb.add("wsdj"); //未税单价  数值类型
        columns_yb.add("hsdj"); //含税单价  数值类型
        columns_yb.add("zxsl"); //执行税率  数值类型
        columns_yb.add("ywrq"); //业务日期  日期类型 yyyy-mm-dd
        columns_yb.add("pp"); // 品牌
        columns_yb.add("gys"); //供应商
        columns_yb.add("lh"); //料号
        columns_yb.add("pm"); //品名
    }

    public static final List<String> columns_dqkz = new ArrayList<>(); //表头_电气控制
    static {
        columns_dqkz.add("lpfl"); //料品分类
        columns_dqkz.add("mc"); //名称
        columns_dqkz.add("ggxh"); //规格型号
        columns_dqkz.add("cz"); //材质
        columns_dqkz.add("zxbz"); //执行标准
        columns_dqkz.add("wsdj"); //未税单价  数值类型
        columns_dqkz.add("hsdj"); //含税单价  数值类型
        columns_dqkz.add("zxsl"); //执行税率  数值类型
        columns_dqkz.add("ywrq"); //业务日期  日期类型 yyyy-mm-dd
        columns_dqkz.add("pp"); // 品牌
        columns_dqkz.add("gys"); //供应商
        columns_dqkz.add("lh"); //料号
        columns_dqkz.add("pm"); //品名
    }
}
