package com.example.marketplace.config;

import com.example.marketplace.security.JwtAuthenticationWebFilter;
import com.example.marketplace.security.JwtTokenProvider;
import com.example.marketplace.user.application.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      JwtTokenProvider jwtTokenProvider,
      UserService userService
  ) {
    JwtAuthenticationWebFilter jwtAuthenticationWebFilter = new JwtAuthenticationWebFilter(
        jwtTokenProvider,
        userService
    );

    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
        )
        .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
            .pathMatchers(HttpMethod.GET,
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/webjars/swagger-ui/**"
            ).permitAll()
            .pathMatchers(HttpMethod.POST,
                "/api/v1/auth/register/customer",
                "/api/v1/auth/register/seller",
                "/api/v1/auth/login"
            ).permitAll()
            .pathMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
            .pathMatchers("/api/v1/seller/**").hasRole("SELLER")
            .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .pathMatchers(HttpMethod.GET,
                "/api/v1/users/me",
                "/api/v1/products/**",
                "/api/v1/categories/**"
            ).authenticated()
            .anyExchange().authenticated()
        )
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
