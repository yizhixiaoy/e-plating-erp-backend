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
import org.springframework.web.servlet.view.RedirectView;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
                // 预览模式：根据文件扩展名设置Content-Type，明确指示浏览器内联显示
                String contentType = resolveContentType(filename);
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setCacheControl("public, max-age=31536000");
                // Content-Disposition: inline 明确告诉浏览器内联显示而非下载
                headers.add("Content-Disposition", "inline; filename=\""
                        + URLEncoder.encode(filename != null ? filename : "file", StandardCharsets.UTF_8) + "\"");
            }

            return ResponseEntity.ok().headers(headers).body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("资源获取失败, filename={}, ossPath={}, action={}", filename, ossPath, action, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** IMM代理HTTP客户端（复用，连接池） */
    private static final HttpClient IMM_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 文档预览端点（后端代理，不暴露OSS直链）
     *
     * 流程：
     * 1. 根据文件类型判断预览策略
     * 2. IMM预览（PDF/Office等）：Java拉取IMM HTML并代理返回（覆盖Content-Disposition:attachment→inline）
     * 3. 图片/文本/其他：重定向到/resource端点（Java后端代理流式传输OSS内容）
     *
     * 为什么代理而不302重定向：
     * 阿里云IMM预览页固定返回 Content-Disposition: attachment，导致浏览器下载而非渲染。
     * Java代理可以去掉该Header，让浏览器正常渲染HTML预览页。
     */
    @GetMapping("/preview")
    public Object preview(
            @RequestParam("ossPath") String ossPath,
            @RequestParam(value = "filename", required = false) String filename) {
        try {
            String previewUrl = FileUploadUtils.getPreviewUrl(ossPath, filename);
            boolean isImm = previewUrl != null && previewUrl.startsWith("https://");
            log.info("[preview] ossPath={}, filename={}, type={}",
                    ossPath, filename, isImm ? "IMM代理" : "resource回退");

            if (isImm) {
                return proxyImmPreview(previewUrl, filename);
            }

            // 非IMM文件（图片/文本等）：重定向到/resource端点
            return new RedirectView("/api/v1/files/resource?ossPath="
                    + URLEncoder.encode(ossPath, StandardCharsets.UTF_8)
                    + "&filename=" + URLEncoder.encode(filename != null ? filename : "", StandardCharsets.UTF_8)
                    + "&action=preview");
        } catch (Exception e) {
            log.error("[preview] 文档预览失败, ossPath={}, filename={}", ossPath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 代理IMM预览内容，覆盖 Content-Disposition: attachment 为 inline
     *
     * 阿里云IMM固定返回 Content-Disposition: attachment，导致浏览器下载HTML而非渲染。
     * 此方法拉取IMM HTML后，去除该Header，浏览器正常渲染预览页。
     */
    private ResponseEntity<byte[]> proxyImmPreview(String immUrl, String filename) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(immUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = IMM_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.warn("[preview] IMM返回非200状态: {}, 回退到/resource", response.statusCode());
                return ResponseEntity.status(response.statusCode())
                        .body("IMM预览服务暂不可用".getBytes(StandardCharsets.UTF_8));
            }

            HttpHeaders headers = new HttpHeaders();
            // 保留IMM返回的Content-Type（通常是text/html）
            response.headers().firstValue("Content-Type")
                    .ifPresent(ct -> headers.setContentType(MediaType.parseMediaType(ct)));
            // 关键：覆盖IMM的 Content-Disposition: attachment → inline
            String safeName = filename != null ? filename : "preview";
            headers.add("Content-Disposition", "inline; filename=\""
                    + URLEncoder.encode(safeName, StandardCharsets.UTF_8) + "\"");
            headers.setCacheControl("no-cache");

            log.info("[preview] IMM代理成功, Content-Type={}, body={}bytes",
                    headers.getContentType(), response.body().length);
            return ResponseEntity.ok().headers(headers).body(response.body());
        } catch (Exception e) {
            log.error("[preview] IMM代理失败, url={}", immUrl, e);
            return ResponseEntity.status(502)
                    .body("文档预览服务暂不可用".getBytes(StandardCharsets.UTF_8));
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
