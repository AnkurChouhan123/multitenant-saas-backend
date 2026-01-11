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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.saas.platform.model.Subscription;
import com.saas.platform.model.Plan;
import com.saas.platform.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;

//
// FileStorageService - NPE FIXED
// Fixed: Added null checks in validation methods

@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

	@Value("${app.storage.default-quota-gb:10}")
	private Integer defaultStorageQuotaGB;

	private final SubscriptionRepository subscriptionRepository;

	@Value("${file.upload.dir:uploads}")
	private String uploadDir;

	@Value("${file.max.size:10485760}") // 10MB default
	private Long maxFileSize;

	private final FileStorageRepository fileStorageRepository;
	private final ActivityLogService activityLogService;
	private final WebhookService webhookService;

	public FileStorageService(FileStorageRepository fileStorageRepository, ActivityLogService activityLogService,
			SubscriptionRepository subscriptionRepository, WebhookService webhookService) {
		this.fileStorageRepository = fileStorageRepository;
		this.activityLogService = activityLogService;
		this.subscriptionRepository = subscriptionRepository;
		this.webhookService = webhookService;
	}

	//
// Upload a file

	@Transactional
	public FileStorage uploadFile(MultipartFile file, Long tenantId, Long userId, String description, String category)
			throws IOException {
		log.info("📤 Starting file upload: {} for tenant: {}", file.getOriginalFilename(), tenantId);

		// Validate file
		validateFile(file);

		// Check storage quota
		checkStorageQuota(tenantId, file.getSize());

		// Create upload directory if not exists
		Path uploadPath = createUploadDirectory(tenantId);

		// Generate unique filename preserving extension
		String originalFilename = file.getOriginalFilename();
		String fileExtension = getFileExtension(originalFilename);
		String storedFilename = UUID.randomUUID().toString() + "." + fileExtension;

		// ✅ CRITICAL FIX: Save file as pure binary
		Path filePath = uploadPath.resolve(storedFilename);

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
		}

		// Verify file was saved correctly
		if (!Files.exists(filePath)) {
			throw new IOException("File was not saved correctly");
		}

		long savedFileSize = Files.size(filePath);
		if (savedFileSize != file.getSize()) {
			log.warn("⚠️ File size mismatch! Original: {}, Saved: {}", file.getSize(), savedFileSize);
		}

		// Get MIME type
		String mimeType = file.getContentType();
		if (mimeType == null || mimeType.isEmpty()) {
			try {
				mimeType = Files.probeContentType(filePath);
			} catch (Exception e) {
				log.warn("Failed to probe content type: {}", e.getMessage());
			}
			if (mimeType == null) {
				mimeType = getMimeTypeFromExtension(fileExtension);
			}
		}

		log.info("📝 File details - Extension: {}, MIME: {}, Size: {}", fileExtension, mimeType, savedFileSize);

		// Calculate checksum
		String checksum = calculateChecksum(filePath);

		// Create file record
		FileStorage fileStorage = new FileStorage(tenantId, userId, originalFilename, storedFilename,
				filePath.toString(), savedFileSize, mimeType, fileExtension);

		fileStorage.setDescription(description);
		fileStorage.setCategory(category);
		fileStorage.setChecksum(checksum);

		FileStorage saved = fileStorageRepository.save(fileStorage);

		webhookService.triggerWebhook(tenantId, "file.uploaded",
				java.util.Map.of("fileId", saved.getId(), "fileName", saved.getOriginalFilename(), "fileSize",
						saved.getFileSize(), "fileType", fileExtension, "mimeType", mimeType, "uploadedBy", userId));

		// Log activity
		activityLogService.logActivity(tenantId, userId, "system", "System", "File uploaded: " + originalFilename,
				"data", String.format("Size: %s, Type: %s", saved.getFileSizeFormatted(), fileExtension));

		log.info("✅ File uploaded successfully - ID: {}, Path: {}", saved.getId(), filePath);

		return saved;
	}
	
	private String getMimeTypeFromExtension(String extension) {
	    if (extension == null || extension.isEmpty()) {
	        return "application/octet-stream";
	    }
	    
	    switch (extension.toLowerCase()) {
	        // Documents
	        case "pdf": return "application/pdf";
	        case "doc": return "application/msword";
	        case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	        case "xls": return "application/vnd.ms-excel";
	        case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	        case "ppt": return "application/vnd.ms-powerpoint";
	        case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
	        case "txt": return "text/plain";
	        case "csv": return "text/csv";
	        
	        // Images
	        case "jpg":
	        case "jpeg": return "image/jpeg";
	        case "png": return "image/png";
	        case "gif": return "image/gif";
	        case "svg": return "image/svg+xml";
	        case "webp": return "image/webp";
	        case "bmp": return "image/bmp";
	        case "ico": return "image/x-icon";
	        
	        // Videos
	        case "mp4": return "video/mp4";
	        case "avi": return "video/x-msvideo";
	        case "mov": return "video/quicktime";
	        case "wmv": return "video/x-ms-wmv";
	        case "flv": return "video/x-flv";
	        case "mkv": return "video/x-matroska";
	        
	        // Audio
	        case "mp3": return "audio/mpeg";
	        case "wav": return "audio/wav";
	        case "ogg": return "audio/ogg";
	        case "m4a": return "audio/mp4";
	        
	        // Archives
	        case "zip": return "application/zip";
	        case "rar": return "application/x-rar-compressed";
	        case "7z": return "application/x-7z-compressed";
	        case "tar": return "application/x-tar";
	        case "gz": return "application/gzip";
	        
	        // Code
	        case "json": return "application/json";
	        case "xml": return "application/xml";
	        case "html": return "text/html";
	        case "css": return "text/css";
	        case "js": return "application/javascript";
	        
	        default: return "application/octet-stream";
	    }
	}

	//
