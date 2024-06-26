package ktpack.compilation

import ktpack.TestCliContext
import ktpack.compilation.ModuleBuilder.BuildType
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ManifestToml
import ktpack.manifest.ModuleToml
import ktpack.sampleDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ModuleBuilderTests {

    lateinit var builder: ModuleBuilder

    @Test
    fun testCollectSourceFiles_1_basic() {
        collectSourceFiles("1-basic", BuildType.BIN) {
            assertNotNull(mainFile)
            assertEquals(0, binFiles.size)
            assertEquals(0, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_2_multifile() {
        collectSourceFiles("2-multifile", BuildType.BIN) {
            assertNotNull(mainFile)
            assertEquals(0, binFiles.size)
            assertEquals(1, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_3_multiple_bins() {
        collectSourceFiles("3-multiple-bins", BuildType.BIN) {
            assertNotNull(mainFile)
            assertEquals(1, binFiles.size, "A: ${binFiles.joinToString()}")
            assertEquals(1, sourceFiles.size, "B: ${sourceFiles.joinToString()}")
        }
    }

    @Test
    fun testCollectSourceFiles_4_basic_lib() {
        collectSourceFiles("4-basic-lib", BuildType.LIB) {
            assertNotNull(mainFile)
            assertEquals(0, binFiles.size)
            assertEquals(0, sourceFiles.size)
        }
    }

    @Test
    fun testCollectSourceFiles_5_multifile_lib() {
        collectSourceFiles("5-multifile-lib", BuildType.LIB) {
            assertNotNull(mainFile)
            assertEquals(0, binFiles.size)
            assertEquals(1, sourceFiles.size)
        }
    }

    private fun collectSourceFiles(
        sample: String,
        type: BuildType,
        target: KotlinTarget? = null,
        body: ModuleBuilder.CollectedSource.() -> Unit,
    ) {
        builder = ModuleBuilder(
            ManifestToml(ModuleToml("test", "0.0.0")),
            TestCliContext(),
            modulePath = sampleDir / sample,
        )

        if (target == null) {
            KotlinTarget.entries.forEach { currentTarget ->
                body(builder.collectSourceFiles(currentTarget, type))
            }
        } else {
            body(builder.collectSourceFiles(target, type))
        }
    }
}
