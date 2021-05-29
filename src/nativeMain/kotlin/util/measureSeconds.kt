package ktpack.util

import kotlin.math.*
import kotlin.time.*

inline fun measureSeconds(maxDigits: Int = 2, block: () -> Unit): Double {
    val duration = TimeSource.Monotonic.measureTime(block).toDouble(DurationUnit.SECONDS)
    val factor = 10.0.pow(maxDigits.toDouble())
    return (duration * factor).roundToInt() / factor
}
