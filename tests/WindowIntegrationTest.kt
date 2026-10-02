package shell

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Интеграция настоящего графического окна на Mac; запускается задачей guiTest. */
class WindowIntegrationTest {
    /** Окно показывает VFS, скрипт продолжает после ошибки, ручной ввод доступен. */
    @Test
    fun realWindowDisplaysVfsAndRunsCommands() {
        withArchive(mapOf("hello.txt" to "hello".toByteArray())) { path ->
            val script = path.parent.resolve("startup.txt")
            Files.writeString(script, "unknown\nls /after-error\n")
            val frame = openWindow(AppConfig(path, script, true))
            try {
                waitForInput(frame)
                SwingUtilities.invokeAndWait {
                    val panel = frame.contentPane as TerminalPanel
                    assertTrue(frame.title.contains("sample.zip"))
                    assertTrue(panel.history.text.contains("/hello.txt"))
                    assertTrue(panel.history.text.contains("Строка 1"))
                    assertTrue(panel.history.text.contains("/after-error"))
                    panel.input.text = "cd /home"
                    panel.input.postActionEvent()
                }
                waitForInput(frame)
                SwingUtilities.invokeAndWait {
                    assertTrue((frame.contentPane as TerminalPanel).history.text.contains("/home"))
                    savePanelPreview(frame)
                }
            } finally {
                SwingUtilities.invokeAndWait { frame.dispose() }
            }
        }
    }

    /** Сброс выполняется из GUI, корректный exit закрывает системное окно. */
    @Test
    fun resetAndExitWorkFromTheWindow() {
        withArchive(mapOf("old.txt" to "old".toByteArray())) { path ->
            val frame = openWindow(AppConfig(path, explicitVfs = true))
            try {
                waitForInput(frame)
                submit(frame, "vfs-init")
                waitForInput(frame)
                assertEquals(setOf("/"), ZipVfs.load(path).paths())
                submit(frame, "exit")
                waitUntil { !frame.isDisplayable }
                assertFalse(frame.isDisplayable)
            } finally {
                SwingUtilities.invokeAndWait { frame.dispose() }
            }
        }
    }

    /** Ошибка VFS видна в окне; стартовый скрипт не выполняется. */
    @Test
    fun invalidVfsSkipsStartupButAllowsExit() {
        withArchive(emptyMap()) { path ->
            Files.writeString(path, "broken ZIP")
            val script = path.parent.resolve("startup.txt")
            Files.writeString(script, "ls /must-not-run\n")
            val frame = openWindow(AppConfig(path, script, true))
            try {
                waitForInput(frame)
                SwingUtilities.invokeAndWait {
                    val text = (frame.contentPane as TerminalPanel).history.text
                    assertTrue(text.contains("Ошибка загрузки VFS"))
                    assertFalse(text.contains("/must-not-run"))
                }
                submit(frame, "exit")
                waitUntil { !frame.isDisplayable }
            } finally {
                SwingUtilities.invokeAndWait { frame.dispose() }
            }
        }
    }

    /** Находит созданное окно приложения в реальном окружении Swing. */
    private fun openWindow(config: AppConfig): JFrame {
        lateinit var frame: JFrame
        SwingUtilities.invokeAndWait {
            TerminalWindow(config).show()
            frame = JFrame.getFrames().filterIsInstance<JFrame>().last { it.isVisible }
        }
        return frame
    }

    /** Ожидает завершения загрузки или выполнения команды. */
    private fun waitForInput(frame: JFrame) {
        waitUntil { (frame.contentPane as TerminalPanel).input.isEnabled }
    }

    /** Посылает Enter через настоящий компонент ввода. */
    private fun submit(frame: JFrame, line: String) {
        SwingUtilities.invokeAndWait {
            val panel = frame.contentPane as TerminalPanel
            panel.input.text = line
            panel.input.postActionEvent()
        }
    }

    /** Проверяет состояние в потоке Swing с ограниченным временем ожидания. */
    private fun waitUntil(condition: () -> Boolean) {
        repeat(WAIT_ATTEMPTS) {
            var ready = false
            SwingUtilities.invokeAndWait { ready = condition() }
            if (ready) return
            Thread.sleep(WAIT_INTERVAL_MS)
        }
        error("Окно не перешло в ожидаемое состояние")
    }

    /** Сохраняет изображение компонентов самого приложения для проверки оформления. */
    private fun savePanelPreview(frame: JFrame) {
        val panel = frame.contentPane
        val image = BufferedImage(panel.width, panel.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        panel.printAll(graphics)
        graphics.dispose()
        Files.createDirectories(Path.of("build/preview"))
        ImageIO.write(image, "png", Path.of("build/preview/terminal.png").toFile())
    }

    private companion object {
        const val WAIT_ATTEMPTS = 250
        const val WAIT_INTERVAL_MS = 20L
    }
}
