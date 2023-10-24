package ktpack.task

typealias TaskAction = suspend () -> Unit

interface Task {
    val name: String
    val description: String
    val dependsOn: List<Task>
    val finalizedBy: List<Task>
    val actionList: List<TaskAction>
}
