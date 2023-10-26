package ktpack.util

expect enum class OsFamily {
    UNKNOWN,
    MACOSX,
    IOS,
    LINUX,
    WINDOWS,
    ANDROID,
    WASM,
    TVOS,
    WATCHOS
}

expect enum class CpuArchitecture {
    UNKNOWN,
    ARM32,
    ARM64,
    X86,
    X64,
    MIPS32,
    MIPSEL32,
    WASM32;

    val bitness: Int
}

expect object Platform {

    val osFamily: OsFamily

    val cpuArchitecture: CpuArchitecture

    fun getAvailableProcessors() : Int
}
