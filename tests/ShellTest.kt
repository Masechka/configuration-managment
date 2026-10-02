package shell

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки команд первого этапа и обработки ошибок. */
class ShellTest {
    /** Заглушка сохраняет несколько аргументов и не завершает диалог. */
    @Test
    fun lsDisplaysItsNameAndArguments() {
        val result = Shell().execute("ls /a /b")
        assertTrue(result.output.contains("ls"))
        assertTrue(result.output.contains("/a"))
        assertTrue(result.output.contains("/b"))
        assertFalse(result.error)
        assertFalse(result.exit)
    }

    /** cd без пути и с одним путём остаётся допустимой заглушкой. */
    @Test
    fun cdAcceptsZeroOrOneArgument() {
        val shell = Shell()
        assertFalse(shell.execute("cd").error)
        val result = shell.execute("cd /home")
        assertTrue(result.output.contains("/home"))
        assertFalse(result.error)
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
