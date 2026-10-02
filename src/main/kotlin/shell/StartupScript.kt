package shell

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** Исполнитель UTF-8 скрипта: ошибочные команды не прерывают следующие строки. */
object StartupScript {
    /** Отображает ввод и вывод, останавливается только при корректном exit. */
    fun run(
        path: Path,
        execute: (String) -> CommandResult,
        display: (String) -> Unit,
    ) {
        try {
            Files.newBufferedReader(path).use { reader ->
                reader.lineSequence().forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    display("$ $line")
                    val result = execute(line)
                    val text = if (result.error) {
                        "Строка ${index + 1}: ${result.output}"
                    } else {
                        result.output
                    }
                    display(text)
                    if (result.exit) return
                }
            }
        } catch (error: IOException) {
            display("Ошибка чтения стартового скрипта '$path': ${error.message}")
        }
    }
}
