package us.dit.muit.hsa.neurologicalassessment.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.kogito.auth.IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class IdentityProviderConfig {

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderConfig.class);

    @Bean
    public IdentityProvider identityProvider() {
        return new IdentityProvider() {
            @Override
            public String getName() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    String name = authentication.getName();
                    logger.debug("IdentityProvider.getName() returning: {}", name);
                    return name;
                }
                logger.debug("IdentityProvider.getName() returning: anonymous");
                return "anonymous";
            }

            @Override
            public List<String> getRoles() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    List<String> roles = authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                            .collect(Collectors.toList());
                    logger.debug("IdentityProvider.getRoles() for user {}: {}", getName(), roles);
                    return roles;
                }
                logger.debug("IdentityProvider.getRoles() returning: empty list");
                return Collections.emptyList();
            }

            @Override
            public boolean hasRole(String role) {
                boolean has = getRoles().contains(role);
                logger.debug("IdentityProvider.hasRole({}) for user {}: {}", role, getName(), has);
                return has;
            }
        };
    }
}