package shell

import javax.swing.SwingUtilities

/** Запускает графический прототип эмулятора. */
fun main() {
    SwingUtilities.invokeLater { TerminalWindow().show() }
}
