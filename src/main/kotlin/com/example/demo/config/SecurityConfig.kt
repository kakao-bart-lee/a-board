package com.example.demo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.config.web.server.SecurityWebFiltersOrder

@Configuration
@EnableReactiveMethodSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/auth/token").permitAll()
                    .pathMatchers(org.springframework.http.HttpMethod.POST, "/users").permitAll()
                    .anyExchange().authenticated()
            }
            .csrf { it.disable() }
            .build()
    }
}
