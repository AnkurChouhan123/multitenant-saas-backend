package com.saas.platform.controller;

import com.saas.platform.model.FileStorage;
import com.saas.platform.security.RoleValidator;
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


@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileStorageController {
    
    private final FileStorageService fileStorageService;
    private final RoleValidator roleValidator;
    
    public FileStorageController(FileStorageService fileStorageService,
                                RoleValidator roleValidator) {
        this.fileStorageService = fileStorageService;
        this.roleValidator = roleValidator;
    }
    
   // upload files
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long tenantId,
            @RequestParam Long userId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category) {
        
        try {
            // Check upload permission (blocks VIEWER)
            roleValidator.requireUploadPermission();
            
            // Validate tenant access
            roleValidator.requireTenantAccess(tenantId);
            
            FileStorage uploadedFile = fileStorageService.uploadFile(
                file, tenantId, userId, description, category);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
   // download file
    @GetMapping("/download/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam Long userId) {
        
        try {
            FileStorage file = fileStorageService.getFileById(fileId);
            
            // Validate tenant access
            roleValidator.requireTenantAccess(file.getTenantId());
            
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
    
    // can view files (of tenant)
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<FileStorage>> getTenantFiles(@PathVariable Long tenantId) {
        // Validate tenant isolation
        roleValidator.validateTenantIsolation(tenantId);
        
        List<FileStorage> files = fileStorageService.getFilesByTenant(tenantId);
        return ResponseEntity.ok(files);
    }
    
   //can view files of (user)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<FileStorage>> getUserFiles(@PathVariable Long userId) {
        List<FileStorage> files = fileStorageService.getFilesByUser(userId);
        return ResponseEntity.ok(files);
    }
    
   // categorize files
    @GetMapping("/category/{tenantId}/{category}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<FileStorage>> getFilesByCategory(
            @PathVariable Long tenantId,
            @PathVariable String category) {
        
        // Validate tenant isolation
        roleValidator.validateTenantIsolation(tenantId);
        
        List<FileStorage> files = fileStorageService.getFilesByCategory(tenantId, category);
        return ResponseEntity.ok(files);
    }
    
    // search files
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<FileStorage>> searchFiles(
            @RequestParam Long tenantId,
            @RequestParam String keyword) {
        
        // Validate tenant isolation
        roleValidator.validateTenantIsolation(tenantId);
        
        List<FileStorage> files = fileStorageService.searchFiles(tenantId, keyword);
        return ResponseEntity.ok(files);
    }
    
    // get recent files
    @GetMapping("/recent/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<FileStorage>> getRecentFiles(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        
        // Validate tenant isolation
        roleValidator.validateTenantIsolation(tenantId);
        
        List<FileStorage> files = fileStorageService.getRecentFiles(tenantId, limit);
        return ResponseEntity.ok(files);
    }
    
   // get files details
    @GetMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<FileStorage> getFileById(@PathVariable Long fileId) {
        FileStorage file = fileStorageService.getFileById(fileId);
        
        // Validate tenant access
        roleValidator.requireTenantAccess(file.getTenantId());
        
        return ResponseEntity.ok(file);
    }
    
   // update files(viewer connot)
    @PutMapping("/{fileId}/metadata")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> updateMetadata(
            @PathVariable Long fileId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags) {
        
        FileStorage file = fileStorageService.getFileById(fileId);
        
        // Validate tenant access
        roleValidator.requireTenantAccess(file.getTenantId());
        
        // Check if viewer (viewers can't modify)
        roleValidator.requireUploadPermission();
        
        FileStorage updated = fileStorageService.updateFileMetadata(
            fileId, description, category, tags);
        
        return ResponseEntity.ok(updated);
    }
    
    // share files
    @PostMapping("/{fileId}/share")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> shareFile(
            @PathVariable Long fileId,
            @RequestBody List<Long> userIds) {
        
        FileStorage file = fileStorageService.getFileById(fileId);
        
        // Validate tenant access
        roleValidator.requireTenantAccess(file.getTenantId());
        
        // Check if viewer (viewers can't share)
        roleValidator.requireUploadPermission();
        
        FileStorage shared = fileStorageService.shareFile(fileId, userIds);
        return ResponseEntity.ok(shared);
    }
    
    // delete files
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long fileId,
            @RequestParam Long userId) {
        
        FileStorage file = fileStorageService.getFileById(fileId);
        
        // Validate tenant access
        roleValidator.requireTenantAccess(file.getTenantId());
        
        // Check if viewer (viewers can't delete)
        roleValidator.requireUploadPermission();
        
        fileStorageService.deleteFile(fileId, userId);
        return ResponseEntity.ok("File deleted successfully");
    }
    
   // delete files permanently
    @DeleteMapping("/{fileId}/permanent")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> permanentlyDeleteFile(@PathVariable Long fileId) {
        try {
            FileStorage file = fileStorageService.getFileById(fileId);
            
            // Only SUPER_ADMIN or TENANT_OWNER can permanently delete
            roleValidator.requirePermanentDeletePermission(file.getTenantId());
            
            fileStorageService.permanentlyDeleteFile(fileId);
            return ResponseEntity.ok("File permanently deleted");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete file");
        }
    }
    
    // restore file
    @PutMapping("/{fileId}/restore")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<FileStorage> restoreFile(@PathVariable Long fileId) {
        FileStorage file = fileStorageService.getFileById(fileId);
        
        // Validate tenant access
        roleValidator.requireTenantAccess(file.getTenantId());
        
        // Check if viewer (viewers can't restore)
        roleValidator.requireUploadPermission();
        
        FileStorage restored = fileStorageService.restoreFile(fileId);
        return ResponseEntity.ok(restored);
    }
    
    // view storage stats or usage
    @GetMapping("/storage/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable Long tenantId) {
        // Validate tenant isolation
        roleValidator.validateTenantIsolation(tenantId);
        
        Long totalStorage = fileStorageService.getTotalStorageUsed(tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalStorageBytes", totalStorage);
        response.put("totalStorageMB", totalStorage / (1024.0 * 1024));
        response.put("totalStorageGB", totalStorage / (1024.0 * 1024 * 1024));
        
        return ResponseEntity.ok(response);
    }
}