package org.example.authservice;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.io.File;

@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Value("${auth.client-secret}")
    private String clientSecret;

    @Value("${auth.demo-password}")
    private String demoPassword;

    @Value("${auth.jwk-file:jwk.json}")
    private String jwkFilePath;

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("gateway-client")
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/authservice")
                .scopes(scopes -> scopes.addAll(
                        Set.of("user.read", "user.write",
                                OidcScopes.OPENID,
                                OidcScopes.PROFILE)))
                .tokenSettings(TokenSettings.builder()
                        .reuseRefreshTokens(false) // Rotation för säkerhet
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://127.0.0.1:9000")
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username("demo")
                .password(passwordEncoder.encode(demoPassword))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = loadOrGenerateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private RSAKey loadOrGenerateRsa() {
        File jwkFile = new File(jwkFilePath);

        if (jwkFile.exists()) {
            try {
                String json = Files.readString(jwkFile.toPath());
                JWKSet jwkSet = JWKSet.parse(json);
                log.info("Läste in RSA-nyckel från {}", jwkFilePath);
                return (RSAKey) jwkSet.getKeys().get(0);
            } catch (Exception e) {
                log.warn("Kunde inte läsa JWK-fil, genererar ny: {}", e.getMessage());
            }
        }

        RSAKey rsaKey = generateRsa();

        try {
            Files.writeString(jwkFile.toPath(), new JWKSet(rsaKey).toString(false));
            log.info("Ny RSA-nyckel sparad till {}", jwkFilePath);
        } catch (Exception e) {
            log.warn("Kunde inte spara JWK-fil: {}", e.getMessage());
        }

        return rsaKey;
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // Standard säkerhetsnivå
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}
