package com.plating.erp.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {
    
    /**
     * 原始文件名
     */
    private String originalFilename;
    
    /**
     * OSS相对路径
     */
    private String ossPath;
}
