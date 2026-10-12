package com.cyberreport.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/login",
        "/register",
        "/report",
        "/track",
        "/dashboard",
        "/profile",
        "/my-complaints",
        "/admin/login",
        "/admin/dashboard",
        "/resources",
        "/security-tools",
        "/complaint/**",
        "/complaints/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
