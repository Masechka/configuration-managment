package nuget

import kotlin.system.exitProcess

private const val CONFIG_ERROR = 2

/** Запускает прототип CLI и возвращает ненулевой код при ошибке параметров. */
fun main(args: Array<String>) {
    try {
        val config = Config.parse(args)
        config.describe().forEach(::println)
    } catch (error: IllegalArgumentException) {
        System.err.println("Ошибка параметров: ${error.message}")
        exitProcess(CONFIG_ERROR)
    }
}
