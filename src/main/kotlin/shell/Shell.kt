package shell

import java.io.IOException

/** Выполняет команды эмулятора независимо от графического интерфейса. */
class Shell(
    private val initializeVfs: (() -> String)? = null,
    private val session: VfsSession? = null,
) {
    private val navigation = session?.navigation ?: VfsNavigation { VirtualFileSystem() }

    @Volatile
    private var hadErrors = false

    /** Запоминает ошибку загрузки или чтения в текущей сессии. */
    fun recordError() {
        hadErrors = true
    }

    /** Обрабатывает строку; ошибка команды не прекращает следующий ввод. */
    fun execute(line: String): CommandResult {
        val command = CommandParser.parse(line) ?: return CommandResult()
        return try {
            dispatch(command)
        } catch (error: IllegalArgumentException) {
            failure("${command.name}: ${error.message}")
        }
    }

    /** Передаёт команду реализации, работающей с той же VFS и текущим каталогом. */
    private fun dispatch(command: Command): CommandResult = when (command.name) {
        "ls" -> CommandResult(ListCommand(navigation).execute(command.arguments))
        "cd" -> changeDirectory(command.arguments)
        "echo" -> CommandResult(command.arguments.joinToString(" "))
        "find" -> CommandResult(FindCommand(navigation).execute(command.arguments))
        "exit" -> exit(command)
        "vfs-init" -> initialize(command)
        else -> failure("Неизвестная команда: ${command.name}")
    }

    /** Проверяет число аргументов и меняет только виртуальный текущий каталог. */
    private fun changeDirectory(arguments: List<String>): CommandResult {
        val operands = CommandOptions.parse(arguments, "").operands
        require(operands.size <= MAX_CD_ARGUMENTS) { "ожидается не более одного аргумента" }
        return CommandResult(navigation.changeDirectory(operands.firstOrNull()))
    }

    /** Завершает работу только при вызове exit без аргументов. */
    private fun exit(command: Command): CommandResult {
        if (command.arguments.isNotEmpty()) return failure("exit: аргументы не поддерживаются")
        val warning = if (hadErrors) "$EXIT_WARNING\n" else ""
        return CommandResult("${warning}Завершение работы.", exit = true)
    }

    /** Выполняет служебный сброс; ошибка записи не завершает приложение. */
    private fun initialize(command: Command): CommandResult {
        val action = initializeVfs ?: session?.let { it::initialize }
            ?: return failure("VFS не загружена: vfs-init недоступна")
        if (command.arguments.isNotEmpty()) {
            return failure("vfs-init: аргументы не поддерживаются")
        }
        return try {
            CommandResult(action())
        } catch (error: IOException) {
            failure("vfs-init: ${error.message}")
        }
    }

    /** Формирует единый формат сообщения об ошибке. */
    private fun failure(message: String): CommandResult {
        recordError()
        return CommandResult("Ошибка: $message", error = true)
    }

    companion object {
        /** Предупреждение перед завершением сессии, в которой были ошибки. */
        const val EXIT_WARNING = "Предупреждение: во время работы были ошибки."
        private const val MAX_CD_ARGUMENTS = 1
    }
}
