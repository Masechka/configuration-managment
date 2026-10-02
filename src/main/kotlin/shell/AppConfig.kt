package shell

import java.nio.file.Path

/** Пути, заданные параметрами запуска эмулятора. */
data class AppConfig(
    val vfsPath: Path = Path.of("default.zip"),
    val startupPath: Path? = null,
    val explicitVfs: Boolean = false,
) {
    /** Имя выбранной VFS для заголовка окна. */
    val vfsName: String get() = vfsPath.fileName?.toString() ?: vfsPath.toString()

    /** Отладочное представление всех параметров запуска. */
    fun debugDescription(): String =
        "Настройки: VFS=$vfsPath; startup=${startupPath ?: "не задан"}; " +
            "explicitVfs=$explicitVfs"

    /** Разбор поддерживаемых параметров командной строки. */
    companion object {
        private const val OPTION_STEP = 2

        /** Проверяет имена, повторы и значения --vfs и --startup. */
        fun parse(args: Array<String>): AppConfig {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val option = args[index]
                require(option in setOf("--vfs", "--startup")) {
                    "Неизвестный параметр: $option"
                }
                require(option !in values) { "Повтор параметра: $option" }
                val value = args.getOrNull(index + 1)
                require(!value.isNullOrBlank() && !value.startsWith("--")) {
                    "Для $option требуется путь"
                }
                values[option] = value
                index += OPTION_STEP
            }
            return AppConfig(
                vfsPath = Path.of(values["--vfs"] ?: "default.zip"),
                startupPath = values["--startup"]?.let(Path::of),
                explicitVfs = "--vfs" in values,
            )
        }
    }
}
