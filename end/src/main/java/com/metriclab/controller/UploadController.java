package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.UploadedFileInfo;
import com.metriclab.service.UploadService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @GetMapping
    public ApiResponse<List<UploadedFileInfo>> listFiles(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(uploadService.listFiles(projectId));
    }

    @PostMapping
    public ApiResponse<List<UploadedFileInfo>> uploadFile(
            @PathVariable String projectId,
            @RequestParam("file") List<MultipartFile> files
    ) throws IOException {
        List<UploadedFileInfo> uploadedFiles = uploadService.uploadFiles(projectId, files);
        return ApiResponse.ok("文件上传成功", uploadedFiles);
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(@PathVariable String projectId, @PathVariable String fileId) throws IOException {
        uploadService.deleteFile(projectId, fileId);
        return ApiResponse.ok("上传文件删除成功", null);
    }
}
