package com.termux

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView

/**
 * Custom terminal view that displays terminal output and handles input.
 * 
 * This is a simplified implementation. For production, use the Termux terminal-view library:
 * https://github.com/termux/termux-app/tree/master/terminal-view
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val textView: TextView
    private var session: TerminalEmulator? = null
    private val outputBuffer = StringBuilder()
    private var maxLines = 500

    init {
        textView = TextView(context).apply {
            textSize = 12f
            setTextColor(0xFFE0E0E0.toInt())
            setBackgroundColor(0xFF1A1A2E.toInt())
            setPadding(16, 16, 16, 16)
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        addView(textView)
        setBackgroundColor(0xFF1A1A2E.toInt())
    }

    /**
     * Attaches a terminal session to this view
     */
    fun attachSession(session: TerminalEmulator) {
        this.session = session
        session.setOutputCallback { output ->
            post {
                appendOutput(output)
            }
        }
    }

    /**
     * Appends output to the terminal display
     */
    fun appendOutput(text: String) {
        outputBuffer.append(text)
        
        // Trim buffer if too large
        val lines = outputBuffer.lines()
        if (lines.size > maxLines) {
            outputBuffer.clear()
            outputBuffer.append(lines.takeLast(maxLines).joinToString("\n"))
        }
        
        textView.text = outputBuffer.toString()
        
        // Auto-scroll to bottom
        post {
            fullScroll(FOCUS_DOWN)
        }
    }

    /**
     * Clears the terminal display
     */
    fun clearScreen() {
        outputBuffer.clear()
        textView.text = ""
    }

    /**
     * Handles keyboard input
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val char = event.unicodeChar.toChar()
            if (char != '\u0000') {
                session?.writeToTerminal(char.toString())
                return true
            }
            
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    session?.writeToTerminal("\n")
                    return true
                }
                KeyEvent.KEYCODE_DEL -> {
                    session?.writeToTerminal("\u007f")
                    return true
                }
                KeyEvent.KEYCODE_TAB -> {
                    session?.writeToTerminal("\t")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    session?.writeToTerminal("\u001b[A")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    session?.writeToTerminal("\u001b[B")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    session?.writeToTerminal("\u001b[D")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    session?.writeToTerminal("\u001b[C")
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Sets the font size
     */
    fun setFontSize(sizeSp: Float) {
        textView.textSize = sizeSp
    }
}
