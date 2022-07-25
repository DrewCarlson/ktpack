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
val USER_HOME = checkNotNull(getHomePath()) {
    "Failed to find user home path."
}

@SharedImmutable
val KONAN_ROOT = "${USER_HOME}$filePathSeparator.konan"

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
