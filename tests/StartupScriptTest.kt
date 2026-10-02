package shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки реального исполнения текстовых стартовых скриптов. */
class StartupScriptTest {
    /** Ошибка с номером строки не препятствует следующей команде. */
    @Test
    fun errorsAreReportedAndTheNextLineRuns() {
        withScript("ls /first\nunknown\n\ncd /last\n") { path ->
            val output = mutableListOf<String>()
            StartupScript.run(path, Shell()::execute, output::add)
            val dialogue = output.joinToString("\n")
            assertTrue(dialogue.contains("$ ls /first"))
            assertTrue(dialogue.contains("Строка 2"))
            assertTrue(dialogue.contains("unknown"))
            assertTrue(dialogue.contains("cd: аргументы=[/last]"))
        }
    }

    /** exit завершает скрипт, неверная exit с аргументами — нет. */
    @Test
    fun onlyValidExitStopsRemainingLines() {
        withScript("exit extra\nls /before\nexit\nls /after\n") { path ->
            val output = mutableListOf<String>()
            StartupScript.run(path, Shell()::execute, output::add)
            val dialogue = output.joinToString("\n")
            assertTrue(dialogue.contains("/before"))
            assertTrue(dialogue.contains("Завершение работы"))
            assertFalse(dialogue.contains("/after"))
        }
    }

    /** Отсутствующий файл диагностируется без необработанного исключения. */
    @Test
    fun unreadableScriptReportsAnError() {
        val path = Files.createTempFile("missing-script", ".txt")
        Files.delete(path)
        val output = mutableListOf<String>()
        StartupScript.run(path, Shell()::execute, output::add)
        assertTrue(output.joinToString().contains("Ошибка чтения стартового скрипта"))
    }

    /** Создаёт временный UTF-8 файл и гарантирует его удаление после проверки. */
    private fun withScript(content: String, test: (java.nio.file.Path) -> Unit) {
        val path = Files.createTempFile("startup with spaces", ".txt")
        try {
            Files.writeString(path, content)
            test(path)
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
