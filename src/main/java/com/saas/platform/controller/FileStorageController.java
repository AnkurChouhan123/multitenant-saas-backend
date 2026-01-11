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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

	public FileStorageController(FileStorageService fileStorageService, RoleValidator roleValidator) {
		this.fileStorageService = fileStorageService;
		this.roleValidator = roleValidator;
	}

	@PostMapping("/upload")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
	public ResponseEntity<FileStorage> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam Long tenantId,
			@RequestParam Long userId, @RequestParam(required = false) String description,
			@RequestParam(required = false) String category) {

		try {
			// Validate tenant access
			roleValidator.requireTenantAccess(tenantId);

			FileStorage uploadedFile = fileStorageService.uploadFile(file, tenantId, userId, description, category);

			return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);

		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/download/{fileId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, @RequestParam Long userId) {

		try {
			FileStorage file = fileStorageService.getFileById(fileId);

			// Validate tenant access
			roleValidator.requireTenantAccess(file.getTenantId());

			// Check file access permission based on role
			if (!fileStorageService.canAccessFile(file, userId, getCurrentUserRole())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}

			Resource resource = fileStorageService.downloadFile(fileId, userId);

		
			String contentType = file.getMimeType();
			if (contentType == null || contentType.isEmpty()) {
				contentType = "application/octet-stream";
			}
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			headers.setContentDispositionFormData("attachment", file.getOriginalFilename());
			headers.setContentLength(file.getFileSize());
			headers.setCacheControl("no-cache, no-store, must-revalidate");
			headers.setPragma("no-cache");
			headers.setExpires(0);


			return ResponseEntity.ok().headers(headers).body(resource);

		} catch (IOException e) {

			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/tenant/{tenantId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<List<FileStorage>> getTenantFiles(@PathVariable Long tenantId,
			@RequestParam(required = false) Long userId) {

		// Validate tenant isolation
		roleValidator.validateTenantIsolation(tenantId);

		String role = getCurrentUserRole();
		List<FileStorage> files;

		// TENANT_OWNER and TENANT_ADMIN can see ALL files
		if ("ROLE_TENANT_OWNER".equals(role) || "ROLE_TENANT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
			files = fileStorageService.getFilesByTenant(tenantId);
		}
		// USER can see their own files + shared files
		else if ("ROLE_USER".equals(role)) {
			files = fileStorageService.getFilesForUser(tenantId, userId);
		}
		// VIEWER can only see shared files
		else {
			files = fileStorageService.getSharedFilesForUser(tenantId, userId);
		}

		return ResponseEntity.ok(files);
	}


	// GET USER'S OWN FILES

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<List<FileStorage>> getUserFiles(@PathVariable Long userId) {
		List<FileStorage> files = fileStorageService.getFilesByUser(userId);
		return ResponseEntity.ok(files);
	}


	@GetMapping("/category/{tenantId}/{category}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<List<FileStorage>> getFilesByCategory(@PathVariable Long tenantId,
			@PathVariable String category, @RequestParam(required = false) Long userId) {

		roleValidator.validateTenantIsolation(tenantId);

		String role = getCurrentUserRole();
		List<FileStorage> files;

		if ("ROLE_TENANT_OWNER".equals(role) || "ROLE_TENANT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
			files = fileStorageService.getFilesByCategory(tenantId, category);
		} else if ("ROLE_USER".equals(role)) {
			files = fileStorageService.getFilesByCategoryForUser(tenantId, category, userId);
		} else {
			files = fileStorageService.getSharedFilesByCategory(tenantId, category, userId);
		}

		return ResponseEntity.ok(files);
	}


	@GetMapping("/search")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<List<FileStorage>> searchFiles(@RequestParam Long tenantId, @RequestParam String keyword,
			@RequestParam(required = false) Long userId) {

		roleValidator.validateTenantIsolation(tenantId);

		String role = getCurrentUserRole();
		List<FileStorage> files;

		if ("ROLE_TENANT_OWNER".equals(role) || "ROLE_TENANT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
			files = fileStorageService.searchFiles(tenantId, keyword);
		} else if ("ROLE_USER".equals(role)) {
			files = fileStorageService.searchFilesForUser(tenantId, keyword, userId);
		} else {
			files = fileStorageService.searchSharedFiles(tenantId, keyword, userId);
		}

		return ResponseEntity.ok(files);
	}


	@GetMapping("/recent/{tenantId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<List<FileStorage>> getRecentFiles(@PathVariable Long tenantId,
			@RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) Long userId) {

		roleValidator.validateTenantIsolation(tenantId);

		String role = getCurrentUserRole();
		List<FileStorage> files;

		if ("ROLE_TENANT_OWNER".equals(role) || "ROLE_TENANT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
			files = fileStorageService.getRecentFiles(tenantId, limit);
		} else if ("ROLE_USER".equals(role)) {
			files = fileStorageService.getRecentFilesForUser(tenantId, userId, limit);
		} else {
			files = fileStorageService.getRecentSharedFiles(tenantId, userId, limit);
		}

		return ResponseEntity.ok(files);
	}


	@GetMapping("/{fileId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<FileStorage> getFileById(@PathVariable Long fileId,
			@RequestParam(required = false) Long userId) {

		FileStorage file = fileStorageService.getFileById(fileId);

		// Validate tenant access
		roleValidator.requireTenantAccess(file.getTenantId());

		// Check file access permission
		if (userId != null && !fileStorageService.canAccessFile(file, userId, getCurrentUserRole())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok(file);
	}

	@PutMapping("/{fileId}/metadata")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
	public ResponseEntity<FileStorage> updateMetadata(@PathVariable Long fileId, @RequestParam Long userId,
			@RequestParam(required = false) String description, @RequestParam(required = false) String category,
			@RequestParam(required = false) String tags) {

		FileStorage file = fileStorageService.getFileById(fileId);

		// Validate tenant access
		roleValidator.requireTenantAccess(file.getTenantId());

		String role = getCurrentUserRole();

		// USER can only update their own files
		if ("ROLE_USER".equals(role) && !file.getUploadedBy().equals(userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		FileStorage updated = fileStorageService.updateFileMetadata(fileId, description, category, tags);

		return ResponseEntity.ok(updated);
	}

	
	@PostMapping("/{fileId}/share")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
	public ResponseEntity<FileStorage> shareFile(@PathVariable Long fileId, @RequestParam Long userId,
			@RequestBody List<Long> userIds) {

		FileStorage file = fileStorageService.getFileById(fileId);

		// Validate tenant access
		roleValidator.requireTenantAccess(file.getTenantId());

		String role = getCurrentUserRole();

		// USER can only share their own files
		if ("ROLE_USER".equals(role) && !file.getUploadedBy().equals(userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		FileStorage shared = fileStorageService.shareFile(fileId, userIds);
		return ResponseEntity.ok(shared);
	}

	
	@DeleteMapping("/{fileId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
	public ResponseEntity<String> deleteFile(@PathVariable Long fileId, @RequestParam Long userId) {

		FileStorage file = fileStorageService.getFileById(fileId);

		// Validate tenant access
		roleValidator.requireTenantAccess(file.getTenantId());

		String role = getCurrentUserRole();

		// USER can only delete their own files
		if ("ROLE_USER".equals(role) && !file.getUploadedBy().equals(userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own files");
		}

		fileStorageService.deleteFile(fileId, userId);
		return ResponseEntity.ok("File deleted successfully");
	}

	
	@DeleteMapping("/{fileId}/permanent")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<String> permanentlyDeleteFile(@PathVariable Long fileId) {
		try {
			FileStorage file = fileStorageService.getFileById(fileId);

			// Validate tenant access
			roleValidator.requireTenantAccess(file.getTenantId());

			fileStorageService.permanentlyDeleteFile(fileId);
			return ResponseEntity.ok("File permanently deleted");
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file");
		}
	}


	@PutMapping("/{fileId}/restore")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
	public ResponseEntity<FileStorage> restoreFile(@PathVariable Long fileId, @RequestParam Long userId) {

		FileStorage file = fileStorageService.getFileById(fileId);

		// Validate tenant access
		roleValidator.requireTenantAccess(file.getTenantId());

		String role = getCurrentUserRole();

		// USER can only restore their own files
		if ("ROLE_USER".equals(role) && !file.getUploadedBy().equals(userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		FileStorage restored = fileStorageService.restoreFile(fileId);
		return ResponseEntity.ok(restored);
	}


	@GetMapping("/storage/{tenantId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable Long tenantId) {
		try {
			// Validate tenant isolation
			roleValidator.validateTenantIsolation(tenantId);

			Map<String, Object> response = fileStorageService.getStorageQuotaInfo(tenantId);

			return ResponseEntity.ok(response);
		} catch (SecurityException e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
		}
	}

	@GetMapping("/storage-quota/{tenantId}")
	@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
	public ResponseEntity<Map<String, Object>> getStorageQuota(@PathVariable Long tenantId) {
		try {
			// Validate tenant isolation
			roleValidator.validateTenantIsolation(tenantId);

			Map<String, Object> quotaInfo = fileStorageService.getStorageQuotaInfo(tenantId);
			return ResponseEntity.ok(quotaInfo);
		} catch (SecurityException e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
		}
	}

	
	private String getCurrentUserRole() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getAuthorities() != null) {
			return authentication.getAuthorities().stream().findFirst().map(auth -> auth.getAuthority())
					.orElse("ROLE_USER");
		}
		return "ROLE_USER";
	}
}