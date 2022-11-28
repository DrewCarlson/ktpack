package ktpack.util

import ktfio.*
import ktpack.configuration.KotlinTarget

/**
 * Attempt to find the user's home directory first by the
 * platform's home directory environment variable, and if
 * not found use native platform function to find it.
 */
expect fun getHomePath(): String?

expect val workingDirectory: String

expect val TEMP_DIR: File

@SharedImmutable
val EXE_EXTENSION by lazy {
    if (Platform.osFamily == OsFamily.WINDOWS) "exe" else "kexe"
}

@SharedImmutable
val USER_HOME = checkNotNull(getHomePath()) {
    "Failed to find user home path."
}

@SharedImmutable
val KTPACK_ROOT = "${USER_HOME}$filePathSeparator.ktpack"

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

@SharedImmutable
val CPSEP = if (Platform.osFamily == OsFamily.WINDOWS) ";" else ":"

object PlatformUtils {

    fun getHostTarget(): KotlinTarget {
        return when (Platform.osFamily) {
            OsFamily.LINUX -> KotlinTarget.LINUX_X64
            OsFamily.WINDOWS -> KotlinTarget.MINGW_X64
            OsFamily.MACOSX -> if (Platform.cpuArchitecture == CpuArchitecture.ARM64) {
                KotlinTarget.MACOS_ARM64
            } else {
                KotlinTarget.MACOS_X64
            }

            else -> error("Unsupported host os: ${Platform.osFamily}")
        }
    }

    fun canHostBuildFor(target: KotlinTarget): Boolean {
        return when (target) {
            KotlinTarget.JVM,
            KotlinTarget.JS_NODE,
            KotlinTarget.JS_BROWSER,
            KotlinTarget.LINUX_ARM64,
            KotlinTarget.LINUX_X64 -> true

            KotlinTarget.MINGW_X86, KotlinTarget.MINGW_X64 -> Platform.osFamily != OsFamily.LINUX
            KotlinTarget.MACOS_ARM64, KotlinTarget.MACOS_X64 -> Platform.osFamily == OsFamily.MACOSX
        }
    }

    fun getHostSupportedTargets() = KotlinTarget.values().filter(::canHostBuildFor)
}
