package ec.mindalai.managementjsf.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.csrf(csrf->csrf.disable()).authorizeHttpRequests(auth->auth.requestMatchers("/javax.faces.resource/**","/jakarta.faces.resource/**","/resources/**","/actuator/**","/login/**").permitAll().anyRequest().permitAll());
        return http.build();
    }
}
