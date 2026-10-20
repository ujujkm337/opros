package com.termux

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ScrollView
import android.widget.TextView

/**
 * View для отображения терминального вывода.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val textView: TextView
    private var session: TerminalSession? = null
    private val outputBuffer = StringBuilder()

    init {
        textView = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFFE0E0E0.toInt())
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(16, 16, 16, 16)
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        addView(textView)
        setBackgroundColor(0xFF0D1117.toInt())
    }

    fun attachSession(session: TerminalSession) {
        this.session = session
        session.setOutputCallback { output ->
            post {
                appendOutput(output)
            }
        }
    }

    fun appendOutput(text: String) {
        outputBuffer.append(text)
        
        // Ограничить буфер
        val lines = outputBuffer.lines()
        if (lines.size > 1000) {
            outputBuffer.clear()
            outputBuffer.append(lines.takeLast(500).joinToString("\n"))
        }
        
        textView.text = outputBuffer.toString()
        post { fullScroll(FOCUS_DOWN) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val unicode = event.unicodeChar
            if (unicode != 0) {
                session?.write(unicode.toChar().toString())
                return true
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> { session?.write("\n"); return true }
                KeyEvent.KEYCODE_DEL -> { session?.write("\u007f"); return true }
                KeyEvent.KEYCODE_TAB -> { session?.write("\t"); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { session?.write("\u001b[A"); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { session?.write("\u001b[B"); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { session?.write("\u001b[D"); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { session?.write("\u001b[C"); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
