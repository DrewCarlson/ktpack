package ktpack.util

import kotlinx.cinterop.toKString
import ktfio.filePathSeparator
import platform.posix.getenv

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
val KONAN_ROOT = "${USER_HOME}${filePathSeparator}.konan"

@SharedImmutable
val KTPACK_ROOT = "${USER_HOME}${filePathSeparator}.ktpack"

@SharedImmutable
val TEMP_DIR by lazy {
    checkNotNull(
        (getenv("TEMP")?.toKString()
            ?: getenv("TMPDIR")?.toKString()
            ?: "/tmp".takeIf { Platform.osFamily == OsFamily.LINUX })
            ?.takeUnless(String::isBlank)
    ) { "TEMP, TMPDIR env variables is missing, unable to find temp directory" }
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
