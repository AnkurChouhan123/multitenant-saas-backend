package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

//
// FileStorage Entity - Manages uploaded files per tenant
 
@Entity
@Table(name = "file_storage")
public class FileStorage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
    
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    
    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename; // UUID-based filename
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Full path including subdirectories
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize; // Size in bytes
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Column(name = "file_extension", length = 20)
    private String fileExtension;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category", length = 50)
    private String category; // documents, images, videos, etc.
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    @Column(name = "download_count")
    private Long downloadCount = 0L;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "parent_file_id")
    private Long parentFileId; // For versioning
    
    @Column(name = "storage_provider", length = 50)
    private String storageProvider = "LOCAL"; // LOCAL, S3, AZURE, etc.
    
    @Column(name = "cloud_storage_url", length = 500)
    private String cloudStorageUrl;
    
    @Column(name = "checksum", length = 64)
    private String checksum; // SHA-256 hash for integrity
    
    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated tags
    
    @Column(name = "shared_with")
    private String sharedWith; // Comma-separated user IDs
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Soft delete
    
    // Constructors
    public FileStorage() {
    }
    
    public FileStorage(Long tenantId, Long uploadedBy, String originalFilename,
                      String storedFilename, String filePath, Long fileSize,
                      String mimeType, String fileExtension) {
        this.tenantId = tenantId;
        this.uploadedBy = uploadedBy;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { 
        this.originalFilename = originalFilename; 
    }
    
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { 
        this.storedFilename = storedFilename; 
    }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { 
        this.fileExtension = fileExtension; 
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Long downloadCount) { 
        this.downloadCount = downloadCount; 
    }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public Long getParentFileId() { return parentFileId; }
    public void setParentFileId(Long parentFileId) { 
        this.parentFileId = parentFileId; 
    }
    
    public String getStorageProvider() { return storageProvider; }
    public void setStorageProvider(String storageProvider) { 
        this.storageProvider = storageProvider; 
    }
    
    public String getCloudStorageUrl() { return cloudStorageUrl; }
    public void setCloudStorageUrl(String cloudStorageUrl) { 
        this.cloudStorageUrl = cloudStorageUrl; 
    }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getSharedWith() { return sharedWith; }
    public void setSharedWith(String sharedWith) { this.sharedWith = sharedWith; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public void incrementDownloadCount() {
        this.downloadCount++;
    }
    
    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}