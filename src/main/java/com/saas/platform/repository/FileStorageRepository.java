package com.saas.platform.repository;

import com.saas.platform.model.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FileStorageRepository - Database operations for file storage
 */
@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {
    
    // Find files by tenant (excluding deleted)
    List<FileStorage> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long tenantId);
    
    // Find files by user (excluding deleted)
    List<FileStorage> findByUploadedByAndDeletedAtIsNullOrderByCreatedAtDesc(Long uploadedBy);
    
    // Find files by category
    List<FileStorage> findByTenantIdAndCategoryAndDeletedAtIsNull(Long tenantId, String category);
    
    // Search files by name
    List<FileStorage> findByTenantIdAndOriginalFilenameContainingIgnoreCaseAndDeletedAtIsNull(
        Long tenantId, String keyword);
    
    // Find deleted files (trash)
    List<FileStorage> findByTenantIdAndDeletedAtIsNotNull(Long tenantId);
    
    // Find recent files
    List<FileStorage> findTop10ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long tenantId);
    
    // Find files by tags
    List<FileStorage> findByTenantIdAndTagsContainingAndDeletedAtIsNull(Long tenantId, String tag);
    
    // Find shared files for a user
    @Query("SELECT f FROM FileStorage f WHERE f.tenantId = ?1 AND f.sharedWith LIKE %?2% AND f.deletedAt IS NULL")
    List<FileStorage> findSharedFiles(Long tenantId, String userId);
    
    // Find expired files
    List<FileStorage> findByExpiresAtBeforeAndDeletedAtIsNull(LocalDateTime dateTime);
    
    // Calculate total storage used by tenant
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileStorage f WHERE f.tenantId = ?1 AND f.deletedAt IS NULL")
    Long sumFileSizeByTenantId(Long tenantId);
    
    // Count files by tenant
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    // Find files by extension
    List<FileStorage> findByTenantIdAndFileExtensionAndDeletedAtIsNull(
        Long tenantId, String fileExtension);
    
    // Find large files (> specific size)
    @Query("SELECT f FROM FileStorage f WHERE f.tenantId = ?1 AND f.fileSize > ?2 AND f.deletedAt IS NULL")
    List<FileStorage> findLargeFiles(Long tenantId, Long minSize);
    
    // Find files uploaded in date range
    List<FileStorage> findByTenantIdAndCreatedAtBetweenAndDeletedAtIsNull(
        Long tenantId, LocalDateTime start, LocalDateTime end);
}