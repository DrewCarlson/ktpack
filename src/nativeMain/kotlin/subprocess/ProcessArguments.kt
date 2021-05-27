package ktpack.subprocess


open class ProcessArgumentBuilder {

    val arguments: MutableList<String> = mutableListOf()

    var workingDirectory: String? = null

    private var environmentOrNull: MutableMap<String, String>? = null

    val environment: MutableMap<String, String>
        get() = environmentOrNull ?: EnvironmentBuilder().also { environmentOrNull = it }

    val isEnvironmentModified: Boolean
        get() = environmentOrNull != null

    var stdin: Redirect = Redirect.Pipe
        set(value) {
            require(!(value is Redirect.Write || value is Redirect.Stdout)) { "Unsupported redirect for stdin: $value" }
            field = value
        }

    var stdout: Redirect = Redirect.Pipe
        set(value) {
            require(!(value is Redirect.Read || value is Redirect.Stdout)) { "Unsupported redirect for stdout: $value" }
            field = value
        }

    var stderr: Redirect = Redirect.Pipe
        set(value) {
            require(value !is Redirect.Read) { "Unsupported redirect for stderr: $value" }
            field = value
        }

    fun arg(arg: String) {
        arguments.add(arg)
    }

    fun stdin(file: String) {
        stdin = Redirect.Read(file)
    }

    fun stdout(file: String, append: Boolean = false) {
        stdout = Redirect.Write(file, append)
    }

    fun stderr(file: String, append: Boolean = false) {
        stderr = Redirect.Write(file, append)
    }

    fun build() = ProcessArguments(
        arguments,
        workingDirectory,
        environmentOrNull,
        stdin,
        stdout,
        stderr
    )
}

class ProcessArguments(
    arguments: Iterable<String>,
    val workingDirectory: String? = null,
    environment: Map<String, String>? = null,
    val stdin: Redirect = Redirect.Pipe,
    val stdout: Redirect = Redirect.Pipe,
    val stderr: Redirect = Redirect.Pipe
) {

    val arguments = arguments.toList()
    val environment: Map<String, String>? = environment?.let { EnvironmentBuilder(it) }

    init {
        require(this.arguments.isNotEmpty()) { "The argument list must have at least one element!" }
    }
}

inline fun ProcessArguments(builder: ProcessArgumentBuilder.() -> Unit) =
    ProcessArgumentBuilder().apply(builder).build()

sealed class Redirect {

    object Null : Redirect() {
        override fun toString() = "discard"
    }

    object Pipe : Redirect() {
        override fun toString() = "pipe"
    }

    object Inherit : Redirect() {
        override fun toString() = "inherit"
    }

    object Stdout : Redirect() {
        override fun toString() = "stdout"
    }

    class Read(val file: String) : Redirect() {
        override fun toString() = "read from $file"
    }

    class Write(val file: String, val append: Boolean = false) : Redirect() {
        override fun toString() = buildString {
            append(if (append) "append to " else "write to ")
            append(file)
        }
    }
}
