package com.mediflow.config;

import com.mediflow.entity.Role;
import com.mediflow.entity.User;
import com.mediflow.repository.RoleRepository;
import com.mediflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Bootstraps roles and the default admin ONLY when the database is empty
 * (e.g. a fresh H2 test run). With the standard mediflow.sql seed nothing
 * here changes because all roles and users already exist.
 */
@Configuration
public class DataInitializer {

    @Bean
    @org.springframework.core.annotation.Order(1)
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.saveAll(List.of(
                        new Role(Role.ADMIN), new Role(Role.PHARMACIST),
                        new Role(Role.STORE_KEEPER), new Role(Role.PROCUREMENT_OFFICER),
                        new Role(Role.CASHIER), new Role(Role.CRO), new Role(Role.FINANCE_MANAGER)));
            }
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setEmail("admin@mediflow.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole(roleRepository.findByName(Role.ADMIN).orElseThrow());
                admin.setActive(true);
                userRepository.save(admin);
            }
        };
    }
}
