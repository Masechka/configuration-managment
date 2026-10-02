package shell

import javax.swing.JFrame

/** Окно графического эмулятора оболочки. Создаётся в потоке событий Swing. */
class TerminalWindow(vfsName: String = "default.zip") {
    private val shell = Shell()
    private val frame = JFrame("VFS Shell — $vfsName")
    private val panel = TerminalPanel(::submit)

    init {
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.contentPane = panel
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setLocationByPlatform(true)
        panel.append("Команды: ls, cd, exit. Для выполнения нажмите Enter.")
    }

    /** Показывает окно и переводит фокус в строку ввода. */
    fun show() {
        frame.isVisible = true
        panel.input.requestFocusInWindow()
    }

    /** Выполняет команду и закрывает окно при корректном exit. */
    private fun submit(line: String) {
        val result = shell.execute(line)
        panel.append(result.output)
        if (result.exit) frame.dispose()
    }

    private companion object {
        const val WINDOW_WIDTH = 760
        const val WINDOW_HEIGHT = 500
    }
}
