package com.plating.erp.base.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.util.FileUploadUtils;
import com.plating.erp.common.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    /**
     * 上传单个文件
     *  业务模块（如 iam/avatar、order/attachment）
     */
    @PostMapping("/upload")
    public ApiResponse<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "module", defaultValue = "common") String module) {
        try {
            FileUploadVO result = FileUploadUtils.upload(file, module);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public ApiResponse<List<FileUploadVO>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                                    @RequestParam(value = "module", defaultValue = "common") String module) {
        try {
            List<FileUploadVO> results = FileUploadUtils.uploadBatch(files, module);
            return ApiResponse.ok(results);
        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return ApiResponse.error(500, "批量文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取资源
     * 
     * @param filename 文件名（用于下载时显示）
     * @param ossPath OSS文件路径（URL编码后的路径）
     * @param action 操作类型：preview-预览/inline, download-下载
     */
    @GetMapping("/resource")
    public ResponseEntity<InputStreamResource> getResource(
            @RequestParam("filename") String filename,
            @RequestParam("ossPath") String ossPath,
            @RequestParam(value = "action", defaultValue = "preview") String action) {
        try {
            InputStream inputStream = FileUploadUtils.getObjectStream(ossPath);
            
            HttpHeaders headers = new HttpHeaders();
            
            if ("download".equals(action)) {
                // 下载模式：设置附件下载头
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", 
                    URLEncoder.encode(filename, StandardCharsets.UTF_8));
            } else {
                // 预览模式：根据文件扩展名设置Content-Type
                String contentType = resolveContentType(filename);
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setCacheControl("public, max-age=31536000");
            }

            return ResponseEntity.ok().headers(headers).body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("资源获取失败, filename={}, ossPath={}, action={}", filename, ossPath, action, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件名解析Content-Type
     */
    private String resolveContentType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".svg" -> "image/svg+xml";
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".html", ".htm" -> "text/html";
            case ".css" -> "text/css";
            case ".js" -> "application/javascript";
            case ".json" -> "application/json";
            case ".xml" -> "application/xml";
            case ".mp4" -> "video/mp4";
            case ".mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam String path) {
        try {
            FileUploadUtils.delete(path);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return ApiResponse.error(500, "文件删除失败: " + e.getMessage());
        }
    }
}
