package shell

/** Проверенные короткие флаги и операнды команды после них. */
internal data class CommandOptions(val flags: Set<Char>, val operands: List<String>) {
    companion object {
        /** Разбирает -alR и --; неизвестные флаги сообщаются как ошибки команды. */
        fun parse(arguments: List<String>, allowed: String): CommandOptions {
            val flags = mutableSetOf<Char>()
            val operands = mutableListOf<String>()
            var readOptions = true
            arguments.forEach { argument ->
                when {
                    readOptions && argument == "--" -> readOptions = false
                    readOptions && argument.startsWith('-') && argument != "-" -> {
                        argument.drop(1).forEach { flag ->
                            require(flag in allowed) { "Неподдерживаемая опция: -$flag" }
                            flags.add(flag)
                        }
                    }
                    else -> {
                        readOptions = false
                        operands.add(argument)
                    }
                }
            }
            return CommandOptions(flags, operands)
        }
    }
}
