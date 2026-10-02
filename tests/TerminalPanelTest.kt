package shell

import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Проверки реальных компонентов Swing без открытия системного окна. */
class TerminalPanelTest {
    /** Отключённый ввод не запускает дополнительные команды во время операции. */
    @Test
    fun disabledInputDoesNotSubmit() {
        SwingUtilities.invokeAndWait {
            var submitted = false
            val panel = TerminalPanel { submitted = true }
            panel.input.isEnabled = false
            panel.input.text = "vfs-init"
            panel.input.postActionEvent()
            assertEquals(false, submitted)
        }
    }

    /** Enter передаёт строку, очищает поле и показывает команду с результатом. */
    @Test
    fun enterSubmitsInputAndDisplaysDialogue() {
        SwingUtilities.invokeAndWait {
            var submitted = ""
            lateinit var panel: TerminalPanel
            panel = TerminalPanel { line ->
                submitted = line
                panel.append(Shell().execute(line).output)
            }
            panel.input.text = "ls /home"
            panel.input.postActionEvent()
            assertEquals("ls /home", submitted)
            assertEquals("", panel.input.text)
            assertTrue(panel.history.text.contains("$ ls /home"))
            assertTrue(panel.history.text.contains("/home"))
        }
    }
}
