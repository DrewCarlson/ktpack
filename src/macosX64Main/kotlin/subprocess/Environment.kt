package ktpack.subprocess

import kotlinx.cinterop.*
import platform.osx.*
import platform.posix.*

private data class EnvEntry(override val key: String, override val value: String) : Map.Entry<String, String>

actual object Environment : AbstractMap<String, String>(), Map<String, String> {

    actual val caseInsensitive: Boolean = false

    override fun containsKey(key: String): Boolean = getenv(key) != null

    override fun get(key: String): String? = getenv(key)?.toKString()

    override val entries: Set<Map.Entry<String, String>> = object : AbstractSet<Map.Entry<String, String>>() {
        override fun contains(element: Map.Entry<String, String>): Boolean =
            get(element.key) == element.value

        override val size: Int
            get() {
                var sz = 0
                val ep = _NSGetEnviron()
                if (ep != null)
                    while (ep[sz] != null) sz++
                return sz
            }

        override fun iterator() = object : Iterator<Map.Entry<String, String>> {

            var ep = _NSGetEnviron()

            override fun hasNext(): Boolean {
                return ep?.get(0) != null
            }

            override fun next(): Map.Entry<String, String> {
                val cur = ep?.pointed?.value?.get(0)?.toKString() ?: throw NoSuchElementException()

                val (key, value) = cur.split('=', limit = 2)
                ep += 1
                return EnvEntry(key, value)
            }
        }
    }

}
