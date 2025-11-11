package us.dit.muit.hsa.neurologicalassessment.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.kogito.auth.IdentityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class IdentityProviderConfig {

    @Bean
    public IdentityProvider identityProvider() {
        return new IdentityProvider() {
            @Override
            public String getName() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    return authentication.getName();
                }
                return "anonymous";
            }

            @Override
            public List<String> getRoles() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    return authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }

            @Override
            public boolean hasRole(String role) {
                return getRoles().contains(role);
            }
        };
    }
}
