package ktpack.util

import co.touchlab.kermit.Logger

inline fun <reified T : Any> Logger.Companion.forClass(): Logger {
    return Logger.withTag(
        requireNotNull(T::class.simpleName) {
            "Failed to create logger for unnamed class ${T::class}"
        }
    )
}
