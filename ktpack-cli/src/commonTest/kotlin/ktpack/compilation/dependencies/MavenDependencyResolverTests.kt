package ktpack.compilation.dependencies

import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import ktpack.MANIFEST_FILENAME
import ktpack.TestCliContext
import ktpack.compilation.ModuleBuilder
import ktpack.compilation.dependencies.models.resolveAndFlatten
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ManifestToml
import ktpack.sampleDir
import okio.Path.Companion.DIRECTORY_SEPARATOR
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private val timeout = 2.minutes

class MavenDependencyResolverTests {

    private lateinit var module: ManifestToml
    private lateinit var context: TestCliContext
    private lateinit var builder: ModuleBuilder
    private lateinit var resolver: MavenDependencyResolver

    @BeforeTest
    fun setup() = runTest {
        val sampleRoot = Path(sampleDir, "6-dependencies")
        val packScript = Path(sampleRoot, MANIFEST_FILENAME)
        context = TestCliContext()
        module = context.loadManifestToml(packScript.toString())
        builder = ModuleBuilder(
            module,
            context,
            modulePath = sampleRoot,
        )
        resolver = MavenDependencyResolver(module, context.http)
    }

    @Test
    fun test_js_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveRootDependencyTree(listOf(KotlinTarget.JS_BROWSER))

        val flatTree = depTree.resolveAndFlatten()
        val children = resolver.resolveArtifacts(flatTree, releaseMode = false, target = KotlinTarget.JS_BROWSER)
        val actual = children
            .flatMap { it.artifacts }
            .map { it.substringAfterLast(DIRECTORY_SEPARATOR) }

        val expected = listOf(
            "kotlinx-atomicfu-runtime-1.8.20.klib",
            "atomicfu-jsir-0.20.2.klib",
            "kotlinx-coroutines-core-jsir-1.7.1.klib",
            "kotlinx-serialization-core-jsir-1.3.3.klib",
            "kotlinx-serialization-json-jsir-1.3.3.klib",
            "kotlin-dom-api-compat-1.9.22.klib",
            "ktor-io-js-2.3.8.klib",
            "ktor-utils-js-2.3.8.klib",
            "ktor-http-js-2.3.8.klib",
            "ktor-events-js-2.3.8.klib",
            "ktor-websockets-js-2.3.8.klib",
            "ktor-serialization-js-2.3.8.klib",
            "ktor-websocket-serialization-js-2.3.8.klib",
            "ktor-client-core-js-2.3.8.klib",
            "kermit-js-2.0.3.klib",
            "kermit-core-js-2.0.3.klib",
            "atomicfu-1.6.21.jar",
            "ktor-client-content-negotiation-jsir-2.0.3.klib",
            "ktor-serialization-kotlinx-jsir-2.0.3.klib",
            "ktor-serialization-kotlinx-json-jsir-2.0.3.klib",
            "coingecko-js-1.0.0-beta02.klib",
            "ktor-client-js-js-2.3.8.klib",
        )
        expected.forEach { assertTrue(actual.contains(it), "missing from actual: $it") }
        actual.forEach { assertTrue(expected.contains(it), "missing from expect: $it") }
    }

    @Test
    fun test_jvm_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveRootDependencyTree(listOf(KotlinTarget.JVM))

        val flatTree = depTree.resolveAndFlatten()
        val children = resolver.resolveArtifacts(flatTree, releaseMode = false, target = KotlinTarget.JVM)
        val actual = children
            .flatMap { it.artifacts }
            .map { it.substringAfterLast(DIRECTORY_SEPARATOR) }

        val expected = listOf(
            "annotations-23.0.0.jar",
            "kotlinx-coroutines-core-jvm-1.7.1.jar",
            "kotlinx-serialization-core-jvm-1.3.3.jar",
            "kotlinx-serialization-json-jvm-1.3.3.jar",
            "kotlinx-coroutines-jdk8-1.7.1.jar",
            "slf4j-api-1.7.36.jar",
            "ktor-io-jvm-2.3.8.jar",
            "ktor-utils-jvm-2.3.8.jar",
            "ktor-http-jvm-2.3.8.jar",
            "ktor-events-jvm-2.3.8.jar",
            "ktor-websockets-jvm-2.3.8.jar",
            "ktor-serialization-jvm-2.3.8.jar",
            "ktor-websocket-serialization-jvm-2.3.8.jar",
            "kotlinx-coroutines-slf4j-1.7.1.jar",
            "ktor-client-core-jvm-2.3.8.jar",
            "ktor-client-content-negotiation-jvm-2.0.3.jar",
            "ktor-serialization-kotlinx-jvm-2.0.3.jar",
            "ktor-serialization-kotlinx-json-jvm-2.0.3.jar",
            "coingecko-jvm-1.0.0-beta02.jar",
            "kermit-jvm-2.0.3.jar",
            "kermit-core-jvm-2.0.3.jar",
            "ktor-network-jvm-2.3.8.jar",
            "ktor-http-cio-jvm-2.3.8.jar",
            "ktor-network-tls-jvm-2.3.8.jar",
            "ktor-client-cio-jvm-2.3.8.jar",
        )
        expected.forEach { assertTrue(actual.contains(it), "missing from actual: $it") }
        actual.forEach { assertTrue(expected.contains(it), "missing from expect: $it") }
    }

    @Test
    fun test_mingwX64_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveRootDependencyTree(listOf(KotlinTarget.MINGW_X64))

        val flatTree = depTree.resolveAndFlatten()
        val children = resolver.resolveArtifacts(flatTree, releaseMode = false, target = KotlinTarget.MINGW_X64)
        val actual = children
            .flatMap { it.artifacts }
            .map { it.substringAfterLast(DIRECTORY_SEPARATOR) }

        val expected = listOf(
            "atomicfu.klib",
            "atomicfu-cinterop-interop.klib",
            "kotlinx-coroutines-core.klib",
            "kotlinx-serialization-core.klib",
            "kotlinx-serialization-json.klib",
            "ktor-io.klib",
            "ktor-utils.klib",
            "ktor-http.klib",
            "ktor-events.klib",
            "ktor-websockets.klib",
            "ktor-serialization.klib",
            "ktor-websocket-serialization.klib",
            "ktor-client-core.klib",
            "ktor-client-content-negotiation.klib",
            "ktor-serialization-kotlinx.klib",
            "ktor-serialization-kotlinx-json.klib",
            "coingecko.klib",
            "kermit.klib",
            "kermit-core.klib",
            "ktor-http-cio.klib",
            "ktor-client-winhttp.klib",
            "ktor-client-winhttp-cinterop-winhttp.klib",
        )
        expected.forEach { assertTrue(actual.contains(it), "missing from actual: $it") }
        actual.forEach { assertTrue(expected.contains(it), "missing from expect: $it") }
    }
}
