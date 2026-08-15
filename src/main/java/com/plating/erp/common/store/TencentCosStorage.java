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
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云COS存储实现
 */
@Slf4j
@Component
public class TencentCosStorage implements OssStorage {

    @Value("${oss.tencent.secret-id:}")
    private String secretId;

    @Value("${oss.tencent.secret-key:}")
    private String secretKey;

    @Value("${oss.tencent.region:ap-guangzhou}")
    private String region;

    @Value("${oss.tencent.bucket:}")
    private String bucket;

    @Value("${oss.tencent.domain:}")
    private String domain;

    @Value("${oss.tencent.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${oss.tencent.socket-timeout:30000}")
    private int socketTimeout;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            try {
                COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
                ClientConfig clientConfig = new ClientConfig(new Region(region));
                // 设置连接超时和socket超时
                clientConfig.setConnectionTimeout(connectionTimeout);
                clientConfig.setSocketTimeout(socketTimeout);
                // 设置HTTPS协议
                clientConfig.setHttpProtocol(com.qcloud.cos.http.HttpProtocol.https);
                cosClient = new COSClient(cred, clientConfig);
                log.info("腾讯云COS初始化成功, region={}, bucket={}, domain={}", region, bucket, domain);
            } catch (Exception e) {
                log.error("腾讯云COS初始化失败", e);
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

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(bucket, objectName, inputStream, metadata);
            // 启用服务器端加密
            putRequest.setStorageClass(com.qcloud.cos.model.StorageClass.Standard);
            cosClient.putObject(putRequest);
        }

        String encodedPath = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        log.info("腾讯云COS上传成功: {} -> {}/{}", originalFilename, bucket, objectName);
        return new FileUploadVO(originalFilename, encodedPath);
    }

    @Override
    public InputStream getObjectStream(String ossPath) throws Exception {
        String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
        return cosClient.getObject(bucket, decodedPath).getObjectContent();
    }

    @Override
    public void delete(String ossPath) {
        try {
            String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
            cosClient.deleteObject(bucket, decodedPath);
            log.info("腾讯云COS删除成功: {}/{}", bucket, decodedPath);
        } catch (Exception e) {
            log.error("腾讯云COS删除失败: {}", ossPath, e);
        }
    }

    @Override
    public String getStorageType() {
        return "tencent";
    }

    public boolean isConfigured() {
        return secretId != null && !secretId.isEmpty() 
            && secretKey != null && !secretKey.isEmpty()
            && bucket != null && !bucket.isEmpty();
    }
}
