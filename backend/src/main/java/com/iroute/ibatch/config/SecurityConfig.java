package com.iroute.ibatch.config;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final AuthProperties authProperties;
    private final boolean secureCookies;
    private final String sameSite;
    private final boolean migrationRepair;

    public SecurityConfig(
            RateLimitFilter rateLimitFilter,
            AuthProperties authProperties,
            @Value("${server.servlet.session.cookie.secure}") boolean secureCookies,
            @Value("${server.servlet.session.cookie.same-site}") String sameSite,
            @Value("${app.auth.migration-repair:false}") boolean migrationRepair) {
        this.rateLimitFilter = rateLimitFilter;
        this.authProperties = authProperties;
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
        this.migrationRepair = migrationRepair;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite(sameSite));

        var csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        return http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository).csrfTokenRequestHandler(csrfRequestHandler))
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/login", "/auth/register", "/auth/csrf", "/api/health", "/api/health/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        response.sendError(401, "Autenticacion requerida")))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()")))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsService(DataSource dataSource, PasswordEncoder encoder) {
        if (migrationRepair) {
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0");
            jdbc.execute("CREATE TABLE IF NOT EXISTS users (username VARCHAR(100) NOT NULL PRIMARY KEY, password VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE)");
            jdbc.execute("CREATE TABLE IF NOT EXISTS authorities (username VARCHAR(100) NOT NULL, authority VARCHAR(50) NOT NULL, CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE, CONSTRAINT uk_authorities UNIQUE (username, authority))");
        }
        var manager = new JdbcUserDetailsManager(dataSource);
        if (!manager.userExists(authProperties.username())) {
            manager.createUser(User.withUsername(authProperties.username())
                    .password(encoder.encode(authProperties.password()))
                    .roles("OPERADOR")
                    .build());
        }
        return manager;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
