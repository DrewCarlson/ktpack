package ktpack.logging

import com.github.ajalt.mordant.rendering.TextColors

data class LogContext(
    val tag: String,
    val tagColor: TextColors,
)
