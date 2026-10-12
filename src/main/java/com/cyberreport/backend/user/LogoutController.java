
package com.cyberreport.backend.user;

import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class LogoutController {

    private final SecurityContextRepository contextRepository;

    public LogoutController(SecurityContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @PostMapping("/api/logout")
    public Map<String, String> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

        return Map.of("message", "Logged out successfully");
    }
}
