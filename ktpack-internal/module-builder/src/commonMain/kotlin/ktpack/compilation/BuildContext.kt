package ktpack.compilation

import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.manifest.ManifestLoader
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.kotlin.KotlincInstalls

class BuildContext(
    manifestLoader: ManifestLoader,
    val resolver: MavenDependencyResolver,
    val jdk: JdkInstalls,
    val kotlinc: KotlincInstalls,
    val debug: Boolean
) : ManifestLoader by manifestLoader
