package com.cyberreport.backend.user;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAndMigratePasswords(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. Ensure primary admin account exists
            String defaultAdminEmail = "admin@cyberreport.com";
            User admin = userRepository.findByEmailIgnoreCase(defaultAdminEmail).orElse(null);

            if (admin == null) {
                admin = new User();
                admin.setName("CyberReport Admin");
                admin.setEmail(defaultAdminEmail);
                admin.setRole("ADMIN");
                admin.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(admin);
                System.out.println("Default admin created: " + defaultAdminEmail + " / admin123");
            }

            // 2. Safely migrate any legacy plain-text passwords to BCrypt and ensure contact phone numbers
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                boolean changed = false;
                String pwd = user.getPassword();
                if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
                    user.setPassword(passwordEncoder.encode(pwd));
                    changed = true;
                    System.out.println("Migrated plain-text password for account: " + user.getEmail());
                }
                if (user.getPhone() == null || user.getPhone().isBlank()) {
                    if ("admin@cyberreport.com".equalsIgnoreCase(user.getEmail())) {
                        user.setPhone("+91 99999 99999");
                    } else if ("tester_audit1@example.com".equalsIgnoreCase(user.getEmail())) {
                        user.setPhone("+91 98451 23098");
                    } else if (user.getEmail() != null && user.getEmail().contains("shravya")) {
                        user.setPhone("+91 94482 71043");
                    } else {
                        user.setPhone("+91 98765 43210");
                    }
                    changed = true;
                }
                if (changed) {
                    userRepository.save(user);
                }
            }

            // 3. Check for environment override if provided
            String envEmail = System.getenv("CYBERREPORT_ADMIN_EMAIL");
            String envPassword = System.getenv("CYBERREPORT_ADMIN_PASSWORD");

            if (envEmail != null && !envEmail.isBlank() && envPassword != null && envPassword.length() >= 8) {
                User envAdmin = userRepository.findByEmailIgnoreCase(envEmail.trim()).orElse(null);
                if (envAdmin != null && "ADMIN".equalsIgnoreCase(envAdmin.getRole())) {
                    envAdmin.setPassword(passwordEncoder.encode(envPassword));
                    userRepository.save(envAdmin);
                    System.out.println("Admin password updated from environment variable for: " + envEmail);
                }
            }
        };
    }
}
