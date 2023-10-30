package ktpack.util

import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


suspend inline fun <R : Any> Terminal.loadingIndeterminate(
    frames: List<String> = loadingFrames,
    crossinline animate: Terminal.(frame: String, duration: Duration) -> String,
    crossinline execute: suspend () -> R,
) = coroutineScope {
    this@loadingIndeterminate.println("")
    val startTime = Clock.System.now()
    var currentDuration = Duration.ZERO
    val animation = textAnimation<Int> { frame ->
        animate(frames[frame], currentDuration)
    }
    val animateJob = launch {
        var i = 0
        while (i < loadingFrames.size) {
            animation.update(i)
            if (i == loadingFrames.lastIndex) i = 0 else i++
            currentDuration = Clock.System.now() - startTime
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
