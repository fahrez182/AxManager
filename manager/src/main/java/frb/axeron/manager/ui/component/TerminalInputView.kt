package frb.axeron.manager.ui.component

import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

class TerminalInputView(context: Context) : View(context) {
    var onTextInput: (String) -> Unit = {}
    var onActionKey: (Int) -> Unit = {}

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        Log.i("TerminalInputView", "LOG: Input connection established")
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_FULLSCREEN

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.let {
                    Log.i("TerminalInputView", "LOG: Keyboard input received: $it")
                    onTextInput(it.toString())
                }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                Log.i("TerminalInputView", "LOG: Physical key event: ${event.keyCode} action: ${event.action}")
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onKeyDown(event.keyCode, event)
                }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                Log.d("TerminalInputView", "LOG: deleteSurroundingText: $beforeLength, $afterLength")
                if (beforeLength > 0 && afterLength == 0) {
                    repeat(beforeLength) {
                        onActionKey(KeyEvent.KEYCODE_DEL)
                    }
                    return true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return false
        Log.i("TerminalInputView", "LOG: Key event received: $keyCode")
        onActionKey(keyCode)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return true
    }

    fun requestTerminalFocus() {
        post {
            Log.i("TerminalInputView", "LOG: Terminal focus requested")
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            Log.i("TerminalInputView", "LOG: Keyboard requested")
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
