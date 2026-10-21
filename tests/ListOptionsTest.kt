package shell

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверяет флаги ls по отдельности, в комбинациях и на границах размеров. */
class ListOptionsTest {
    /** Любой порядок объединённых и раздельных -l, -h, -a даёт одинаковый результат. */
    @Test
    fun allFlagCombinationsAndOrders() = withFiles { shell ->
        val combinations = listOf(
            "", "l", "h", "a", "lh", "hl", "la", "al", "ha", "ah",
            "lha", "lah", "hla", "hal", "alh", "ahl",
        )
        combinations.forEach { flags ->
            val names = if ('a' in flags) listOf(".", "..", ".hidden", "large.bin", "small.txt")
                else listOf("large.bin", "small.txt")
            val expected = names.joinToString("\n") { name ->
                val size = when (name) {
                    "large.bin" -> if ('h' in flags) "1.5K" else "1536"
                    "small.txt" -> "4"
                    else -> "0"
                }
                val type = if (name == "." || name == "..") "d" else "-"
                if ('l' in flags) "$type root root $size $name" else name
            }
            val options = if (flags.isEmpty()) listOf("") else listOf(
                "-$flags", flags.map { "-$it" }.joinToString(" "), "-$flags -$flags",
            )
            options.forEach { option ->
                val result = shell.execute("ls $option /")
                assertFalse(result.error, option)
                assertEquals(expected, result.output, option)
            }
        }
    }

    /** -h использует степени 1024, округляет вверх и не зависит от системной локали. */
    @Test
    fun humanReadableSizeBoundaries() {
        val sizes = linkedMapOf(
            0 to "0", 1 to "1", 1023 to "1023", 1024 to "1.0K", 1025 to "1.1K",
            1536 to "1.5K", 10_240 to "10K", 10_241 to "11K",
            1_048_575 to "1.0M", 1_048_576 to "1.0M",
        )
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            withArchive(sizes.keys.associate { "size-$it" to ByteArray(it) }) { path ->
                val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
                sizes.forEach { (bytes, human) ->
                    assertEquals(
                        "- root root $human size-$bytes",
                        shell.execute("ls -lh size-$bytes").output,
                    )
                    assertEquals(
                        "- root root $bytes size-$bytes",
                        shell.execute("ls -l size-$bytes").output,
                    )
                }
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    /** Новые флаги сочетаются с рекурсией и не ломают разделитель --. */
    @Test
    fun recursiveListingAndOptionTerminator() {
        withArchive(mapOf(".hidden/large.bin" to ByteArray(1536), "-h" to byteArrayOf())) { path ->
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            val result = shell.execute("ls -lahR /")
            assertFalse(result.error)
            assertTrue(result.output.contains("/.hidden:\n"))
            assertTrue(result.output.contains("- root root 1.5K large.bin"))
            assertFalse(shell.execute("ls -lhR /").output.contains("large.bin"))
            assertEquals("-h", shell.execute("ls -- -h").output)
            assertTrue(shell.execute("ls -lhz").error)
        }
    }

    /** Создаёт небольшую VFS со скрытым файлом и разными размерами содержимого. */
    private fun withFiles(test: (Shell) -> Unit) {
        withArchive(mapOf(
            ".hidden" to byteArrayOf(), "large.bin" to ByteArray(1536), "small.txt" to ByteArray(4),
        )) { path ->
            test(Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true))))
        }
    }
}
