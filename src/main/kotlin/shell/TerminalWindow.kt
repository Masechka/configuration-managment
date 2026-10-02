package shell

import java.io.IOException
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/** Окно графического эмулятора оболочки. Создаётся в потоке событий Swing. */
class TerminalWindow(private val config: AppConfig = AppConfig()) {
    private var shell = Shell()
    private val frame = JFrame("VFS Shell — ${config.vfsName}")
    private val panel = TerminalPanel(::submit)

    init {
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.contentPane = panel
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setLocationByPlatform(true)
        panel.append("Команды: ls, cd, vfs-init, exit. Для выполнения нажмите Enter.")
        panel.append(config.debugDescription())
    }

    /** Показывает окно и переводит фокус в строку ввода. */
    fun show() {
        frame.isVisible = true
        startSession()
    }

    /** Выполняет команду и закрывает окно при корректном exit. */
    private fun submit(line: String) {
        panel.input.isEnabled = false
        thread(name = "shell-command", isDaemon = true) {
            val result = shell.execute(line)
            SwingUtilities.invokeLater {
                panel.append(result.output)
                if (result.exit) frame.dispose()
                panel.input.isEnabled = true
                panel.input.requestFocusInWindow()
            }
        }
    }

    /** Исполняет скрипт вне потока Swing, блокируя ввод до его окончания. */
    private fun startSession() {
        panel.input.isEnabled = false
        thread(name = "startup-script", isDaemon = true) {
            if (loadVfs()) {
                config.startupPath?.let { path ->
                    StartupScript.run(path, ::executeStartup, ::display)
                }
            }
            SwingUtilities.invokeLater {
                panel.input.isEnabled = true
                panel.input.requestFocusInWindow()
            }
        }
    }

    /** Загружает ZIP вне потока Swing; ошибку показывает в окне и терминале. */
    private fun loadVfs(): Boolean {
        return try {
            val session = VfsSession.load(config)
            shell = Shell(session::initialize)
            display("Загружена ${config.vfsName}\n${session.fileSystem.describe()}")
            true
        } catch (error: IOException) {
            display("Ошибка загрузки VFS: ${error.message}")
            false
        } catch (error: IllegalArgumentException) {
            display("Ошибка загрузки VFS: ${error.message}")
            false
        }
    }

    /** Возвращает результат команды и планирует закрытие окна при exit. */
    private fun executeStartup(line: String): CommandResult {
        val result = shell.execute(line)
        if (result.exit) SwingUtilities.invokeLater { frame.dispose() }
        return result
    }

    /** Показывает вывод скрипта в окне и дублирует его в терминале запуска. */
    private fun display(text: String) {
        println(text)
        SwingUtilities.invokeLater { panel.append(text) }
    }

    private companion object {
        const val WINDOW_WIDTH = 760
        const val WINDOW_HEIGHT = 500
    }
}
