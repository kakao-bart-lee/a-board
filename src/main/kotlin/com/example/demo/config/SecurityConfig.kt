package com.example.demo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableReactiveMethodSecurity
class SecurityConfig {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { }
            .csrf { csrf -> csrf.disable() }
            .build()
    }
}
