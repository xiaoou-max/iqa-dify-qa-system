package com.frml.api.util;

import java.io.File;

/**
 * 文件路径工具类：判断路径是否存在，不存在则创建多级文件夹
 */
public class FilePathUtils {

    public static void main(String[] args) {
        // 测试目标路径（可替换为任意需要的路径，如相对路径、Linux路径）
        String targetPath = "D:/iqa/audio_files";
        // 其他测试用例（按需注释切换）
        // String targetPath = "E:/test/aaa/bbb/ccc"; // 多级缺失目录
        // String targetPath = "/home/user/data/files"; // Linux 路径
        // String targetPath = "relativeDir/subDir"; // 相对路径

        // 执行路径创建逻辑
        boolean result = createDirIfNotExists(targetPath);

        // 结果提示
        if (result) {
            System.out.println("路径已存在或创建成功：" + targetPath);
        } else {
            System.out.println("路径创建失败：" + targetPath);
        }
    }

    /**
     * 核心方法：判断路径是否存在，不存在则创建多级文件夹
     * @param path 目标文件路径（支持绝对路径、相对路径、Windows/Linux 路径）
     * @return true：路径存在或创建成功；false：创建失败（如权限不足、路径非法）
     */
    public static boolean createDirIfNotExists(String path) {
        // 1. 校验路径参数合法性
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("错误：路径不能为空！");
        }

        // 2. 将路径字符串转换为 File 对象
        File dir = new File(path);

        // 3. 判断路径是否已存在
        if (dir.exists()) {
            if (dir.isDirectory()) {
                // 路径存在且是文件夹，直接返回成功
                return true;
            } else {
                // 路径存在，但不是文件夹（是文件），冲突返回失败
                throw new RuntimeException("错误：路径已存在，但不是文件夹！路径：" + path);
            }
        }

        // 4. 路径不存在，创建多级文件夹（mkdirs() 支持创建中间缺失的目录）
        // mkdir()：仅创建最后一级目录（要求上级目录已存在）
        // mkdirs()：创建所有缺失的目录（推荐，适配多级目录场景）
        boolean createSuccess = dir.mkdirs();

        // 5. 输出创建结果日志
        if (!createSuccess) {
            throw new RuntimeException("文件夹创建失败！可能原因：权限不足、路径非法、磁盘空间不足等。路径：" + path);
        }
        return createSuccess;
    }
}
