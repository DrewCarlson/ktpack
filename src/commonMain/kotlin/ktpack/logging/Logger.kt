package ktpack.logging

interface Logger {

    fun info(message: String, vararg objects: Any)

    fun debug(message: String, vararg objects: Any)

    fun trace(message: String, vararg objects: Any)

    fun warn(message: String, vararg objects: Any)

    fun error(message: String, vararg objects: Any)
}
