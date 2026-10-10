package shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки команд оболочки и обработки ошибок. */
class ShellTest {
    /** Ошибка сохраняется до exit даже после успешных команд. */
    @Test
    fun exitWarnsAboutEarlierErrors() {
        val shell = Shell()
        shell.execute("unknown")
        shell.execute("ls")
        val result = shell.execute("exit")
        assertTrue(result.exit)
        assertFalse(result.error)
        assertTrue(result.output.contains("Предупреждение: во время работы были ошибки."))
        assertTrue(result.output.indexOf("Предупреждение") < result.output.indexOf("Завершение"))
    }

    /** Чистая сессия завершается без предупреждения. */
    @Test
    fun cleanSessionExitsWithoutWarning() {
        val shell = Shell()
        shell.execute("ls")
        assertFalse(shell.execute("exit").output.contains("Предупреждение"))
    }

    /** ls реальной пустой VFS возвращает пустой список и ошибки для пропавших путей. */
    @Test
    fun lsUsesTheVirtualRoot() {
        val shell = Shell()
        assertEquals("", shell.execute("ls").output)
        assertTrue(shell.execute("ls /a /b").error)
        assertFalse(shell.execute("ls").exit)
    }

    /** cd разрешает корень без аргументов, но не создаёт отсутствующие каталоги. */
    @Test
    fun cdUsesExistingDirectories() {
        val shell = Shell()
        assertFalse(shell.execute("cd").error)
        assertFalse(shell.execute("cd /").error)
        assertTrue(shell.execute("cd /home").error)
    }

    /** Ошибочная команда не прерывает следующий ввод. */
    @Test
    fun unknownCommandReportsAnErrorWithoutExiting() {
        val shell = Shell()
        val result = shell.execute("missing")
        assertTrue(result.error)
        assertTrue(result.output.contains("missing"))
        assertFalse(result.exit)
        assertFalse(shell.execute("ls").error)
    }

    /** Дополнительные аргументы cd вызывают ошибку. */
    @Test
    fun cdRejectsExtraArguments() {
        assertTrue(Shell().execute("cd a b").error)
    }

    /** Завершение происходит только при корректном вызове exit. */
    @Test
    fun exitRejectsArgumentsAndExitsWithoutArguments() {
        val shell = Shell()
        assertTrue(shell.execute("exit").exit)
        assertTrue(shell.execute("exit extra").error)
        assertFalse(shell.execute("exit extra").exit)
    }

    /** Пустой ввод не вызывает ошибки или завершения. */
    @Test
    fun emptyInputDoesNothing() {
        val result = Shell().execute(" ")
        assertTrue(result.output.isEmpty())
        assertFalse(result.error)
        assertFalse(result.exit)
    }
}
