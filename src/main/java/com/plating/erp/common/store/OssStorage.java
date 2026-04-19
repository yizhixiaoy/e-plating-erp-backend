package com.plating.erp.common.store;

import java.io.InputStream;

import com.plating.erp.common.vo.FileUploadVO;

import org.springframework.web.multipart.MultipartFile;

/**
 * OSS存储接口
 * 支持MinIO、阿里云OSS、腾讯云COS等多种实现
 * 
 * 文件存储路径规范: {module}/{yyyy}_{MM}_{dd}/{uuid}.{ext}
 * - module: 业务模块名称，如 iam、order、product 等
 * - yyyy_MM_dd: 年月日目录结构
 * - uuid: 唯一文件名
 */
public interface OssStorage {

    /**
     * 上传文件
     * @param file 文件
     * @param module 业务模块名称（如 iam、order、product）
     * @return 上传结果
     */
    FileUploadVO upload(MultipartFile file, String module) throws Exception;

    /**
     * 上传文件（兼容旧版本，使用prefix作为模块名）
     * @param file 文件
     * @param prefix 前缀/模块名
     * @param useDatePath 是否使用日期路径（true: 使用 yyyy/MM/dd 格式）
     * @return 上传结果
     */
    default FileUploadVO upload(MultipartFile file, String prefix, boolean useDatePath) throws Exception {
        if (useDatePath) {
            return upload(file, prefix);
        } else {
            // 不使用日期路径的兼容处理
            return upload(file, prefix);
        }
    }

    /**
     * 获取文件流
     * @param ossPath OSS路径
     * @return 文件流
     */
    InputStream getObjectStream(String ossPath) throws Exception;

    /**
     * 删除文件
     * @param ossPath OSS路径
     */
    void delete(String ossPath);

    /**
     * 获取存储类型
     * @return 存储类型标识
     */
    String getStorageType();
}
