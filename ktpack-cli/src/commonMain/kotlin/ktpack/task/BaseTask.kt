package ktpack.task

abstract class BaseTask : Task {

    private val dependsOnInternal = mutableListOf<Task>()
    private val finalizedByInternal = mutableListOf<Task>()
    private val actionsInternal = mutableListOf<TaskAction>()

    override val dependsOn: List<Task> = dependsOnInternal
    override val finalizedBy: List<Task> = finalizedByInternal
    override val actionList: List<TaskAction> = actionsInternal

    fun dependsOn(task: Task) {
        dependsOnInternal.add(task)
    }

    fun finalizedBy(task: Task) {
        finalizedByInternal.add(task)
    }

    fun doFirst(action: TaskAction) {
        actionsInternal.add(0, action)
    }

    fun doLast(action: TaskAction) {
        actionsInternal.add(actionsInternal.size, action)
    }
}
