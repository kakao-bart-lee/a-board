package com.example.demo.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID
/**
 * Ensures every session carries a random anonymous identifier. This id is
 * later embedded into JWTs so posts remain unlinkable to real user ids.
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class AnonymousIdFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return exchange.session.flatMap { session ->
            if (session.getAttribute<String>("anonId") == null) {
                session.attributes["anonId"] = UUID.randomUUID().toString()
            }
            chain.filter(exchange)
        }
    }
}
