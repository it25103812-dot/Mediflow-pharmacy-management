package com.mediflow.security;

import com.mediflow.entity.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint entryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          @org.springframework.beans.factory.annotation.Value("${mediflow.cors.allowed-origins}")
                          String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/login", "/api/auth/health").permitAll()
                    // ----- Module-level role rules (anyRequest MUST stay last) -----
                    .requestMatchers("/api/users/**").hasRole(Role.ADMIN)
                    .requestMatchers("/api/audit-logs/**").hasRole(Role.ADMIN)
                    .requestMatchers(HttpMethod.POST, "/api/medicines/**", "/api/categories/**").hasAnyRole(Role.ADMIN, Role.PHARMACIST)
                    .requestMatchers(HttpMethod.PUT, "/api/medicines/**", "/api/categories/**").hasAnyRole(Role.ADMIN, Role.PHARMACIST)
                    .requestMatchers(HttpMethod.DELETE, "/api/medicines/**", "/api/categories/**").hasRole(Role.ADMIN)
                    // Cashiers need read-only batch info for the POS (stock + expiry checks)
                    .requestMatchers(HttpMethod.GET, "/api/batches/**").hasAnyRole(Role.ADMIN, Role.PHARMACIST, Role.STORE_KEEPER, Role.CASHIER)
                    .requestMatchers("/api/inventory/**", "/api/batches/**").hasAnyRole(Role.ADMIN, Role.PHARMACIST, Role.STORE_KEEPER)
                    .requestMatchers("/api/suppliers/**", "/api/purchase-orders/**", "/api/grn/**").hasAnyRole(Role.ADMIN, Role.PROCUREMENT_OFFICER, Role.STORE_KEEPER)
                    // Only Finance Manager (and Admin) may edit / delete bills
                    .requestMatchers(HttpMethod.PUT, "/api/sales/**").hasAnyRole(Role.ADMIN, Role.FINANCE_MANAGER)
                    .requestMatchers(HttpMethod.DELETE, "/api/sales/**").hasAnyRole(Role.ADMIN, Role.FINANCE_MANAGER)
                    .requestMatchers("/api/sales/**").hasAnyRole(Role.ADMIN, Role.CASHIER, Role.PHARMACIST, Role.FINANCE_MANAGER)
                    // Pharmacists look customers up when recording prescriptions (read-only)
                    .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole(Role.ADMIN, Role.CASHIER, Role.CRO, Role.PHARMACIST)
                    .requestMatchers("/api/customers/**").hasAnyRole(Role.ADMIN, Role.CASHIER, Role.CRO)
                    .requestMatchers("/api/prescriptions/**").hasAnyRole(Role.ADMIN, Role.PHARMACIST, Role.CASHIER)
                    .requestMatchers("/api/reports/**").hasAnyRole(Role.ADMIN, Role.FINANCE_MANAGER, Role.PHARMACIST, Role.STORE_KEEPER, Role.PROCUREMENT_OFFICER)
                    // Everything else requires a valid token
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
