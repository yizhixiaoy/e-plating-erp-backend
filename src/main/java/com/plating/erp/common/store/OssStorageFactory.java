package com.plating.erp.common.store;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OSS存储工厂
 * 根据配置自动选择存储实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssStorageFactory {

    @Value("${oss.provider:tencent}")
    private String provider;

    private final List<OssStorage> storages;

    private static OssStorage currentStorage;

    @PostConstruct
    public void init() {
        // 按优先级选择：配置项 > 第一个可用的
        for (OssStorage storage : storages) {
            if (storage.getStorageType().equalsIgnoreCase(provider)) {
                if (isAvailable(storage)) {
                    currentStorage = storage;
                    log.info("OSS存储初始化成功: {}", provider);
                    return;
                }
            }
        }
        
        // 回退到第一个可用的
        for (OssStorage storage : storages) {
            if (isAvailable(storage)) {
                currentStorage = storage;
                log.info("OSS存储回退到: {}", storage.getStorageType());
                return;
            }
        }
        
        log.error("没有可用的OSS存储实现");
    }

    private boolean isAvailable(OssStorage storage) {
        if (storage instanceof MinioStorage) {
            return ((MinioStorage) storage).isConfigured();
        } else if (storage instanceof TencentCosStorage) {
            return ((TencentCosStorage) storage).isConfigured();
        } else if (storage instanceof AliyunOssStorage) {
            return ((AliyunOssStorage) storage).isConfigured();
        }
        return false;
    }

    public static OssStorage getStorage() {
        if (currentStorage == null) {
            throw new RuntimeException("OSS存储未初始化");
        }
        return currentStorage;
    }
}
