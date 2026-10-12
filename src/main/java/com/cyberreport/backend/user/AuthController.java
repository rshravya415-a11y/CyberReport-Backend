
package com.cyberreport.backend.user;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (request.name == null || request.name.isBlank()
                || request.email == null || request.email.isBlank()
                || request.password == null || request.password.length() < 8) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "Enter your name and email, and use a password of at least 8 characters.")
            );
        }

        String email = request.email.trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("message", "An account with this email already exists.")
            );
        }

        User user = new User();
        user.setName(request.name.trim());
        user.setEmail(email);
        if (request.phone != null && !request.phone.isBlank()) {
            user.setPhone(request.phone.trim());
        }
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("USER");

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of("message", "Registration successful. Please log in.")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request.email == null || request.password == null) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Email and password are required.")
            );
        }

        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.email.trim().toLowerCase(),
                        request.password
                    )
                );

            SecurityContext context =
                SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            contextRepository.saveContext(
                context, httpRequest, httpResponse
            );

            User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow();

            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
                "phone", user.getPhone() != null ? user.getPhone() : ""
            ));

        } catch (org.springframework.security.core.AuthenticationException ex) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Invalid email or password.")
            );
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Email", required = false)
            String headerEmail,
            Authentication authentication) {

        String email = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
        } else if (headerEmail != null && !headerEmail.isBlank()) {
            email = headerEmail.trim();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to view profile."));
        }

        return userRepository.findByEmailIgnoreCase(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "name", user.getName() != null ? user.getName() : "",
                        "email", user.getEmail(),
                        "phone", user.getPhone() != null ? user.getPhone() : "",
                        "role", user.getRole()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User account not found.")));
    }

    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Email", required = false)
            String headerEmail,
            Authentication authentication) {

        String email = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
        } else if (headerEmail != null && !headerEmail.isBlank()) {
            email = headerEmail.trim();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to update profile."));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found."));
        }

        if (request.name != null && !request.name.isBlank()) {
            user.setName(request.name.trim());
        }
        if (request.phone != null) {
            user.setPhone(request.phone.trim());
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully.",
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole()
        ));
    }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
        public String phone;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class ProfileUpdateRequest {
        public String name;
        public String phone;
    }
}
