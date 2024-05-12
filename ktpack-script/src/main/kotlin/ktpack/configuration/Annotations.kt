package ktpack.configuration

/**
 * Import other script(s)
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
public annotation class Import(vararg val paths: String)

/**
 * Compiler options that will be applied on script compilation
 *
 * @see [kotlin.script.experimental.api.compilerOptions]
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
public annotation class CompilerOptions(vararg val options: String)

/**
 * Option that configures the name of the variable that will hold a file pointing to the script location.
 * If not specified, {@link [SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME]} will be used as the variable name
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
public annotation class ScriptFileLocation(val variable: String)
