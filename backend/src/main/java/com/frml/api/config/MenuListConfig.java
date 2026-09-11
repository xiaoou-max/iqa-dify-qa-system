package com.frml.api.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frml.api.util.NullUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 绑定 properties 中以 "users" 为前缀的配置
@Component
@ConfigurationProperties(prefix = "menus")
public class MenuListConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // List 字段名需与 properties 中的 "userList" 一致
    private List<Menu> menuList = new ArrayList<>();

    public List<Menu> getMenuList() {
        return menuList;
    }

    public void setMenuList(List<Menu> menuList) {
        this.menuList = menuList;
    }

    // 内部类：User（对应列表中的每个 JSON 对象）
    public static class Menu {

        private static final Logger logger = LoggerFactory.getLogger(Menu.class);
        private String name; //要展示的菜单名称
        @JsonIgnore
        private int sort; //展示顺序
        private String route; //对应前端页面的路由
        @JsonIgnore
        private String roleUids; //能够看见此菜单的角色唯一编号，多个用逗号隔开，例如：a,b
        @JsonIgnore
        private List<String> roleUidList = new ArrayList<>();
        public String getName() {
            return name;
        }

        public void setName(String name) {
            // 避免 null 导致空指针
            if (name == null) {
                this.name = null;
                return;
            }
            logger.info("原始菜单名称："+name);
            try {
                // 关键：判断原始字符串是否能通过 ISO-8859-1 → UTF-8 还原为有效中文
                // 逻辑：将字符串按 ISO-8859-1 转字节，再按 UTF-8 解码，若解码后与原字符串不同，说明是乱码需修复
                String decodedName = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                // 额外校验：解码后的字符串是否包含有效中文（UTF-8 中文的字节范围是 0xE4~0xED 等）
                if (isUtf8Chinese(decodedName) && !decodedName.equals(name)) {
                    this.name = decodedName;
                    logger.info("Linux 环境：编码修复后菜单名称：{}", decodedName);
                } else {
                    // Windows 环境：无需转码，直接使用原始值
                    this.name = name;
                    logger.info("Windows 环境：直接使用原始菜单名称：{}", name);
                }
            } catch (Exception e) {
                // 转码失败时使用原始值，避免问号乱码
                this.name = name;
                logger.error("菜单名称编码处理失败：", e);
            }
        }

        // 辅助方法：判断字符串是否包含 UTF-8 中文（避免误转码纯英文/数字）
        private boolean isUtf8Chinese(String str) {
            for (char c : str.toCharArray()) {
                // UTF-8 中文的 Unicode 范围：0x4E00（一）~0x9FA5（龥）
                if (c >= 0x4E00 && c <= 0x9FA5) {
                    return true;
                }
            }
            return false;
        }

        public int getSort() {
            return sort;
        }

        public void setSort(int sort) {
            this.sort = sort;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        public String getRoleUids() {
            return roleUids;
        }

        public void setRoleUids(String roleUids) {
            this.roleUids = roleUids;
            if(NullUtil.IsAllNotNullOfString(roleUids)) {
                String[] array = roleUids.split(",");
                this.roleUidList = new ArrayList<>(Arrays.asList(array));
            }
        }

        public List<String> getRoleUidList() {
            return roleUidList;
        }
    }
}
