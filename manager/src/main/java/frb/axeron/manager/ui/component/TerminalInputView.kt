package frb.axeron.manager.ui.component

import android.content.Context
import android.text.InputType
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
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.let { onTextInput(it.toString()) }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onActionKey(event.keyCode)
                }
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && afterLength == 0) {
                    onActionKey(KeyEvent.KEYCODE_DEL)
                    return true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    fun requestTerminalFocus() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}
