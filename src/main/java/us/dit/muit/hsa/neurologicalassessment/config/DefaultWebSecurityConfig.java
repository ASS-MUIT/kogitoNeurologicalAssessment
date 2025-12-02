package us.dit.muit.hsa.neurologicalassessment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class DefaultWebSecurityConfig {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/**").authenticated()
                .and()
                .httpBasic();

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // Nota: En Kogito, los grupos (GroupId) no llevan el prefijo ROLE_
        // Por eso usamos .authorities() en lugar de .roles()
        auth.inMemoryAuthentication()
                .withUser("doctorWho").password("doctorWho")
                .authorities("practitioner");

        auth.inMemoryAuthentication()
                .withUser("mary").password("mary")
                .authorities("patient");

        auth.inMemoryAuthentication()
                .withUser("paul").password("paul")
                .authorities("practitioner");
        auth.inMemoryAuthentication()
                .withUser("wbadmin").password("wbadmin")
                .authorities("rest-admin");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}