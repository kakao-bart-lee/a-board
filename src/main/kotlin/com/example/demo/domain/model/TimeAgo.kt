package com.example.demo.domain.model

import java.time.Instant
import java.time.Duration

fun Instant.toAgoString(): String {
    val duration = Duration.between(this, Instant.now())
    val minutes = duration.toMinutes()
    return when {
        minutes < 1 -> "방금전"
        minutes < 60 -> "${minutes}분 전"
        minutes < 60 * 24 -> "${duration.toHours()}시간 전"
        else -> "${duration.toDays()}일 전"
    }
}
