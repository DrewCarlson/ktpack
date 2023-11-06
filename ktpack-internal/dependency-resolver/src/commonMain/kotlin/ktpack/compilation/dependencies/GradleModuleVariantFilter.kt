package ktpack.compilation.dependencies

import ktpack.configuration.KotlinTarget
import ktpack.gradle.GradleModule

// TODO: Native and js target selection is mostly accurate, but JVM
//   modules can have various attributes that are not properly evaluated.
internal fun List<GradleModule.Variant>.findVariantFor(
    target: KotlinTarget,
): GradleModule.Variant {
    val match = if (target.isNative) {
        val knTarget = target.name.lowercase()
        firstOrNull { variant ->
            variant.attributes?.run {
                orgJetbrainsKotlinPlatformType == "native" && orgJetbrainsKotlinNativeTarget == knTarget
            } ?: false
        }
    } else if (target == KotlinTarget.JVM) {
        firstOrNull { variant ->
            variant.attributes?.run {
                orgGradleUsage == "java-runtime" &&
                        orgJetbrainsKotlinPlatformType == "jvm" /*&&
                        orgGradleLibraryElements == "jar"*/
            } ?: false
        } ?: firstOrNull { variant ->
            variant.attributes?.run {
                orgGradleUsage == "java-runtime" &&
                        orgGradleLibraryElements == "jar"
            } ?: false
        } ?: firstOrNull { variant ->
            variant.attributes?.orgGradleUsage == "java-runtime"
        }
    } else {
        firstOrNull { variant ->
            variant.attributes?.run {
                orgGradleUsage == "kotlin-runtime" &&
                        orgJetbrainsKotlinJsCompiler == "ir" &&
                        orgJetbrainsKotlinPlatformType == "js"
            } ?: false
        } ?: firstOrNull { variant ->
            variant.attributes?.run {
                orgGradleUsage == "kotlin-runtime" &&
                        orgJetbrainsKotlinPlatformType == "js"
            } ?: false
        }
    }

    return checkNotNull(match) {
        val attributes = joinToString("\n") { it.attributes.toString() }
        "Failed to resolve variant of (${this.firstOrNull()?.name}) for $target:\n$attributes"
    }
}
