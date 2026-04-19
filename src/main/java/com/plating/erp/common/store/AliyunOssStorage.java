package com.plating.erp.common.store;

import java.io.InputStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.plating.erp.common.vo.FileUploadVO;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
