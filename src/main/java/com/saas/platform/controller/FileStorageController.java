package com.saas.platform.controller;

import com.saas.platform.model.FileStorage;
import com.saas.platform.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileStorageController - REST API for file management
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileStorageController {
    
    private final FileStorageService fileStorageService;
    
    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * POST /api/files/upload - Upload a file
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long tenantId,
            @RequestParam Long userId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category) {
        
        try {
            FileStorage uploadedFile = fileStorageService.uploadFile(
                file, tenantId, userId, description, category);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/files/download/{fileId} - Download a file
     */
    @GetMapping("/download/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam Long userId) {
        
        try {
            FileStorage file = fileStorageService.getFileById(fileId);
            Resource resource = fileStorageService.downloadFile(fileId, userId);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * GET /api/files/tenant/{tenantId} - Get all files for tenant
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<FileStorage>> getTenantFiles(@PathVariable Long tenantId) {
        List<FileStorage> files = fileStorageService.getFilesByTenant(tenantId);
        return ResponseEntity.ok(files);
    }
    
    /**
     * GET /api/files/user/{userId} - Get files uploaded by user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<FileStorage>> getUserFiles(@PathVariable Long userId) {
        List<FileStorage> files = fileStorageService.getFilesByUser(userId);
        return ResponseEntity.ok(files);
    }
    
    /**
     * GET /api/files/category/{tenantId}/{category} - Get files by category
     */
    @GetMapping("/category/{tenantId}/{category}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<FileStorage>> getFilesByCategory(
            @PathVariable Long tenantId,
            @PathVariable String category) {
        
        List<FileStorage> files = fileStorageService.getFilesByCategory(tenantId, category);
        return ResponseEntity.ok(files);
    }
    
    /**
     * GET /api/files/search - Search files
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<FileStorage>> searchFiles(
            @RequestParam Long tenantId,
            @RequestParam String keyword) {
        
        List<FileStorage> files = fileStorageService.searchFiles(tenantId, keyword);
        return ResponseEntity.ok(files);
    }
    
    /**
     * GET /api/files/recent/{tenantId} - Get recent files
     */
    @GetMapping("/recent/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<FileStorage>> getRecentFiles(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<FileStorage> files = fileStorageService.getRecentFiles(tenantId, limit);
        return ResponseEntity.ok(files);
    }
    
    /**
     * GET /api/files/{fileId} - Get file details
     */
    @GetMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> getFileById(@PathVariable Long fileId) {
        FileStorage file = fileStorageService.getFileById(fileId);
        return ResponseEntity.ok(file);
    }
    
    /**
     * PUT /api/files/{fileId}/metadata - Update file metadata
     */
    @PutMapping("/{fileId}/metadata")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> updateMetadata(
            @PathVariable Long fileId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags) {
        
        FileStorage updated = fileStorageService.updateFileMetadata(
            fileId, description, category, tags);
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/files/{fileId}/share - Share file with users
     */
    @PostMapping("/{fileId}/share")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> shareFile(
            @PathVariable Long fileId,
            @RequestBody List<Long> userIds) {
        
        FileStorage shared = fileStorageService.shareFile(fileId, userIds);
        return ResponseEntity.ok(shared);
    }
    
    /**
     * DELETE /api/files/{fileId} - Soft delete file
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long fileId,
            @RequestParam Long userId) {
        
        fileStorageService.deleteFile(fileId, userId);
        return ResponseEntity.ok("File deleted successfully");
    }
    
    /**
     * DELETE /api/files/{fileId}/permanent - Permanently delete file
     */
    @DeleteMapping("/{fileId}/permanent")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> permanentlyDeleteFile(@PathVariable Long fileId) {
        try {
            fileStorageService.permanentlyDeleteFile(fileId);
            return ResponseEntity.ok("File permanently deleted");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete file");
        }
    }
    
    /**
     * PUT /api/files/{fileId}/restore - Restore deleted file
     */
    @PutMapping("/{fileId}/restore")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> restoreFile(@PathVariable Long fileId) {
        FileStorage restored = fileStorageService.restoreFile(fileId);
        return ResponseEntity.ok(restored);
    }
    
    /**
     * GET /api/files/storage/{tenantId} - Get storage usage
     */
    @GetMapping("/storage/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable Long tenantId) {
        Long totalStorage = fileStorageService.getTotalStorageUsed(tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalStorageBytes", totalStorage);
        response.put("totalStorageMB", totalStorage / (1024.0 * 1024));
        response.put("totalStorageGB", totalStorage / (1024.0 * 1024 * 1024));
        
        return ResponseEntity.ok(response);
    }
}