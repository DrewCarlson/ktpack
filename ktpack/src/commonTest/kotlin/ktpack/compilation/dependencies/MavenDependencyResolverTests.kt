package ktpack.compilation.dependencies

import kotlinx.coroutines.test.runTest
import ktfio.nestedFile
import ktpack.PACK_SCRIPT_FILENAME
import ktpack.TestCliContext
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.sampleDir
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MavenDependencyResolverTests {

    private lateinit var module: ModuleConf
    private lateinit var context: TestCliContext
    private lateinit var builder: ModuleBuilder
    private lateinit var resolver: MavenDependencyResolver

    @BeforeTest
    fun setup() = runTest {
        context = TestCliContext()
        module = context.loadKtpackConf(sampleDir.nestedFile("6-dependencies\\${PACK_SCRIPT_FILENAME}").getAbsolutePath()).module
        builder = ModuleBuilder(
            module,
            context,
            sampleDir.nestedFile("6-dependencies").getAbsolutePath()
        )
        resolver = MavenDependencyResolver(module, context.http)
    }

    @Test
    fun test() = runTest {
        val depTree = builder.resolveDependencyTree(
            root = module,
            rootFolder = sampleDir.nestedFile("6-dependencies"),
            targets = listOf(KotlinTarget.MINGW_X64)
        )

        val children = depTree.filterChildVersions()
        val updated = children.joinToString("\n")

        assertEquals(
            """
                maven: co.touchlab:kermit:2.0.0-RC3
                maven: co.touchlab:kermit-core:2.0.0-RC3
                maven: io.ktor:ktor-client-core:2.2.4
                maven: org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
                maven: org.jetbrains.kotlinx:atomicfu:0.18.5
                maven: io.ktor:ktor-http:2.2.4
                maven: io.ktor:ktor-utils:2.2.4
                maven: io.ktor:ktor-io:2.2.4
                maven: io.ktor:ktor-events:2.2.4
                maven: io.ktor:ktor-websocket-serialization:2.2.4
                maven: io.ktor:ktor-serialization:2.2.4
                maven: io.ktor:ktor-websockets:2.2.4
                maven: org.drewcarlson:coingecko:1.0.0-alpha01
                maven: org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3
                maven: org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.3
                maven: io.ktor:ktor-client-content-negotiation:2.0.2
                maven: io.ktor:ktor-serialization-kotlinx-json:2.0.2
                maven: io.ktor:ktor-serialization-kotlinx:2.0.2
                maven: io.ktor:ktor-client-winhttp:2.2.4
                maven: io.ktor:ktor-http-cio:2.2.4
            """.trimIndent(),
            updated
        )
    }
}