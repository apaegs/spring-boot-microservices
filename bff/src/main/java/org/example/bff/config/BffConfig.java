package org.example.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class BffConfig {

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.message-service.url}")
    private String messageServiceUrl;

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/chat.html", true)
                )
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route()
                .GET("/api/users/**", http())
                .POST("/api/users/**", http())
                .PUT("/api/users/**", http())
                .DELETE("/api/users/**", http())
                .before(uri(userServiceUrl))
                .before(stripPrefix(1))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> messageServiceRoute() {
        return route()
                .GET("/api/messages/**", http())
                .POST("/api/messages/**", http())
                .before(uri(messageServiceUrl))
                .before(stripPrefix(1))
                .filter(tokenRelay())
                .build();
    }
}
