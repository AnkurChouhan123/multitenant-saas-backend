package com.saas.platform.controller;

import com.saas.platform.model.ActivityLog;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//  ActivityLogController with proper permissions:
//  - TENANT_OWNER, TENANT_ADMIN: Full access ✅
// - USER: No access ❌
// - VIEWER: No access ❌

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class ActivityLogController {

	private final ActivityLogService activityLogService;
	private final RoleValidator roleValidator;

	public ActivityLogController(ActivityLogService activityLogService, RoleValidator roleValidator) {
		this.activityLogService = activityLogService;
		this.roleValidator = roleValidator;
	}

	// View tenant activities - ADMIN ONLY

	@GetMapping("/tenant/{tenantId}")
	public ResponseEntity<?> getActivitiesByTenant(@PathVariable Long tenantId) {
		try {
			// Only TENANT_OWNER and TENANT_ADMIN can view detailed logs
			roleValidator.requireDetailedLogPermission(tenantId);

			List<ActivityLog> activities = activityLogService.getActivitiesByTenant(tenantId);
			return ResponseEntity.ok(activities);
		} catch (SecurityException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse(e.getMessage()));
		}
	}

	// View activities with pagination - ADMIN ONLY

	@GetMapping("/tenant/{tenantId}/page")
	public ResponseEntity<?> getActivitiesPage(@PathVariable Long tenantId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		try {
			// Only TENANT_OWNER and TENANT_ADMIN can view detailed logs
			roleValidator.requireDetailedLogPermission(tenantId);

			Page<ActivityLog> activities = activityLogService.getActivitiesPage(tenantId, page, size);
			return ResponseEntity.ok(activities);
		} catch (SecurityException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse(e.getMessage()));
		}
	}

	// Filter activities by type - ADMIN ONLY

	@GetMapping("/tenant/{tenantId}/type/{actionType}")
	public ResponseEntity<?> getActivitiesByType(@PathVariable Long tenantId, @PathVariable String actionType) {
		try {
			// Only TENANT_OWNER and TENANT_ADMIN can view detailed logs
			roleValidator.requireDetailedLogPermission(tenantId);

			List<ActivityLog> activities = activityLogService.getActivitiesByType(tenantId, actionType);
			return ResponseEntity.ok(activities);
		} catch (SecurityException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse(e.getMessage()));
		}
	}

       // View activities by date range - ADMIN ONLY

	@GetMapping("/tenant/{tenantId}/range")
	public ResponseEntity<?> getActivitiesByDateRange(@PathVariable Long tenantId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		try {
			// Only TENANT_OWNER and TENANT_ADMIN can view detailed logs
			roleValidator.requireDetailedLogPermission(tenantId);

			List<ActivityLog> activities = activityLogService.getActivitiesByDateRange(tenantId, start, end);
			return ResponseEntity.ok(activities);
		} catch (SecurityException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse(e.getMessage()));
		}
	}

	
	  //Check if current user can access activity logs
	
	@GetMapping("/check-permission/{tenantId}")
	public ResponseEntity<Map<String, Object>> checkPermission(@PathVariable Long tenantId) {
		Map<String, Object> response = new HashMap<>();

		try {
			response.put("canViewLogs", roleValidator.canViewDetailedLogs(tenantId));
			response.put("role", roleValidator.getCurrentUser().getRole().toString());
		} catch (Exception e) {
			response.put("canViewLogs", false);
			response.put("error", e.getMessage());
		}

		return ResponseEntity.ok(response);
	}

	// Helper method
	private Map<String, String> createErrorResponse(String message) {
		Map<String, String> response = new HashMap<>();
		response.put("error", message);
		return response;
	}
}