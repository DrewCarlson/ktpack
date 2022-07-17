val STDERR = platform.posix.fdopen(2, "w")
fun printErr(message: String) {
    platform.posix.fprintf(STDERR, "%s\n", message)
    platform.posix.fflush(STDERR)
}
fun main() {
    repeat(5) {
        printErr("Hello, world!")
        println("Hello, world!")
        platform.posix.sleep(2)
    }
}
