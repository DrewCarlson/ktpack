package ktpack.subprocess

import kotlinx.cinterop.*
import platform.posix.*

private data class EnvEntry(override val key: String, override val value: String) : Map.Entry<String, String>

@ThreadLocal
actual object Environment : AbstractMap<String, String>(), Map<String, String> {
    actual val caseInsensitive: Boolean = false

    override fun containsKey(key: String): Boolean = getenv(key) != null

    override fun get(key: String): String? = getenv(key)?.toKString()

    override val entries: Set<Map.Entry<String, String>> = object : AbstractSet<Map.Entry<String, String>>() {
        override fun contains(element: Map.Entry<String, String>): Boolean = get(element.key) == element.value

        override val size: Int
            get() {
                var sz = 0
                val ep = __environ
                // loop until null entry
                if (ep != null)
                    while (ep[sz] != null) sz++
                return sz
            }

        override fun iterator() = object : Iterator<Map.Entry<String, String>> {

            var ep = __environ

            override fun hasNext(): Boolean {
                return ep?.get(0) != null
            }

            override fun next(): Map.Entry<String, String> {
                val cur = ep?.get(0)?.toKString() ?: throw NoSuchElementException()

                val (key, value) = cur.split('=', limit = 2)
                ep += 1
                return EnvEntry(key, value)
            }
        }
    }
}

internal fun Map<String, String>.toEnviron() = map { "${it.key}=${it.value}" }
