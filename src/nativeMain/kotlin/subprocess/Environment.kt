package ktpack.subprocess

expect object Environment : Map<String, String> {
    val caseInsensitive: Boolean
}

fun Environment.validateKey(key: String) {
    require(!('=' in key || '\u0000' in key)) { "Invalid environment key: $key" }
}

fun Environment.validateValue(value: String) {
    require('\u0000' !in value) { "Invalid environment value: $value" }
}

private typealias EBEntryIF = MutableMap.MutableEntry<String, String>

class EnvironmentBuilder(init: Map<String, String> = Environment) : AbstractMutableMap<String, String>() {

    private val backing = mutableMapOf<EnvKey, String>()

    init {
        if (init == Environment) {
            init.forEach { (key, value) ->
                if ('=' !in key)
                    put(key, value)
            }
        } else {
            if (init !is EnvironmentBuilder) {
                init.forEach { (key, value) ->
                    Environment.validateKey(key)
                    Environment.validateValue(value)
                }
            }
            putAll(init)
        }
    }

    override fun containsKey(key: String) = backing.containsKey(EnvKey(key))

    override fun containsValue(value: String) = backing.containsValue(value)
    override fun get(key: String) = backing[EnvKey(key)]

    override fun remove(key: String) = backing.remove(EnvKey(key))

    override fun put(key: String, value: String): String? {
        Environment.validateKey(key)
        Environment.validateValue(value)
        return backing.put(EnvKey(key), value)
    }

    override val entries: MutableSet<EBEntryIF> = object : AbstractMutableSet<EBEntryIF>() {
        override val size: Int
            get() = backing.size

        override fun contains(element: EBEntryIF): Boolean = backing.entries.contains(EnvKeyEntry(element))

        override fun add(element: EBEntryIF) = put(element.key, element.value) != element.value

        override fun remove(element: EBEntryIF) = backing.entries.remove(EnvKeyEntry(element))

        override fun iterator() = object : MutableIterator<EBEntryIF> {
            val wrapped = backing.iterator()

            override fun next() = EnvEntry(wrapped.next())

            override fun hasNext() = wrapped.hasNext()
            override fun remove() = wrapped.remove()

        }

    }

    private class EnvKey(val value: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as EnvKey

            return value.compareTo(other.value, Environment.caseInsensitive) == 0
        }

        override fun hashCode(): Int {
            return if (Environment.caseInsensitive) value.uppercase().hashCode() else value.hashCode()
        }
    }

    private data class EnvEntry(val wrapped: MutableMap.MutableEntry<EnvKey, String>) : EBEntryIF {
        override val key: String
            get() = wrapped.key.value
        override val value: String
            get() = wrapped.value

        override fun setValue(newValue: String): String {
            Environment.validateValue(newValue)
            return wrapped.setValue(newValue)
        }
    }

    private data class EnvKeyEntry(
        override val key: EnvKey,
        override var value: String
    ) : MutableMap.MutableEntry<EnvKey, String> {
        override fun setValue(newValue: String): String = value.also {
            value = newValue
        }

        constructor(outer: EBEntryIF) : this(EnvKey(outer.key), outer.value)
    }
}
