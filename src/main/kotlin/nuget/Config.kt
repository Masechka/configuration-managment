package nuget

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

private const val DEFAULT_DEPTH = 3
private const val MAX_PACKAGE_LENGTH = 128
private const val OPTION_WIDTH = 2
private const val NO_REMAINDER = 0
private const val MIN_DEPTH = 0
private const val DEFAULT_REPOSITORY = "https://api.nuget.org/v3/index.json"

/** Пять пользовательских настроек анализатора. Глубина корня равна нулю. */
data class Config(
    val packageName: String,
    val repository: String,
    val testMode: Boolean,
    val ascii: Boolean,
    val maxDepth: Int
) {
    /** Формирует диагностический вывод этапа 1 в формате ключ-значение. */
    fun describe(): List<String> = listOf(
        "package=$packageName", "repository=$repository", "test-mode=$testMode",
        "ascii=$ascii", "max-depth=$maxDepth"
    )

    companion object {
        private val options = setOf(
            "--package", "--repository", "--test-mode", "--ascii", "--max-depth"
        )

        /** Проверяет аргументы CLI и возвращает настройки или понятную ошибку. */
        fun parse(args: Array<String>): Config {
            val values = readOptions(args)
            val name = requireNotNull(values["--package"]) { "Нужна опция --package." }
            val testMode = boolean(values, "--test-mode")
            val source = values["--repository"] ?: DEFAULT_REPOSITORY
            validateName(name, testMode)
            validateSource(source, testMode)
            return Config(name, source, testMode, boolean(values, "--ascii"), depth(values))
        }

        /** Читает пары опция-значение, запрещая повторы и неизвестные ключи. */
        private fun readOptions(args: Array<String>): Map<String, String> {
            require(args.size % OPTION_WIDTH == NO_REMAINDER) {
                "У каждой опции должно быть значение."
            }
            val values = linkedMapOf<String, String>()
            args.toList().chunked(OPTION_WIDTH).forEach { (key, value) ->
                require(key in options) { "Неизвестная опция: $key" }
                require(key !in values) { "Повтор опции: $key" }
                require(value.isNotBlank()) { "Пустое значение: $key" }
                values[key] = value
            }
            return values
        }

        /** Принимает только явные true и false, чтобы опечатки не терялись. */
        private fun boolean(values: Map<String, String>, key: String): Boolean {
            val value = values[key] ?: "false"
            return requireNotNull(value.toBooleanStrictOrNull()) { "$key: нужны true или false." }
        }

        /** Допускает любую глубину, представимую неотрицательным Int. */
        private fun depth(values: Map<String, String>): Int {
            val value = values["--max-depth"] ?: DEFAULT_DEPTH.toString()
            val depth = requireNotNull(value.toIntOrNull()) { "--max-depth: нужно целое число." }
            require(depth >= MIN_DEPTH) { "--max-depth не может быть отрицательной." }
            return depth
        }

        /** Проверяет имя NuGet либо имя из больших латинских букв в тестовом режиме. */
        private fun validateName(name: String, testMode: Boolean) {
            require(name.length <= MAX_PACKAGE_LENGTH) { "Слишком длинное имя пакета." }
            val pattern = if (testMode) "[A-Z]+" else "[A-Za-z0-9_][A-Za-z0-9_.-]*"
            require(Regex(pattern).matches(name)) { "Некорректное имя пакета: $name" }
        }

        /** Различает читаемый локальный файл и абсолютный HTTP(S) URL NuGet. */
        private fun validateSource(source: String, testMode: Boolean) {
            if (testMode) {
                val path = Path.of(source)
                require(Files.isRegularFile(path) && Files.isReadable(path)) {
                    "--repository: файл недоступен: $source"
                }
                return
            }
            val uri = runCatching { URI(source) }.getOrElse {
                throw IllegalArgumentException("--repository: некорректный URL.")
            }
            require(uri.scheme in setOf("https", "http") && !uri.host.isNullOrBlank()) {
                "--repository: нужен абсолютный HTTP(S) URL."
            }
            require(uri.userInfo == null && uri.fragment == null) {
                "Недопустимый URL репозитория."
            }
        }
    }
}
