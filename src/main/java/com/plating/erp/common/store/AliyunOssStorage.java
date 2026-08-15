package com.plating.erp.common.store;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.plating.erp.common.vo.FileUploadVO;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云OSS存储实现
 */
@Slf4j
@Component
public class AliyunOssStorage implements OssStorage {

    @Value("${oss.aliyun.endpoint:}")
    private String endpoint;

    @Value("${oss.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${oss.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${oss.aliyun.bucket:}")
    private String bucket;

    @Value("${oss.aliyun.domain:}")
    private String domain;

    @Value("${oss.aliyun.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${oss.aliyun.socket-timeout:30000}")
    private int socketTimeout;

    private OSS ossClient;

    /** IMM文档预览支持的文件格式（与阿里云官方文档一致） */
    private static final Set<String> IMM_PREVIEW_EXTENSIONS = Set.of(
            "et", "xls", "xlt", "xlsx", "xlsm", "xltx", "xltm", "csv",
            "doc", "docx", "txt", "dot", "wps", "wpt", "dotx", "docm", "dotm", "rtf",
            "ppt", "pptx", "pptm", "ppsx", "ppsm", "pps", "potx", "potm", "dpt", "dps",
            "pdf"
    );

    /** IMM预览URL签名参数 */
    private static final String IMM_PREVIEW_PROCESS = "imm/previewdoc,copy_1";

    /** IMM预览URL默认有效期（秒） */
    private static final long IMM_PREVIEW_EXPIRE_MS = 3600L * 1000L;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            try {
                com.aliyun.oss.ClientBuilderConfiguration conf = new com.aliyun.oss.ClientBuilderConfiguration();
                // 设置连接超时
                conf.setConnectionTimeout(connectionTimeout);
                // 设置socket超时
                conf.setSocketTimeout(socketTimeout);
                // 设置请求协议为HTTPS
                conf.setProtocol(com.aliyun.oss.common.comm.Protocol.HTTPS);
                // 设置最大重试次数
                conf.setMaxErrorRetry(3);
                
                ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
                log.info("阿里云OSS初始化成功, endpoint={}, bucket={}, domain={}", endpoint, bucket, domain);
            } catch (Exception e) {
                log.error("阿里云OSS初始化失败", e);
            }
        }
    }

    @Override
    public FileUploadVO upload(MultipartFile file, String module) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成路径: {module}/{yyyy}_{MM}_{dd}/{uuid}.{ext}
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = String.format("%s/%s/%s%s", module, datePath, uuid, extension);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        // 设置缓存控制
        metadata.setCacheControl("public, max-age=31536000");
        // 设置服务器端加密
        metadata.setHeader("x-oss-server-side-encryption", "AES256");

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucket, objectName, inputStream, metadata);
        }

        String encodedPath = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        log.info("阿里云OSS上传成功: {} -> {}/{}", originalFilename, bucket, objectName);
        return new FileUploadVO(originalFilename, encodedPath);
    }

    @Override
    public InputStream getObjectStream(String ossPath) throws Exception {
        String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
        return ossClient.getObject(bucket, decodedPath).getObjectContent();
    }

    /**
     * 检查文件是否支持IMM文档预览
     * @param filename 文件名
     * @return true=支持IMM预览
     */
    public boolean isImmPreviewable(String filename) {
        if (filename == null || !filename.contains(".")) return false;
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return IMM_PREVIEW_EXTENSIONS.contains(ext);
    }

    /**
     * 生成IMM文档预览签名URL（用于PDF、Office等文档在线预览）
     *
     * 原理：使用阿里云IMM（智能媒体管理）的 imm/previewdoc 功能，
     * 生成带有 x-oss-process=imm/previewdoc,copy_1 参数的签名URL，
     * OSS+IMM负责在浏览器中渲染文档。
     *
     * 前提条件：Bucket已绑定IMM Project（在OSS控制台操作）
     *
     * @param ossPath       OSS路径（URL编码）
     * @param expireSeconds URL有效期（秒），默认3600秒
     * @return 签名预览URL；如果文件不支持IMM预览则返回null
     */
    public String generatePreviewUrl(String ossPath, int expireSeconds) {
        if (ossPath == null || ossPath.isEmpty() || ossClient == null) return null;

        String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
        String filename = decodedPath.contains("/") ? decodedPath.substring(decodedPath.lastIndexOf("/") + 1) : decodedPath;

        if (!isImmPreviewable(filename)) return null;

        try {
            Date expiration = new Date(System.currentTimeMillis() + (long) expireSeconds * 1000L);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    bucket, decodedPath, HttpMethod.GET);
            req.setExpiration(expiration);
            req.setProcess(IMM_PREVIEW_PROCESS);
            URL signedUrl = ossClient.generatePresignedUrl(req);
            log.debug("[OSS/Aliyun] 生成IMM预览URL: {} (expire={}s)", filename, expireSeconds);
            return signedUrl.toString();
        } catch (Exception e) {
            log.warn("[OSS/Aliyun] 生成IMM预览URL失败: {}, err={}", filename, e.getMessage());
            return null;
        }
    }

    /**
     * 生成IMM文档预览签名URL（默认有效期3600秒）
     */
    public String generatePreviewUrl(String ossPath) {
        return generatePreviewUrl(ossPath, 3600);
    }

    @Override
    public void delete(String ossPath) {
        try {
            String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
            ossClient.deleteObject(bucket, decodedPath);
            log.info("阿里云OSS删除成功: {}/{}", bucket, decodedPath);
        } catch (Exception e) {
            log.error("阿里云OSS删除失败: {}", ossPath, e);
        }
    }

    @Override
    public String getStorageType() {
        return "aliyun";
    }

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isEmpty() 
            && accessKeyId != null && !accessKeyId.isEmpty()
            && accessKeySecret != null && !accessKeySecret.isEmpty()
            && bucket != null && !bucket.isEmpty();
    }
}
