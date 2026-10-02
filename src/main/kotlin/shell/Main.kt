package shell

import javax.swing.SwingUtilities
import kotlin.system.exitProcess

private const val CONFIGURATION_ERROR = 2

/** Запускает графический прототип эмулятора. */
fun main(args: Array<String>) {
    val config = try {
        AppConfig.parse(args)
    } catch (error: IllegalArgumentException) {
        System.err.println("Ошибка конфигурации: ${error.message}")
        exitProcess(CONFIGURATION_ERROR)
    }
    println(config.debugDescription())
    SwingUtilities.invokeLater { TerminalWindow(config).show() }
}