// Download a file

	public Resource downloadFile(Long fileId, Long userId) throws IOException {
    log.info("📥 Starting download - File ID: {} by user: {}", fileId, userId);
    
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
    
    // Verify file exists
    if (!Files.exists(filePath)) {
        log.error("❌ File not found on disk: {}", filePath);
        throw new IOException("File not found: " + file.getOriginalFilename());
    }
    
    // Verify file is readable
    if (!Files.isReadable(filePath)) {
        log.error("❌ File is not readable: {}", filePath);
        throw new IOException("File is not readable: " + file.getOriginalFilename());
    }
    
    // Create resource
    Resource resource = new UrlResource(filePath.toUri());
    
    if (!resource.exists()) {
        throw new IOException("Resource does not exist: " + file.getOriginalFilename());
    }
    
    // Increment download count
    file.incrementDownloadCount();
    fileStorageRepository.save(file);
    
    log.info("✅ File ready for download: {} ({})", 
        file.getOriginalFilename(), file.getFileSizeFormatted());
    
    return resource;
}

	//
// Get all files for a tenant

	public List<FileStorage> getFilesByTenant(Long tenantId) {
		return fileStorageRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);
	}

	//
// Get files uploaded by a user

	public List<FileStorage> getFilesByUser(Long userId) {
		return fileStorageRepository.findByUploadedByAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
	}

	//
// Get files by category

	public List<FileStorage> getFilesByCategory(Long tenantId, String category) {
		return fileStorageRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId, category);
	}

	//
// Search files by name

	public List<FileStorage> searchFiles(Long tenantId, String keyword) {
		return fileStorageRepository.findByTenantIdAndOriginalFilenameContainingIgnoreCaseAndDeletedAtIsNull(tenantId,
				keyword);
	}

	//
// Update file metadata

	@Transactional
	public FileStorage updateFileMetadata(Long fileId, String description, String category, String tags) {
		FileStorage file = getFileById(fileId);

		if (description != null)
			file.setDescription(description);
		if (category != null)
			file.setCategory(category);
		if (tags != null)
			file.setTags(tags);

		return fileStorageRepository.save(file);
	}

	//
// Share file with users

	@Transactional
	public FileStorage shareFile(Long fileId, List<Long> userIds) {
		FileStorage file = getFileById(fileId);

		String sharedWith = userIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");

		file.setSharedWith(sharedWith);

		FileStorage saved = fileStorageRepository.save(file);

		return saved;
	}

	//
