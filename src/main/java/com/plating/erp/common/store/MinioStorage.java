package com.plating.erp.common.store;

import java.io.InputStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.plating.erp.common.vo.FileUploadVO;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import okhttp3.OkHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * MinIO存储实现
 * 参考官方SDK: https://min.io/docs/minio/linux/developers/java/minio-java.html
 */
@Slf4j
@Component
public class MinioStorage implements OssStorage {

    @Value("${oss.minio.endpoint:}")
    private String endpoint;

    @Value("${oss.minio.access-key:}")
    private String accessKey;

    @Value("${oss.minio.secret-key:}")
    private String secretKey;

    @Value("${oss.minio.bucket:}")
    private String bucket;

    @Value("${oss.minio.region:}")
    private String region;

    @Value("${oss.minio.domain:}")
    private String domain;

    @Value("${oss.minio.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${oss.minio.socket-timeout:30000}")
    private int socketTimeout;

    @Value("${oss.minio.secure:true}")
    private boolean secure;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            try {
                // 配置OkHttpClient以设置超时
                OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                    .build();

                // 构建MinioClient，region在创建时设置
                MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .httpClient(httpClient);
                
                minioClient = builder.build();
                
                boolean found = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(bucket).build());
                if (!found) {
                    io.minio.MakeBucketArgs.Builder makeBucketBuilder = 
                        io.minio.MakeBucketArgs.builder().bucket(bucket);
                    if (region != null && !region.isEmpty()) {
                        makeBucketBuilder.region(region);
                    }
                    minioClient.makeBucket(makeBucketBuilder.build());
                    log.info("MinIO Bucket创建成功: {}", bucket);
                }
                log.info("MinIO初始化成功, endpoint={}, bucket={}, region={}", endpoint, bucket, region);
            } catch (Exception e) {
                log.error("MinIO初始化失败", e);
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

        try (InputStream inputStream = file.getInputStream()) {
            // 构建上传参数，添加元数据
            PutObjectArgs.Builder putArgsBuilder = PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, file.getSize(), -1)
                .contentType(file.getContentType());
            
            // 添加缓存控制头
            putArgsBuilder.headers(java.util.Map.of(
                "Cache-Control", "public, max-age=31536000"
            ));
            
            minioClient.putObject(putArgsBuilder.build());
        }

        String encodedPath = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        log.info("MinIO上传成功: {} -> {}/{}", originalFilename, bucket, objectName);
        return new FileUploadVO(originalFilename, encodedPath);
    }

    @Override
    public InputStream getObjectStream(String ossPath) throws Exception {
        String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .object(decodedPath)
                .build()
        );
    }

    @Override
    public void delete(String ossPath) {
        try {
            String decodedPath = URLDecoder.decode(ossPath, StandardCharsets.UTF_8);
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(decodedPath)
                    .build()
            );
            log.info("MinIO删除成功: {}/{}", bucket, decodedPath);
        } catch (Exception e) {
            log.error("MinIO删除失败: {}", ossPath, e);
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isEmpty();
    }
}
