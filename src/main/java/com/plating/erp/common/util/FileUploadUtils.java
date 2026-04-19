package com.plating.erp.common.util;

import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.plating.erp.common.store.OssStorageFactory;
import com.plating.erp.common.vo.FileUploadVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传工具类
 * 通过OssStorageFactory自动切换存储实现
 * 
 * 文件存储路径规范: {module}/{yyyy}_{MM}_{dd}/{uuid}.{ext}
 * - module: 业务模块名称，如 iam、order、product 等
 * - yyyy_MM_dd: 年月日目录结构
 * - uuid: 唯一文件名
 */
@Slf4j
public class FileUploadUtils {

    /**
     * 上传单个文件
     * 
     * @param file 文件
     * @param module 业务模块名称（如 iam、order、product）
     * @return 上传结果
     * 
     * 示例:
     * - 头像上传: upload(avatarFile, "iam/avatar") -> iam/avatar/2026_04_17/uuid.jpg
     * - 订单附件: upload(orderFile, "order/attachment") -> order/attachment/2026_04_17/uuid.pdf
     * - 产品图片: upload(productImg, "product/image") -> product/image/2026_04_17/uuid.png
     */
    public static FileUploadVO upload(MultipartFile file, String module) throws Exception {
        return OssStorageFactory.getStorage().upload(file, module);
    }

    /**
     * 批量上传文件
     * 
     * @param files 文件列表
     * @param module 业务模块名称
     * @return 上传结果列表
     */
    public static List<FileUploadVO> uploadBatch(List<MultipartFile> files, String module) throws Exception {
        List<FileUploadVO> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            results.add(upload(file, module));
        }
        return results;
    }

    /**
     * 获取文件流
     */
    public static InputStream getObjectStream(String ossPath) throws Exception {
        return OssStorageFactory.getStorage().getObjectStream(ossPath);
    }

    /**
     * 删除文件
     */
    public static void delete(String ossPath) {
        OssStorageFactory.getStorage().delete(ossPath);
    }
}
