package com.saas.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MultiTenantPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiTenantPlatformApplication.class, args);
	}

}