// Soft delete a file

	@Transactional
	public void deleteFile(Long fileId, Long userId) {
		log.info("Deleting file ID: {} by user: {}", fileId, userId);

		FileStorage file = getFileById(fileId);
		file.softDelete();

		fileStorageRepository.save(file);

		webhookService.triggerWebhook(file.getTenantId(), "file.deleted",
				java.util.Map.of("fileId", fileId, "fileName", file.getOriginalFilename(), "deletedBy", userId));

		// Log activity
		activityLogService.logActivity(file.getTenantId(), userId, "system", "System",
				"File deleted: " + file.getOriginalFilename(), "data", "File moved to trash");

		log.info("File deleted successfully");
	}

	//
// Permanently delete a file

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

	//
// Restore deleted file

	@Transactional
	public FileStorage restoreFile(Long fileId) {
		FileStorage file = getFileById(fileId);

		if (!file.isDeleted()) {
			throw new IllegalArgumentException("File is not deleted");
		}

		file.setDeletedAt(null);
		return fileStorageRepository.save(file);
	}

	//
// Get total storage used by tenant

	public Long getTotalStorageUsed(Long tenantId) {
		return fileStorageRepository.sumFileSizeByTenantId(tenantId);
	}

	//
// Get file by ID

	public FileStorage getFileById(Long id) {
		return fileStorageRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + id));
	}

	//
// Get recent files

	public List<FileStorage> getRecentFiles(Long tenantId, int limit) {
		return fileStorageRepository.findTop10ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);
	}

	// Private helper methods

	//
