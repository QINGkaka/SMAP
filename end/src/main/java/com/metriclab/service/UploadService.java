package com.metriclab.service;

import com.metriclab.model.dto.ProjectInfo;
import com.metriclab.model.dto.UploadIndex;
import com.metriclab.model.dto.UploadedFileInfo;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("zip", "java", "oom", "xml", "xmi");

    private final FileStorageService fileStorageService;
    private final ProjectService projectService;

    public UploadService(FileStorageService fileStorageService, ProjectService projectService) {
        this.fileStorageService = fileStorageService;
        this.projectService = projectService;
    }

    public synchronized List<UploadedFileInfo> listFiles(String projectId) throws IOException {
        ensureProjectExists(projectId);
        UploadIndex index = readIndex(projectId);
        return index.files().stream()
                .sorted(Comparator.comparing(UploadedFileInfo::uploadedAt).reversed())
                .toList();
    }

    public synchronized UploadedFileInfo uploadFile(String projectId, MultipartFile file) throws IOException {
        ensureProjectExists(projectId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 zip、java、oom、xml、xmi 文件");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String fileId = createFileId(now);
        String storedName = fileStorageService.storeUpload(projectId, file, fileId);
        UploadedFileInfo info = new UploadedFileInfo(
                fileId,
                originalName,
                storedName,
                extension,
                file.getSize(),
                now
        );

        UploadIndex index = readIndex(projectId);
        List<UploadedFileInfo> files = new ArrayList<>(index.files());
        files.add(info);
        fileStorageService.writeJson(fileStorageService.uploadsIndexPath(projectId), new UploadIndex(files));
        return info;
    }

    public synchronized List<UploadedFileInfo> uploadFiles(String projectId, List<MultipartFile> files) throws IOException {
        ensureProjectExists(projectId);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        List<UploadedFileInfo> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            uploaded.add(uploadFile(projectId, file));
        }
        if (uploaded.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        return uploaded;
    }

    public synchronized void deleteFile(String projectId, String fileId) throws IOException {
        ensureProjectExists(projectId);
        UploadIndex index = readIndex(projectId);
        UploadedFileInfo target = index.files().stream()
                .filter(file -> file.id().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("上传文件不存在：" + fileId));
        List<UploadedFileInfo> files = index.files().stream()
                .filter(file -> !file.id().equals(fileId))
                .toList();
        fileStorageService.deleteUpload(projectId, target.storedName());
        fileStorageService.writeJson(fileStorageService.uploadsIndexPath(projectId), new UploadIndex(files));
    }

    private UploadIndex readIndex(String projectId) throws IOException {
        if (!fileStorageService.exists(fileStorageService.uploadsIndexPath(projectId))) {
            return new UploadIndex(List.of());
        }
        UploadIndex index = fileStorageService.readJson(fileStorageService.uploadsIndexPath(projectId), UploadIndex.class);
        if (index.files() == null) {
            return new UploadIndex(List.of());
        }
        return index;
    }

    private void ensureProjectExists(String projectId) throws IOException {
        List<ProjectInfo> projects = projectService.listProjects();
        boolean exists = projects.stream().anyMatch(project -> project.id().equals(projectId));
        if (!exists) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        fileStorageService.createProjectDirectories(projectId);
    }

    private String createFileId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "file-" + timestamp + "-" + suffix;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
