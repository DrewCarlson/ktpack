package ktpack.util

import ktfio.filePathSeparator

/**
 * Attempt to find the user's home directory first by the
 * platform's home directory environment variable, and if
 * not found use native platform function to find it.
 */
expect fun getHomePath(): String?

@SharedImmutable
val EXE_EXTENSION by lazy {
    if (Platform.osFamily == OsFamily.WINDOWS) "exe" else "kexe"
}

@SharedImmutable
val USER_HOME by lazy {
    checkNotNull(getHomePath()) {
        "Failed to find user home path."
    }
}

@SharedImmutable
val KONAN_ROOT by lazy {
    "${USER_HOME}$filePathSeparator.konan"
}

@SharedImmutable
val KOTLINC_BIN_NAME by lazy {
    if (Platform.osFamily == OsFamily.WINDOWS) {
        "kotlinc-native.bat"
    } else {
        "kotlinc-native"
    }
}

@SharedImmutable
val ARCH by lazy {
    when (Platform.cpuArchitecture) {
        CpuArchitecture.ARM64 -> if (Platform.osFamily == OsFamily.MACOSX) {
            "aarch64"
        } else {
            error("Unsupported Host ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        CpuArchitecture.X86,
        CpuArchitecture.X64 -> "x86_64"
        else -> error("Unsupported Host ${Platform.osFamily} ${Platform.cpuArchitecture}")
    }
}

fun findKotlincNative(version: String): String {
    val (major, minor, _) = version.split('.').map(String::toInt)
    return buildString {
        append(KONAN_ROOT)
        append(filePathSeparator)
        append("kotlin-native-prebuilt-")
        when (Platform.osFamily) {
            OsFamily.MACOSX -> "macos"
            OsFamily.WINDOWS -> "windows"
            OsFamily.LINUX -> "linux"
            else -> error("Unsupported Host ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }.run(::append)
        append('-')
        if (major == 1 && minor >= 6) {
            append(ARCH)
            append('-')
        }
        append(version)
        append(filePathSeparator)
        append("bin")
        append(filePathSeparator)
        append(KOTLINC_BIN_NAME)
    }
}
