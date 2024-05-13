package ktpack.util

import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors.brightWhite
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


suspend inline fun <R : Any> Terminal.loadingIndeterminate(
    frames: List<String> = loadingFrames,
    crossinline animate: Terminal.(frame: String, duration: Duration) -> String = { text, duration ->
        bold(brightWhite(text)) + reset(white(" ${duration.inWholeSeconds}s"))
    },
    crossinline execute: suspend () -> R,
) = coroutineScope {
    val startTime = 0L
    var currentDuration = 0L
    val animation = textAnimation<Int> { frame ->
        animate(frames[frame], currentDuration.milliseconds)
    }
    val animateJob = launch {
        var i = 0
        while (i < loadingFrames.size) {
            animation.update(i)
            if (i == loadingFrames.lastIndex) i = 0 else i++
            currentDuration = startTime + 500L
            delay(500.milliseconds)
        }
    }
    try {
        execute()
    } finally {
        animateJob.cancelAndJoin()
        animation.clear()
    }
}

val loadingFrames = listOf(
    "[    ]",
    "[=   ]",
    "[==  ]",
    "[=== ]",
    "[ ===]",
    "[  ==]",
    "[   =]",
    "[    ]",
    "[   =]",
    "[  ==]",
    "[ ===]",
    "[====]",
    "[=== ]",
    "[==  ]",
    "[=   ]",
)

val dots1 = listOf(
    "⠋",
    "⠙",
    "⠹",
    "⠸",
    "⠼",
    "⠴",
    "⠦",
    "⠧",
    "⠇",
    "⠏"
)

val dots2 = listOf(
    "⠋",
    "⠙",
    "⠚",
    "⠞",
    "⠖",
    "⠦",
    "⠴",
    "⠲",
    "⠳",
    "⠓"
)
