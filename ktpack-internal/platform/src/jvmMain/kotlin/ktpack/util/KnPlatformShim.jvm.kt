package ktpack.util

actual enum class OsFamily {
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

actual enum class CpuArchitecture(
    actual val bitness: Int
) {
    UNKNOWN(-1),
    ARM32(32),
    ARM64(64),
    X86(32),
    X64(64),
    MIPS32(32),
    MIPSEL32(32),
    WASM32(32);
}

actual object Platform {
    actual val osFamily: OsFamily
        get() {
            val os = System.getProperty("os.name")
            return when {
                os.contains("win", ignoreCase = true) -> OsFamily.WINDOWS
                os.contains("mac", ignoreCase = true) -> OsFamily.MACOSX
                else -> OsFamily.LINUX
            }
        }

    actual val cpuArchitecture: CpuArchitecture
        get() {
            val arch = System.getProperty("os.arch")
            return when {
                arch.contains("aarch64") -> CpuArchitecture.ARM64
                else -> CpuArchitecture.X64
            }
        }

    actual fun getAvailableProcessors(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
}
