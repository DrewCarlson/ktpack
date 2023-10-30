package ktpack.compilation.dependencies

import kotlinx.coroutines.test.runTest
import ktpack.PACK_SCRIPT_FILENAME
import ktpack.TestCliContext
import ktpack.compilation.ModuleBuilder
import ktpack.compilation.dependencies.models.shakeAndFlattenDependencies
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.sampleDir
import okio.Path.Companion.DIRECTORY_SEPARATOR
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

private val timeout = 2.minutes

class MavenDependencyResolverTests {

    private lateinit var module: ModuleConf
    private lateinit var context: TestCliContext
    private lateinit var builder: ModuleBuilder
    private lateinit var resolver: MavenDependencyResolver

    @BeforeTest
    fun setup() = runTest {
        val sampleRoot = sampleDir / "6-dependencies"
        val packScript = sampleRoot / PACK_SCRIPT_FILENAME
        context = TestCliContext()
        module = context.loadKtpackConf(packScript.toString()).module
        builder = ModuleBuilder(
            module,
            context,
            modulePath = sampleRoot,
        )
        resolver = MavenDependencyResolver(module, context.http)
    }

    @Test
    fun test_js_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveDependencyTree(
            rootModule = module,
            rootFolder = sampleDir / "6-dependencies",
            targets = listOf(KotlinTarget.JS_BROWSER),
        )

        val flatTree = depTree.shakeAndFlattenDependencies()
        val children = resolver.resolveArtifacts(flatTree, releaseMode = false, target = KotlinTarget.JS_BROWSER)
        val actual = children
            .flatMap { it.artifacts }
            .map { it.substringAfterLast(DIRECTORY_SEPARATOR) }

        val expected = listOf(
            "kotlinx-atomicfu-runtime-1.9.10.klib",
            "atomicfu-jsir-0.20.2.klib",
            "kotlinx-coroutines-core-jsir-1.7.1.klib",
            "kotlinx-serialization-core-jsir-1.3.3.klib",
            "kotlinx-serialization-json-jsir-1.3.3.klib",
            "kotlin-dom-api-compat-1.9.10.klib",
            "ktor-io-js-2.3.5.klib",
            "ktor-utils-js-2.3.5.klib",
            "ktor-http-js-2.3.5.klib",
            "ktor-events-js-2.3.5.klib",
            "ktor-websockets-js-2.3.5.klib",
            "ktor-serialization-js-2.3.5.klib",
            "ktor-websocket-serialization-js-2.3.5.klib",
            "ktor-client-core-js-2.3.5.klib",
            "atomicfu-1.6.21.jar",
            "ktor-client-content-negotiation-jsir-2.0.3.klib",
            "ktor-serialization-kotlinx-jsir-2.0.3.klib",
            "ktor-serialization-kotlinx-json-jsir-2.0.3.klib",
            "coingecko-js-1.0.0-beta02.klib",
            "atomicfu-js-0.22.0.klib",
            "ktor-client-js-js-2.3.5.klib",
        )
        assertEquals(expected.joinToString("\n"), actual.joinToString("\n"))
    }

    @Test
    fun test_jvm_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveDependencyTree(
            rootModule = module,
            rootFolder = sampleDir / "6-dependencies",
            targets = listOf(KotlinTarget.JVM),
        )

        val flatTree = depTree.shakeAndFlattenDependencies()
        val children = resolver.resolveArtifacts(flatTree, releaseMode = false, target = KotlinTarget.JVM)
        val actual = children
            .flatMap { it.artifacts }
            .map { it.substringAfterLast(DIRECTORY_SEPARATOR) }

        val expected = listOf(
            "annotations-23.0.0.jar",
            "kotlinx-coroutines-core-jvm-1.7.3.jar",
            "kotlinx-serialization-core-jvm-1.3.3.jar",
            "kotlinx-serialization-json-jvm-1.3.3.jar",
            "kotlinx-coroutines-jdk8-1.7.1.jar",
            "slf4j-api-1.7.36.jar",
            "ktor-io-jvm-2.3.5.jar",
            "ktor-utils-jvm-2.3.5.jar",
            "ktor-http-jvm-2.3.5.jar",
            "ktor-events-jvm-2.3.5.jar",
            "ktor-websockets-jvm-2.3.5.jar",
            "ktor-serialization-jvm-2.3.5.jar",
            "ktor-websocket-serialization-jvm-2.3.5.jar",
            "kotlinx-coroutines-slf4j-1.7.3.jar",
            "ktor-client-core-jvm-2.3.5.jar",
            "ktor-client-content-negotiation-jvm-2.0.3.jar",
            "ktor-serialization-kotlinx-jvm-2.0.3.jar",
            "ktor-serialization-kotlinx-json-jvm-2.0.3.jar",
            "coingecko-jvm-1.0.0-beta02.jar",
            "ktor-network-jvm-2.3.5.jar",
            "ktor-http-cio-jvm-2.3.5.jar",
            "ktor-network-tls-jvm-2.3.5.jar",
            "ktor-client-cio-jvm-2.3.5.jar",
        )
        assertEquals(expected.joinToString("\n"), actual.joinToString("\n"))
    }

    @Test
    fun test_mingwX64_artifact_resolution() = runTest(timeout = timeout) {
        val depTree = builder.resolveDependencyTree(
            rootModule = module,
            rootFolder = sampleDir / "6-dependencies",
            targets = listOf(KotlinTarget.MINGW_X64),
        )

        val flatTree = depTree.shakeAndFlattenDependencies()
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
            "ktor-http-cio.klib",
            "ktor-client-winhttp.klib",
            "ktor-client-winhttp-cinterop-winhttp.klib",
        )
        assertEquals(expected.joinToString("\n"), actual.joinToString("\n"))
    }
}
