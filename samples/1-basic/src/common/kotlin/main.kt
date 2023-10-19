@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main() {
    repeat(5) {
        val message = "Hello, world! $it\n"
        print(message)
        platform.posix.fprintf(platform.posix.stderr, message)
    }
}

expect fun test(): String
actual fun test(): String = "hello"
