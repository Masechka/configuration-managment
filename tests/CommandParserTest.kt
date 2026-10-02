package shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Проверки простого разделения команды и аргументов. */
class CommandParserTest {
    /** Пустой ввод не создаёт команду. */
    @Test
    fun emptyInputIsIgnored() {
        assertNull(CommandParser.parse("   "))
    }

    /** Повторные пробелы не создают пустые аргументы. */
    @Test
    fun repeatedSpacesSeparateArguments() {
        val command = CommandParser.parse("  ls   /a  /b ")
        assertEquals("ls", command?.name)
        assertEquals(listOf("/a", "/b"), command?.arguments)
    }

    /** Простой парсер не добавляет обработку кавычек или переменных. */
    @Test
    fun quotesAndVariablesRemainLiteral() {
        val command = CommandParser.parse("ls \"a b\" \$HOME")
        assertEquals(listOf("\"a", "b\"", "\$HOME"), command?.arguments)
    }
}
