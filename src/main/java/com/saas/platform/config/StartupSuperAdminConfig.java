package com.saas.platform.config;

import com.saas.platform.model.*;
import com.saas.platform.repository.TenantRepository;
import com.saas.platform.repository.UserRepository;
import com.saas.platform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupSuperAdminConfig implements CommandLineRunner {
	
	@Value("${app.superadmin.default-password:CHANGE_ME_IN_PRODUCTION}")
	private String defaultPassword;

    private static final Logger log = LoggerFactory.getLogger(StartupSuperAdminConfig.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserService userService;

    public StartupSuperAdminConfig(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   UserService userService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {

        // If super admin exists → skip
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("SUPER_ADMIN already exists → skipping creation.");
            return;
        }

        log.warn("⚠ No SUPER_ADMIN found → Creating default SUPER_ADMIN...");

        // Create or get MASTER tenant
        Tenant masterTenant = tenantRepository.findBySubdomain("master")
                .orElseGet(() -> {
                    Tenant t = new Tenant();
                    t.setName("Master Tenant");
                    t.setSubdomain("master");
                    return tenantRepository.save(t);
                });

        // Create Super Admin
        User superAdmin = new User();
        superAdmin.setFirstName("Super");
        superAdmin.setLastName("Admin");
        superAdmin.setEmail("superadmin@saas.com");
        superAdmin.setPassword(defaultPassword);
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setTenant(masterTenant); //

        userService.saveWithoutTenant(superAdmin); // saves user with encoded password

        log.info("✅ SUPER_ADMIN created successfully");
        log.info("   email: superadmin@saas.com");
        log.info("   password: Admin@1234");
        log.info("   tenant: master");
    }
}
