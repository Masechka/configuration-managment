package shell

/** Выполняет команды прототипа независимо от графического интерфейса. */
class Shell {
    /** Обрабатывает строку; ошибка команды не прекращает следующий ввод. */
    fun execute(line: String): CommandResult {
        val command = CommandParser.parse(line) ?: return CommandResult()
        return when (command.name) {
            "ls" -> stub(command)
            "cd" -> changeDirectoryStub(command)
            "exit" -> exit(command)
            else -> failure("Неизвестная команда: ${command.name}")
        }
    }

    /** Выводит имя и все аргументы заглушки. */
    private fun stub(command: Command): CommandResult {
        val arguments = command.arguments.joinToString(", ", "[", "]")
        return CommandResult("${command.name}: аргументы=$arguments")
    }

    /** Проверяет число аргументов cd, не меняя состояние файловой системы. */
    private fun changeDirectoryStub(command: Command): CommandResult {
        if (command.arguments.size > MAX_CD_ARGUMENTS) {
            return failure("cd: ожидается не более одного аргумента")
        }
        return stub(command)
    }

    /** Завершает работу только при вызове exit без аргументов. */
    private fun exit(command: Command): CommandResult {
        if (command.arguments.isNotEmpty()) return failure("exit: аргументы не поддерживаются")
        return CommandResult("Завершение работы.", exit = true)
    }

    /** Формирует единый формат сообщения об ошибке. */
    private fun failure(message: String) = CommandResult("Ошибка: $message", error = true)

    private companion object {
        const val MAX_CD_ARGUMENTS = 1
    }
}
