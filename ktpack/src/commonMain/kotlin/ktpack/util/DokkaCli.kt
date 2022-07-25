package ktpack.util

import ksubprocess.Process
import ktfio.File
import ktpack.configuration.ManifestConf

// https://kotlin.github.io/dokka/1.7.10/user_guide/cli/usage/
// 1. Download dokka-cli (and deps) from https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-cli/1.7.10/dokka-cli-1.7.10.jar
// 2. Create dokka configuration
// 3. Run dokka with cli argument or JSON configuration
class DokkaCli {

    fun runDokka(manifest: ManifestConf) {
        val outputDir = File("out", "docs", manifest.module.name)
        Process {
            // the output directory where the documentation is generated
            args("-outputDir", outputDir.getAbsolutePath())
            // (required) - module name used as a part of source set ID when declaring dependent source sets
            args("-moduleName", manifest.module.name)
            // cache directory to enable package-list caching
            //args("-cacheRoot", "")
            // artifacts with Dokka plugins, separated by ;. TODO: REQUIRES DOKKA BASE DEPENDENCIES!
            //args("-pluginClasspath", ";")
            // configuration for plugins in format fqPluginName=json^^fqPluginName=json...
            //args("-pluginsConfiguration", "")
            // do not resolve package-lists online
            //args("-offlineMode", "")
            // throw an exception instead of a warning
            //args("-failOnWarning", "")
            // per package options added to all source sets
            //args("-globalPackageOptions", "")
            // external documentation links added to all source sets
            //args("-globalLinks", "")
            // source links added to all source sets
            //args("-globalSrcLink", "")
            // don't suppress obvious functions like default toString or equals
            //args("-noSuppressObviousFunctions", "")
            // suppress all inherited members that were not overriden in a given class. Eg. using it you can suppress toString or equals functions but you can't suppress componentN or copy on data class
            //args("-suppressInheritedMembers", "")
            // (repeatable) - configuration for a single source set. Following this argument, you can pass other arguments:
            //args("-sourceSet", "")
            // source set name as a part of source set ID when declaring dependent source sets
            // -> args("-sourceSetName", "")
            // source set name displayed in the generated documentation
            // -> args("-displayName", "")
            // list of source files or directories separated by ;
            // -> args("-src", "")
            // list of directories or .jar files to include in the classpath (used for resolving references) separated by ;
            // -> args("-classpath", "")
            // list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the @sample tag) separated by ;
            // -> args("-samples", "")
            // list of files containing the documentation for the module and individual packages separated by ;
            // -> args("-includes", "")
            // Deprecated, prefer using documentedVisibilities. Include protected and private code
            // -> args("-includeNonPublic", "")
            // a list of visibility modifiers (separated by ;) that should be documented. Overrides includeNonPublic. Default is PUBLIC. Possible values: PUBLIC, PRIVATE, PROTECTED, INTERNAL (Kotlin-specific), PACKAGE (Java-specific package-private)
            // -> args("-documentedVisibilities", "")
            // if set, deprecated elements are not included in the generated documentation
            // -> args("-skipDeprecated", "")
            // warn about undocumented members
            // -> args("-reportUndocumented", "")
            // create index pages for empty packages
            // -> args("-noSkipEmptyPackages", "")
            // list of package options in format matchingRegex,-deprecated,-privateApi,+reportUndocumented;+visibility:PRIVATE;matchingRegex, ..., separated by ;
            // -> args("-perPackageOptions", "")
            // list of external documentation links in format url^packageListUrl^^url2..., separated by ;
            // -> args("-links", "")
            // mapping between a source directory and a Web site for browsing the code in format <path>=<url>[#lineSuffix]
            // -> args("-srcLink", "")
            // disable linking to online kotlin-stdlib documentation
            // -> args("-noStdlibLink", "")
            // disable linking to online JDK documentation
            // -> args("-noJdkLink", "")
            // version of JDK to use for linking to JDK JavaDoc
            // -> args("-jdkVersion", "")
            // platform used for analysis, see the Platforms section
            // -> args("-analysisPlatform", "")
            // list of dependent source sets in format moduleName/sourceSetName, separated by ;
            // -> args("-dependentSourceSets", "")
            // one of DEBUG, PROGRESS, INFO, WARN, ERROR. Defaults to DEBUG. Please note that this argument can't be passed in JSON.
            //args("-", "loggingLevel")
        }
    }
}