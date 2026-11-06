package nuget

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверяет настоящий процесс CLI, сообщения и коды завершения. */
class CliIntegrationTest {
    /** Цикл отображается конечным деревом, ошибки ввода не дают stack trace. */
    @Test fun runsCliAndReportsErrors() {
        val file = Files.createTempFile("graph", ".txt")
        try {
            Files.writeString(file, "A: B\nB: A")
            val good = run("--package", "A", "--test-mode", "true",
                "--repository", file.toString(), "--ascii", "true")
            assertEquals(0, good.first)
            assertTrue("A [seen]" in good.second)
            val bad = run("--package", "A", "--repository", "http://bad host")
            assertEquals(2, bad.first)
            assertFalse("Exception" in bad.second)
            Files.writeString(file, "A: MISSING")
            val broken = run("--package", "A", "--test-mode", "true",
                "--repository", file.toString())
            assertEquals(1, broken.first)
            assertTrue("Не объявлен" in broken.second)
        } finally { Files.delete(file) }
    }

    /** Создаёт отдельный JVM-процесс с классами приложения и стандартной библиотекой. */
    private fun run(vararg args: String): Pair<Int, String> {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val classpath = listOf(Config::class.java, Unit::class.java).map {
            Path.of(it.protectionDomain.codeSource.location.toURI()).toString()
        }.joinToString(System.getProperty("path.separator"))
        val process = ProcessBuilder(listOf(java, "-cp", classpath, "nuget.MainKt") + args)
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        return process.waitFor() to output
    }
}
