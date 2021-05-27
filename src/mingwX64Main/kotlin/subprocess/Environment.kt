package ktpack.subprocess

import kotlinx.cinterop.get
import kotlinx.cinterop.plus
import kotlinx.cinterop.toKString
import kotlinx.cinterop.wcstr
import platform.posix._wenviron
import platform.posix._wgetenv

private data class EnvEntry(override val key: String, override val value: String) : Map.Entry<String, String>

@ThreadLocal
actual object Environment : AbstractMap<String, String>(), Map<String, String> {

    actual val caseInsensitive: Boolean = true

    override fun containsKey(key: String): Boolean = _wgetenv(key.wcstr) != null

    override fun get(key: String): String? = _wgetenv(key.wcstr)?.toKString()

    override val entries: Set<Map.Entry<String, String>> = object : AbstractSet<Map.Entry<String, String>>() {
        override fun contains(element: Map.Entry<String, String>): Boolean = get(element.key) == element.value

        override val size: Int
            get() {
                var sz = 0
                val ep = _wenviron
                if (ep != null)
                    while (ep[sz] != null) sz++
                return sz
            }

        override fun iterator() = object : Iterator<Map.Entry<String, String>> {

            var ep = _wenviron

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