// ✅ FIXED: Added null check for file validation

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty or null");
		}

		if (file.getSize() > maxFileSize) {
			throw new IllegalArgumentException(
					"File size exceeds maximum allowed size: " + (maxFileSize / 1024 / 1024) + "MB");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || filename.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid filename");
		}
	}

	private void checkStorageQuota(Long tenantId, Long fileSize) {
		Long currentStorage = getTotalStorageUsed(tenantId);

		// ✅ FIXED: Get max storage from tenant's plan
		Long maxStorageBytes = getMaxStorageForTenant(tenantId);

		if (currentStorage + fileSize > maxStorageBytes) {
			double currentGB = currentStorage / (1024.0 * 1024 * 1024);
			double maxGB = maxStorageBytes / (1024.0 * 1024 * 1024);

			throw new IllegalArgumentException(
					String.format("Storage quota exceeded. Using %.2f GB of %.2f GB allowed. "
							+ "Please upgrade your plan or delete some files.", currentGB, maxGB));
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

	//
// ✅ FIXED: Added null safety check

	private String getFileExtension(String filename) {
		if (filename == null || filename.trim().isEmpty() || !filename.contains(".")) {
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

	private Long getMaxStorageForTenant(Long tenantId) {
		try {
			Subscription subscription = subscriptionRepository.findByTenantId(tenantId).orElse(null);

			if (subscription != null) {
				Plan plan = subscription.getPlan();

				// If unlimited (-1), return very large number
				if (plan.getMaxStorageGB() == -1) {
					return Long.MAX_VALUE; // Effectively unlimited
				}

				// Convert GB to bytes
				return (long) plan.getMaxStorageGB() * 1024 * 1024 * 1024;
			}
		} catch (Exception e) {
			log.warn("Failed to get storage quota from subscription, using default: {}", e.getMessage());
		}

		// Fallback to configured default
		return (long) defaultStorageQuotaGB * 1024 * 1024 * 1024;
	}

	public Map<String, Object> getStorageQuotaInfo(Long tenantId) {
		Long usedBytes = getTotalStorageUsed(tenantId);
		Long maxBytes = getMaxStorageForTenant(tenantId);

		double usedGB = usedBytes / (1024.0 * 1024 * 1024);
		double maxGB = maxBytes / (1024.0 * 1024 * 1024);
		double percentageUsed = maxBytes > 0 ? (usedBytes * 100.0) / maxBytes : 0;

		Map<String, Object> quotaInfo = new HashMap<>();
		quotaInfo.put("usedBytes", usedBytes);
		quotaInfo.put("maxBytes", maxBytes);
		quotaInfo.put("usedGB", String.format("%.2f", usedGB));
		quotaInfo.put("maxGB", maxBytes == Long.MAX_VALUE ? "Unlimited" : String.format("%.2f", maxGB));
		quotaInfo.put("percentageUsed", String.format("%.1f", percentageUsed));
		quotaInfo.put("isUnlimited", maxBytes == Long.MAX_VALUE);
		quotaInfo.put("remainingBytes", maxBytes - usedBytes);
		quotaInfo.put("remainingGB", String.format("%.2f", (maxBytes - usedBytes) / (1024.0 * 1024 * 1024)));

		return quotaInfo;
	}

	// ===================================================================
	// ROLE-BASED ACCESS CONTROL METHODS
	// ===================================================================

	/**
	 * Check if a user can access a file based on their role
	 */
	public boolean canAccessFile(FileStorage file, Long userId, String role) {
		// TENANT_OWNER and TENANT_ADMIN can access all files
		if ("ROLE_TENANT_OWNER".equals(role) || "ROLE_TENANT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
			return true;
		}

		// USER can access their own files or files shared with them
		if ("ROLE_USER".equals(role)) {
			return file.getUploadedBy().equals(userId) || isSharedWithUser(file, userId);
		}

		// VIEWER can only access files shared with them
		if ("ROLE_VIEWER".equals(role)) {
			return isSharedWithUser(file, userId);
		}

		return false;
	}

	/**
	 * Check if file is shared with a specific user
	 */
	private boolean isSharedWithUser(FileStorage file, Long userId) {
		if (file.getSharedWith() == null || file.getSharedWith().isEmpty()) {
			return false;
		}
		return file.getSharedWith().contains(userId.toString());
	}

	/**
	 * Get files accessible by USER role (own files + shared files)
	 */
	public List<FileStorage> getFilesForUser(Long tenantId, Long userId) {
		List<FileStorage> allFiles = fileStorageRepository
				.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);

		return allFiles.stream().filter(file -> file.getUploadedBy().equals(userId) || isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Get files shared with VIEWER role
	 */
	public List<FileStorage> getSharedFilesForUser(Long tenantId, Long userId) {
		List<FileStorage> allFiles = fileStorageRepository
				.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);

		return allFiles.stream().filter(file -> isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Get files by category for USER role
	 */
	public List<FileStorage> getFilesByCategoryForUser(Long tenantId, String category, Long userId) {
		List<FileStorage> categoryFiles = fileStorageRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId,
				category);

		return categoryFiles.stream()
				.filter(file -> file.getUploadedBy().equals(userId) || isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Get shared files by category for VIEWER role
	 */
	public List<FileStorage> getSharedFilesByCategory(Long tenantId, String category, Long userId) {
		List<FileStorage> categoryFiles = fileStorageRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId,
				category);

		return categoryFiles.stream().filter(file -> isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Search files for USER role
	 */
	public List<FileStorage> searchFilesForUser(Long tenantId, String keyword, Long userId) {
		List<FileStorage> searchResults = fileStorageRepository
				.findByTenantIdAndOriginalFilenameContainingIgnoreCaseAndDeletedAtIsNull(tenantId, keyword);

		return searchResults.stream()
				.filter(file -> file.getUploadedBy().equals(userId) || isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Search shared files for VIEWER role
	 */
	public List<FileStorage> searchSharedFiles(Long tenantId, String keyword, Long userId) {
		List<FileStorage> searchResults = fileStorageRepository
				.findByTenantIdAndOriginalFilenameContainingIgnoreCaseAndDeletedAtIsNull(tenantId, keyword);

		return searchResults.stream().filter(file -> isSharedWithUser(file, userId))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Get recent files for USER role
	 */
	public List<FileStorage> getRecentFilesForUser(Long tenantId, Long userId, int limit) {
		List<FileStorage> recentFiles = fileStorageRepository
				.findTop10ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);

		return recentFiles.stream()
				.filter(file -> file.getUploadedBy().equals(userId) || isSharedWithUser(file, userId)).limit(limit)
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Get recent shared files for VIEWER role
	 */
	public List<FileStorage> getRecentSharedFiles(Long tenantId, Long userId, int limit) {
		List<FileStorage> recentFiles = fileStorageRepository
				.findTop10ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId);

		return recentFiles.stream().filter(file -> isSharedWithUser(file, userId)).limit(limit)
				.collect(java.util.stream.Collectors.toList());
	}

}