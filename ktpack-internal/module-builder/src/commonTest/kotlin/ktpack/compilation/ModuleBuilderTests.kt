package ktpack.compilation

import io.ktor.client.*
import kotlinx.io.files.Path
import ktpack.KtpackUserConfig
import ktpack.compilation.ModuleBuilder.BuildType
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.configuration.KotlinTarget
import ktpack.manifest.DefaultManifestLoader
import ktpack.manifest.MANIFEST_FILENAME
import ktpack.manifest.ManifestToml
import ktpack.manifest.ModuleToml
import ktpack.sampleDir
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.kotlin.KotlincInstalls
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ModuleBuilderTests {

    private lateinit var http: HttpClient
    private lateinit var manifest: ManifestToml
    private lateinit var context: BuildContext
    private lateinit var builder: ModuleBuilder

    @BeforeTest
    fun setup() {
        val sampleRoot = Path(sampleDir, "6-dependencies")
        val packScript = Path(sampleRoot, MANIFEST_FILENAME)
        http = HttpClient()
        val config = KtpackUserConfig()
        context = BuildContext(
            manifestLoader = DefaultManifestLoader(),
            resolver = MavenDependencyResolver(http),
            jdk = JdkInstalls(config = config.jdk, http = http),
            kotlinc = KotlincInstalls(config = config, http = http),
            debug = true
        )
        manifest = context.load(packScript.toString())
    }

    @AfterTest
    fun cleanup() {
        http.close()
    }

    @Test
    fun testCollectSourceFiles_1_basic() {
        collectSourceFiles("1-basic", BuildType.BIN) {
            //assertEquals(0, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_2_multifile() {
        collectSourceFiles("2-multifile", BuildType.BIN) {
            //assertEquals(0, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_3_multiple_bins() {
        collectSourceFiles("3-multiple-bins", BuildType.BIN) {
            //assertEquals(3, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_4_basic_lib() {
        collectSourceFiles("4-basic-lib", BuildType.LIB) {
            //assertEquals(1, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_5_multifile_lib() {
        collectSourceFiles("5-multifile-lib", BuildType.LIB) {
            //assertEquals(0, sourceFiles.size)
        }
    }

    private fun collectSourceFiles(
        sample: String,
        type: BuildType,
        target: KotlinTarget? = null,
        body: CollectedSource.() -> Unit,
    ) {
        builder = ModuleBuilder(
            manifest = ManifestToml(ModuleToml("test", "0.0.0")),
            modulePath = Path(sampleDir, sample),
            context = context,
        )

        if (target == null) {
            KotlinTarget.entries.forEach { currentTarget ->
                body(builder.sourceCollector.collectKotlin(currentTarget, type))
            }
        } else {
            body(builder.sourceCollector.collectKotlin(target, type))
        }
    }
}
