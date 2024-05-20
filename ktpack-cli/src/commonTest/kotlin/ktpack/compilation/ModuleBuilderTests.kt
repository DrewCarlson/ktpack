package ktpack.compilation

import kotlinx.io.files.Path
import ktpack.TestCliContext
import ktpack.compilation.ModuleBuilder.BuildType
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ManifestToml
import ktpack.manifest.ModuleToml
import ktpack.sampleDir
import kotlin.test.Test

class ModuleBuilderTests {

    lateinit var builder: ModuleBuilder

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
            ManifestToml(ModuleToml("test", "0.0.0")),
            TestCliContext(),
            modulePath = Path(sampleDir, sample),
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
