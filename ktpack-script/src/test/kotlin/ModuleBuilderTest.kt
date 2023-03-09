package ktpack.configuration

import kotlinx.serialization.encodeToString
import ktpack.configuration.DependencyConf.GitDependency
import ktpack.configuration.DependencyConf.LocalPathDependency
import ktpack.configuration.DependencyScope.API
import ktpack.configuration.DependencyScope.COMPILE
import ktpack.configuration.DependencyScope.IMPLEMENTATION
import ktpack.configuration.DependencyScope.TEST
import ktpack.configuration.KotlinTarget.JS_BROWSER
import ktpack.configuration.KotlinTarget.JVM
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ModuleBuilderTest {

    @Test
    fun testEmptyModuleConfiguration() = withTestScope {
        module("hello_world")
        assertEquals(1, getConfCount())

        val conf = getConf()
        val confJson = testJson.encodeToString(conf)

        assertEquals("hello_world", conf.name)
        assertEquals("0.0.1", conf.version)
        assertEquals(
            """|{
               |    "name": "hello_world",
               |    "version": "0.0.1"
               |}
            """.trimMargin(),
            confJson
        )
    }

    @Test
    fun testCompleteModuleConfiguration() = withTestScope {
        module("hello_world") {
            version = "1.0.0"
            kotlinVersion = "1.8.10"
            authors += "Test Author"
            autobin = false
            license = "license"
            homepage = "homepage"
            repository = "repository"
            publish = true
            targets(JVM, JS_BROWSER)
        }

        assertEquals(1, getConfCount())

        val conf = getConf()
        val confJson = testJson.encodeToString(conf)
        assertEquals("hello_world", conf.name)
        assertEquals("1.0.0", conf.version)
        assertEquals("1.7.10", conf.kotlinVersion)
        assertEquals("Test Author", conf.authors.firstOrNull())
        assertEquals(false, conf.autobin)
        assertEquals("license", conf.license)
        assertEquals("homepage", conf.homepage)
        assertEquals("repository", conf.repository)
        assertEquals(true, conf.publish)
        assertEquals(listOf(JVM, JS_BROWSER), conf.targets)
        assertEquals(
            """|{
               |    "name": "hello_world",
               |    "version": "1.0.0",
               |    "authors": [
               |        "Test Author"
               |    ],
               |    "homepage": "homepage",
               |    "repository": "repository",
               |    "license": "license",
               |    "publish": true,
               |    "autobin": false,
               |    "targets": [
               |        "jvm",
               |        "js_browser"
               |    ],
               |    "kotlinVersion": "1.7.10"
               |}
            """.trimMargin(),
            confJson
        )
    }

    @Test
    fun testLocalDependencies() = withTestScope {
        module("hello_world") {
            dependencies {
                local("test_module_impl")
                localApi("test_module_api")
                localTest("test_module_test")
                localCompile("test_module_compile")
            }
        }

        assertEquals(1, getConfCount())

        val conf = getConf()
        val confJson = testJson.encodeToString(conf)

        val depContainer = conf.dependencies.find { it.targets.isEmpty() }
        assertNotNull(depContainer)

        val implDep = depContainer.dependencies[0]
        val apiDep = depContainer.dependencies[1]
        val testDep = depContainer.dependencies[2]
        val compileDep = depContainer.dependencies[3]

        assertEquals(LocalPathDependency("test_module_impl", IMPLEMENTATION), implDep)
        assertEquals(LocalPathDependency("test_module_api", API), apiDep)
        assertEquals(LocalPathDependency("test_module_test", TEST), testDep)
        assertEquals(LocalPathDependency("test_module_compile", COMPILE), compileDep)

        assertEquals(
            """|{
               |    "name": "hello_world",
               |    "version": "0.0.1",
               |    "dependencies": [
               |        {
               |            "targets": [
               |            ],
               |            "dependencies": [
               |                {
               |                    "type": "ktpack.configuration.DependencyConf.LocalPathDependency",
               |                    "path": "test_module_impl",
               |                    "scope": "IMPLEMENTATION"
               |                },
               |                {
               |                    "type": "ktpack.configuration.DependencyConf.LocalPathDependency",
               |                    "path": "test_module_api",
               |                    "scope": "API"
               |                },
               |                {
               |                    "type": "ktpack.configuration.DependencyConf.LocalPathDependency",
               |                    "path": "test_module_test",
               |                    "scope": "TEST"
               |                },
               |                {
               |                    "type": "ktpack.configuration.DependencyConf.LocalPathDependency",
               |                    "path": "test_module_compile",
               |                    "scope": "COMPILE"
               |                }
               |            ]
               |        }
               |    ]
               |}
            """.trimMargin(),
            confJson
        )
    }

    @Test
    fun testGitDependencies() = withTestScope {
        module("hello_world") {
            dependencies {
                git("test_module_impl")
                gitApi("test_module_api")
                gitTest("test_module_test")
                gitCompile("test_module_compile")
            }
        }

        assertEquals(1, getConfCount())

        val conf = getConf()

        val depContainer = conf.dependencies.find { it.targets.isEmpty() }
        assertNotNull(depContainer)

        val implDep = depContainer.dependencies[0]
        val apiDep = depContainer.dependencies[1]
        val testDep = depContainer.dependencies[2]
        val compileDep = depContainer.dependencies[3]

        assertEquals(GitDependency("test_module_impl", null, null, null, IMPLEMENTATION), implDep)
        assertEquals(GitDependency("test_module_api", null, null, null, API), apiDep)
        assertEquals(GitDependency("test_module_test", null, null, null, TEST), testDep)
        assertEquals(GitDependency("test_module_compile", null, null, null, COMPILE), compileDep)
    }
}
