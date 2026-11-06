package nuget

import kotlin.system.exitProcess

private const val CONFIG_ERROR = 2
private const val DATA_ERROR = 1

/** Запускает CLI и разделяет ошибки конфигурации и получения данных. */
fun main(args: Array<String>) {
    val config = try {
        Config.parse(args)
    } catch (error: Exception) {
        System.err.println("Ошибка параметров: ${error.message}")
        exitProcess(CONFIG_ERROR)
    }
    try {
        check(!config.testMode) { "Файловый репозиторий будет доступен на этапе 3." }
        val dependencies = NugetRepository(config.repository).dependencies(config.packageName)
        println("Прямые зависимости ${config.packageName}:")
        dependencies.forEach { println("${it.name} ${it.versionRange} ${it.framework}".trim()) }
        if (dependencies.isEmpty()) println("(нет)")
    } catch (error: Exception) {
        System.err.println("Ошибка данных: ${error.message}")
        exitProcess(DATA_ERROR)
    }
}
