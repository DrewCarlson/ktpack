
fun main() {


    module("hello_world") {
        version = "1.0.0"
        kotlinVersion = "1.7.10"
        authors += "Drew Carlson <awcarlson@protonmail.com>"

        dependencies {
            local("hello_lib")
            maven("co.touchlab:kermit:1.1.3")
            maven("co.touchlab", "kermit", "1.1.3")
        }

        dependencies(Target.JS_NODE, Target.JS_BROWSER) {
            maven("co.touchlab:kermit:1.1.3")
            maven("co.touchlab", "kermit", "1.1.3")
        }
    }
}