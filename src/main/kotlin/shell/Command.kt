package shell

/** Имя введённой команды и её аргументы после разделения по пробелам. */
data class Command(val name: String, val arguments: List<String>)

/** Текст ответа и признаки ошибки и завершения приложения. */
data class CommandResult(
    val output: String = "",
    val error: Boolean = false,
    val exit: Boolean = false,
)
