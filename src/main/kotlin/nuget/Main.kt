package nuget

import java.nio.file.Path
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
        val graph = GraphBuilder(repository(config)).build(config.packageName, config.maxDepth)
        val lines = if (config.ascii) AsciiTree.render(graph) else GraphOutput.lines(graph)
        lines.forEach(::println)
    } catch (error: Exception) {
        System.err.println("Ошибка данных: ${error.message}")
        exitProcess(DATA_ERROR)
    }
}

/** Выбирает HTTP NuGet либо проверенный локальный граф. */
private fun repository(config: Config): Repository {
    if (!config.testMode) return NugetRepository(config.repository)
    val repository = FileRepository.read(Path.of(config.repository))
    require(repository.contains(config.packageName)) {
        "В тестовом графе нет пакета ${config.packageName}."
    }
    return repository
}
