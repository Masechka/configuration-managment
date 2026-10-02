package shell

import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField

/** Панель диалога: история вывода и ввод одной команды по Enter. */
class TerminalPanel(private val submit: (String) -> Unit) : JPanel(BorderLayout()) {
    /** История диалога, доступная для выделения и копирования. */
    val history = JTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE)
        lineWrap = true
        wrapStyleWord = true
    }

    /** Поле ввода команды. */
    val input = JTextField().apply {
        font = history.font
        toolTipText = "Введите команду и нажмите Enter"
    }

    init {
        border = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        add(JScrollPane(history), BorderLayout.CENTER)
        add(input, BorderLayout.SOUTH)
        input.addActionListener {
            if (!input.isEnabled) return@addActionListener
            val line = input.text
            input.text = ""
            append("$ $line")
            submit(line)
        }
    }

    /** Дописывает непустой результат и прокручивает историю к последней строке. */
    fun append(text: String) {
        if (text.isEmpty()) return
        history.append("$text\n")
        history.caretPosition = history.document.length
    }

    private companion object {
        const val FONT_SIZE = 14
        const val PADDING = 10
    }
}
