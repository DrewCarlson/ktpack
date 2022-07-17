package ktpack.util

import kotlin.math.*
import kotlin.time.*

@OptIn(ExperimentalTime::class)
inline fun <T> measureSeconds(maxDigits: Int = 2, block: () -> T): Pair<T, Double> {
    val (result, duration) = TimeSource.Monotonic.measureTimedValue(block)
    val factor = 10.0.pow(maxDigits.toDouble())
    return result to (duration.toDouble(DurationUnit.SECONDS) * factor).roundToInt() / factor
}
