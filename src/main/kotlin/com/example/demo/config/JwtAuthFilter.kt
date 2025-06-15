package com.example.demo.config

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
/**
 * Extracts and verifies the JWT token from incoming requests. When valid,
 * a JwtAuthenticationToken is placed into the reactive security context so
 * downstream handlers can access the authenticated user.
 */

class JwtAuthenticationToken(val userId: String, val role: String, val anonId: String) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority(role))) {
    init { super.setAuthenticated(true) }
    override fun getCredentials(): Any? = null
    override fun getPrincipal(): Any = userId
}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class JwtAuthFilter(private val jwtService: JwtService) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val header = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return chain.filter(exchange)
        if (!header.startsWith("Bearer ")) return chain.filter(exchange)
        val token = header.substring(7)
        val decoded = jwtService.verify(token) ?: return chain.filter(exchange)
        val auth = JwtAuthenticationToken(decoded.subject, decoded.getClaim("role").asString(), decoded.getClaim("anon").asString())
        return chain.filter(exchange).contextWrite(
            org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication(auth)
        )
    }
}
