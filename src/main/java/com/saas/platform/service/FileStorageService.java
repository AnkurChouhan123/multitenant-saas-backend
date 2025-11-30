package com.saas.platform.service;

import com.saas.platform.model.FileStorage;
import com.saas.platform.repository.FileStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FileStorageService - Handles file upload, download, and management
 */
@Service
public class FileStorageService {
    
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${file.max.size:10485760}") // 10MB default
    private Long maxFileSize;
    
    private final FileStorageRepository fileStorageRepository;
    private final ActivityLogService activityLogService;

    
    public FileStorageService(FileStorageRepository fileStorageRepository,
                            ActivityLogService activityLogService
                           ) {
        this.fileStorageRepository = fileStorageRepository;
        this.activityLogService = activityLogService;

    }
    
    /**
     * Upload a file
     */
    @Transactional
    public FileStorage uploadFile(MultipartFile file, Long tenantId, Long userId,
                                  String description, String category) throws IOException {
        log.info("Uploading file: {} for tenant: {}", file.getOriginalFilename(), tenantId);
        
        // Validate file
        validateFile(file);
        
        // Check storage quota
        checkStorageQuota(tenantId, file.getSize());
        
        // Create upload directory if not exists
        Path uploadPath = createUploadDirectory(tenantId);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Save file to disk
        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Calculate checksum
        String checksum = calculateChecksum(filePath);
        
        // Create file record
        FileStorage fileStorage = new FileStorage(
            tenantId, userId, originalFilename, storedFilename,
            filePath.toString(), file.getSize(), file.getContentType(), fileExtension
        );
        
        fileStorage.setDescription(description);
        fileStorage.setCategory(category);
        fileStorage.setChecksum(checksum);
        
        FileStorage saved = fileStorageRepository.save(fileStorage);
        
        // Log activity
        activityLogService.logActivity(
            tenantId, userId, "system", "System",
            "File uploaded: " + originalFilename,
            "data",
            String.format("Size: %s, Type: %s", saved.getFileSizeFormatted(), fileExtension)
        );
        
        log.info("File uploaded successfully with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Download a file
     */
    public Resource downloadFile(Long fileId, Long userId) throws IOException {
        log.info("Downloading file ID: {} by user: {}", fileId, userId);
        
        FileStorage file = getFileById(fileId);
        
        // Check if file is deleted
        if (file.isDeleted()) {
            throw new IllegalArgumentException("File has been deleted");
        }
        
        // Check if file is expired
        if (file.isExpired()) {
            throw new IllegalArgumentException("File has expired");
        }
        
        Path filePath = Paths.get(file.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("File not found or not readable: " + file.getOriginalFilename());
        }
        
        // Increment download count
        file.incrementDownloadCount();
        fileStorageRepository.save(file);
        
        log.info("File downloaded: {}", file.getOriginalFilename());
        
        return resource;
    }
    
    /**
     * Get all files for a tenant
     */
    public List<FileStorage> getFilesByTenant(Long tenantId) {
        return fileStorageRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);
    }
    
    /**
     * Get files uploaded by a user
     */
    public List<FileStorage> getFilesByUser(Long userId) {
        return fileStorageRepository.findByUploadedByAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get files by category
     */
    public List<FileStorage> getFilesByCategory(Long tenantId, String category) {
        return fileStorageRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId, category);
    }
    
    /**
     * Search files by name
     */
    public List<FileStorage> searchFiles(Long tenantId, String keyword) {
        return fileStorageRepository.findByTenantIdAndOriginalFilenameContainingIgnoreCaseAndDeletedAtIsNull(
            tenantId, keyword);
    }
    
    /**
     * Update file metadata
     */
    @Transactional
    public FileStorage updateFileMetadata(Long fileId, String description,
                                         String category, String tags) {
        FileStorage file = getFileById(fileId);
        
        if (description != null) file.setDescription(description);
        if (category != null) file.setCategory(category);
        if (tags != null) file.setTags(tags);
        
        return fileStorageRepository.save(file);
    }
    
    /**
     * Share file with users
     */
    @Transactional
    public FileStorage shareFile(Long fileId, List<Long> userIds) {
        FileStorage file = getFileById(fileId);
        
        String sharedWith = userIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        
        file.setSharedWith(sharedWith);
        
        FileStorage saved = fileStorageRepository.save(file);
      
        
        return saved;
    }
    
    /**
     * Soft delete a file
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        log.info("Deleting file ID: {} by user: {}", fileId, userId);
        
        FileStorage file = getFileById(fileId);
        file.softDelete();
        
        fileStorageRepository.save(file);
        
        // Log activity
        activityLogService.logActivity(
            file.getTenantId(), userId, "system", "System",
            "File deleted: " + file.getOriginalFilename(),
            "data",
            "File moved to trash"
        );
        
        log.info("File deleted successfully");
    }
    
    /**
     * Permanently delete a file
     */
    @Transactional
    public void permanentlyDeleteFile(Long fileId) throws IOException {
        log.info("Permanently deleting file ID: {}", fileId);
        
        FileStorage file = getFileById(fileId);
        
        // Delete physical file
        Path filePath = Paths.get(file.getFilePath());
        Files.deleteIfExists(filePath);
        
        // Delete database record
        fileStorageRepository.delete(file);
        
        log.info("File permanently deleted");
    }
    
    /**
     * Restore deleted file
     */
    @Transactional
    public FileStorage restoreFile(Long fileId) {
        FileStorage file = getFileById(fileId);
        
        if (!file.isDeleted()) {
            throw new IllegalArgumentException("File is not deleted");
        }
        
        file.setDeletedAt(null);
        return fileStorageRepository.save(file);
    }
    
    /**
     * Get total storage used by tenant
     */
    public Long getTotalStorageUsed(Long tenantId) {
        return fileStorageRepository.sumFileSizeByTenantId(tenantId);
    }
    
    /**
     * Get file by ID
     */
    public FileStorage getFileById(Long id) {
        return fileStorageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + id));
    }
    
    /**
     * Get recent files
     */
    public List<FileStorage> getRecentFiles(Long tenantId, int limit) {
        return fileStorageRepository.findTop10ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);
    }
    
    // Private helper methods
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                "File size exceeds maximum allowed size: " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }
    
    private void checkStorageQuota(Long tenantId, Long fileSize) {
        Long currentStorage = getTotalStorageUsed(tenantId);
        Long maxStorage = 1024L * 1024 * 1024 * 10; // 10GB default
        
        if (currentStorage + fileSize > maxStorage) {
            throw new IllegalArgumentException("Storage quota exceeded");
        }
    }
    
    private Path createUploadDirectory(Long tenantId) throws IOException {
        Path uploadPath = Paths.get(uploadDir, "tenant_" + tenantId);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath);
        }
        
        return uploadPath;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private String calculateChecksum(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Failed to calculate checksum: {}", e.getMessage());
            return "";
        }
    }
}