package shell

/** Меняет только метаданные VFS в памяти; ни архив, ни содержимое файлов не записываются. */
internal class ChownCommand(private val navigation: VfsNavigation) {
    /** Поддерживает owner[:group], :group, числовые IDs, -R, несколько путей и --. */
    fun execute(arguments: List<String>): String {
        val options = CommandOptions.parse(arguments, "R")
        require(options.operands.size >= 2) { "chown: ожидаются владелец и хотя бы один путь" }
        val ownership = parseOwnership(options.operands.first())
        val paths = options.operands.drop(1).map(navigation::resolve)
        val fileSystem = navigation.fileSystem
        val targets = if ('R' in options.flags) paths.flatMap(fileSystem::subtree) else paths
        targets.distinct().forEach { path ->
            fileSystem.changeOwnership(path, ownership.owner, ownership.group)
        }
        return ""
    }

    /** owner: задаёт группу с именем виртуального владельца; пропуски сохраняют прежнее поле. */
    private fun parseOwnership(text: String): OwnershipChange {
        val parts = text.split(':')
        require(parts.size <= 2) { "chown: неверный формат owner[:group]" }
        val owner = parts.first().ifEmpty { null }
        val group = if (parts.size == 1) null else parts.last().ifEmpty { owner }
        require(owner != null || group != null) { "chown: не задан владелец или группа" }
        listOfNotNull(owner, group).forEach { name ->
            require(NAME.matches(name)) { "chown: неверное имя или ID: $name" }
        }
        return OwnershipChange(owner, group)
    }

    /** Поле null означает сохранение прежнего владельца или группы. */
    private data class OwnershipChange(val owner: String?, val group: String?)

    private companion object {
        val NAME = Regex("([0-9]+|[A-Za-z_][A-Za-z0-9_.-]*[$]?)")
    }
}
