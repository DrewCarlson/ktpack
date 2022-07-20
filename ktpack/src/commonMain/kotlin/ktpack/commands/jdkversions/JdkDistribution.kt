package ktpack.commands.jdkversions

enum class JdkDistribution {
    Zulu(), // https://cdn.azul.com/zulu/bin/
    Temurin(), // https://github.com/adoptium/temurin<MAJOR>-binaries
    Corretto(), // https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html
}
