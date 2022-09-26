
fun main() {
    repeat(5) {
        println("Hello, world!")
    }
}

expect fun test(): String
actual fun test(): String = "hello"