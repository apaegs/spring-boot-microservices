package org.example.authservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * The public base URL of the BFF/app, used to build the link to the
     * registration page. Configurable via the PUBLIC_APP_URL environment
     * variable, so scheme/host can change per deployment; defaults to
     * http://app.localhost for local Kubernetes.
     */
    @Value("${public.app.url}")
    private String publicAppUrl;

    /**
     * Serves the Thymeleaf login template at templates/login.html.
     * Thymeleaf renders the CSRF token into the form server-side.
     * The public app URL is added to the model so the "Create one"
     * link can be built without hardcoding the host.
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("publicAppUrl", publicAppUrl);
        return "login";
    }
}
