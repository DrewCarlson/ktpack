package ktpack.util

import ktpack.CliContext
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf


/**
 * Given a non-null [requestedTarget], ensure that [hostTarget]
 * supports building for the target or return null.  When no
 * target is requested ([requestedTarget] is null), return a
 * target supported by [hostTarget] or null.
 */
fun ModuleConf.validateTargetOrAlternative(
    context: CliContext,
    requestedTarget: KotlinTarget?,
): KotlinTarget? {
    return if (requestedTarget == null) {
        val hostTarget = PlatformUtils.getHostTarget()
        if (targets.isEmpty() || targets.contains(hostTarget)) {
            hostTarget
        } else {
            targets.firstOrNull { it == KotlinTarget.JS_BROWSER || it == KotlinTarget.JS_NODE || it == KotlinTarget.JVM }
        } ?: error("No supported build targets.")
    } else {
        if (targets.isEmpty() || targets.contains(requestedTarget)) {
            requestedTarget
        } else {
            context.term.println("${failed("Error")} Selected target '$requestedTarget' but choices are '${targets.joinToString()}'")
            null
        }
    }
}
