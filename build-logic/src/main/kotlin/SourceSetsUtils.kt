@file:Suppress("unused")

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


private val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

fun NamedDomainObjectContainer<KotlinSourceSet>.jvmMain(
    configure: KotlinSourceSet.() -> Unit,
) {
    named("jvmMain", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.jvmTest(
    configure: KotlinSourceSet.() -> Unit,
) {
    named("jvmTest", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.posixMain(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (hostOs.isWindows) {
        return
    }
    named("posixMain", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.windowsMain(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isWindows) {
        return
    }
    named("windowsX64Main", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.windowsTest(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isWindows) {
        return
    }
    named("windowsX64Main", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.linuxMain(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isLinux) {
        return
    }
    named("linuxX64Main", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.linuxTest(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isLinux) {
        return
    }
    named("linuxX64Test", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.darwinMain(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isMacOsX) {
        return
    }
    named("darwinMain", configure)
}

fun NamedDomainObjectContainer<KotlinSourceSet>.darwinTest(
    configure: KotlinSourceSet.() -> Unit,
) {
    if (!hostOs.isMacOsX) {
        return
    }
    named("darwinTest", configure)
}

