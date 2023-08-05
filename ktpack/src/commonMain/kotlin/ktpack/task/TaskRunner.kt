package ktpack.task

class TaskRunner(
    launchTasks: List<Task> = emptyList(),
) {
    private val taskList: MutableList<Task> = launchTasks.toMutableList()

    fun addTask(task: Task) {
        taskList.add(0, task)
    }

    suspend fun execute() {
        taskList.map { it.execute() }
    }

    private suspend fun Task.execute() {
        actionList.forEach { it.invoke() }
    }
}
