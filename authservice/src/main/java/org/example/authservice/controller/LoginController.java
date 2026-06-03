package org.example.authservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * Serves the Thymeleaf login template at templates/login.html.
     * Thymeleaf renders the CSRF token into the form server-side.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
