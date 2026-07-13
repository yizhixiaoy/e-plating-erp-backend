package com.plating.erp.common.util;

import com.plating.erp.common.store.AliyunOssStorage;
import com.plating.erp.common.store.OssStorageFactory;
import com.plating.erp.common.store.OssStorage;
import com.plating.erp.common.vo.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
     * 资源访问接口基础路径（与前端约定一致）
     */
    private static final String RESOURCE_BASE_PATH = "/api/v1/files/resource";

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

    /**
     * 获取文档预览URL（IMM签名URL）
     *
     * 支持IMM预览的文件（PDF/Office等）：返回带签名的IMM预览URL（有效期3600秒）
     * 不支持IMM的文件（图片等）：回退到普通资源URL
     *
     * @param ossPath  OSS路径（URL编码）
     * @param filename 原始文件名（用于判断文件类型和下载时显示）
     * @return 预览URL
     */
    public static String getPreviewUrl(String ossPath, String filename) {
        if (ossPath == null || ossPath.isEmpty()) return "";

        try {
            OssStorage storage = OssStorageFactory.getStorage();
            if (storage instanceof AliyunOssStorage aliyunStorage) {
                String previewUrl = aliyunStorage.generatePreviewUrl(ossPath);
                if (previewUrl != null) {
                    return previewUrl;
                }
                // 文件不支持IMM预览，回退到普通资源URL
            }
        } catch (Exception e) {
            log.warn("生成IMM预览URL失败，回退到普通资源URL: {}", e.getMessage());
        }

        // 回退到普通资源访问URL
        return getResourceUrl(ossPath, filename, "preview");
    }

    /**
     * 获取文档预览URL（默认从ossPath提取文件名）
     */
    public static String getPreviewUrl(String ossPath) {
        return getPreviewUrl(ossPath, null);
    }

    /**
     * 获取资源访问完整URL（用于前端直接展示/下载）
     *
     * @param ossPath      OSS相对路径（已URL编码）
     * @param filename     原始文件名（用于下载时显示，也用于判断Content-Type）
     * @param action       操作类型：preview-预览, download-下载
     * @return 完整的资源访问URL，如 /api/v1/files/resource?filename=xxx&ossPath=xxx&action=preview
     */
    public static String getResourceUrl(String ossPath, String filename, String action) {
        if (ossPath == null || ossPath.isEmpty()) {
            return "";
        }
        String actualFilename = (filename != null && !filename.isEmpty()) ? filename : extractFilenameFromOssPath(ossPath);
        return RESOURCE_BASE_PATH + "?filename=" + java.net.URLEncoder.encode(actualFilename, StandardCharsets.UTF_8)
                + "&ossPath=" + ossPath + "&action=" + action;
    }

    /**
     * 获取资源预览URL（支持传入原始文件名）
     *
     * @param ossPath  OSS相对路径
     * @param filename 原始文件名
     * @return 完整的预览URL
     */
    public static String getResourceUrl(String ossPath, String filename) {
        return getResourceUrl(ossPath, filename, "preview");
    }

    /**
     * 获取资源预览URL（默认preview，从OSS路径提取文件名）
     *
     * @param ossPath OSS相对路径
     * @return 完整的预览URL
     */
    public static String getResourceUrl(String ossPath) {
        return getResourceUrl(ossPath, null, "preview");
    }

    /**
     * 获取资源下载URL（支持传入原始文件名）
     *
     * @param ossPath  OSS相对路径
     * @param filename 原始文件名
     * @return 完整的下载URL
     */
    public static String getDownloadUrl(String ossPath, String filename) {
        return getResourceUrl(ossPath, filename, "download");
    }

    /**
     * 获取资源下载URL（从OSS路径提取文件名）
     *
     * @param ossPath OSS相对路径
     * @return 完整的下载URL
     */
    public static String getDownloadUrl(String ossPath) {
        return getResourceUrl(ossPath, null, "download");
    }

    /**
     * 从完整资源URL中提取OSS相对路径
     * 用于后端接收前端回传URL时还原为存储路径
     *
     * @param url 完整URL或OSS路径
     * @return OSS相对路径；如果输入本身就是OSS路径则原样返回
     */
    public static String extractOssPath(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // 如果不是以资源接口路径开头，说明本身就是OSS路径
        if (!url.contains(RESOURCE_BASE_PATH)) {
            return url;
        }
        try {
            // 提取 ossPath 参数值
            int idx = url.indexOf("ossPath=");
            if (idx == -1) {
                return url;
            }
            String value = url.substring(idx + "ossPath=".length());
            // 截断后续参数
            int ampIdx = value.indexOf("&");
            if (ampIdx != -1) {
                value = value.substring(0, ampIdx);
            }
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("从URL提取OSS路径失败: {}", url, e);
            return url;
        }
    }

    /**
     * 从OSS路径中提取文件名
     */
    private static String extractFilenameFromOssPath(String ossPath) {
        if (ossPath == null || ossPath.isEmpty()) {
            return "";
        }
        try {
            String decoded = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
            int lastSlash = decoded.lastIndexOf("/");
            return lastSlash >= 0 ? decoded.substring(lastSlash + 1) : decoded;
        } catch (Exception e) {
            int lastSlash = ossPath.lastIndexOf("/");
            return lastSlash >= 0 ? ossPath.substring(lastSlash + 1) : ossPath;
        }
    }
}
