package frb.axeron.manager.ui.component

import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Parcelable
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import frb.axeron.manager.ui.theme.AppShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.noties.markwon.Markwon
import io.noties.markwon.utils.NoCopySpannableFactory
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

private const val TAG = "DialogComponent"

interface ConfirmDialogVisuals : Parcelable {
    val dragHandle: Boolean
    val title: String
    val content: String
    val isMarkdown: Boolean
    val confirm: String?
    val dismiss: String?
    val neutral: String?
}

@Parcelize
private data class ConfirmDialogVisualsImpl(
    override val title: String,
    override val content: String,
    override val dragHandle: Boolean,
    override val isMarkdown: Boolean,
    override val confirm: String?,
    override val dismiss: String?,
    override val neutral: String?
) : ConfirmDialogVisuals {
    companion object {
        val Empty: ConfirmDialogVisuals = ConfirmDialogVisualsImpl(
            "", "",
            dragHandle = false,
            isMarkdown = false,
            confirm = null,
            dismiss = null,
            neutral = null
        )
    }
}

interface DialogHandle {
    val isShown: Boolean
    val dialogType: String
    fun show()
    fun hide()
}

interface LoadingDialogHandle : DialogHandle {
    suspend fun <R> withLoading(block: suspend () -> R): R
    fun showLoading()
}

sealed interface ConfirmResult {
    object Confirmed : ConfirmResult
    object Dismissed : ConfirmResult
    object Neutral : ConfirmResult
}

interface ConfirmDialogHandle : DialogHandle {
    val visuals: ConfirmDialogVisuals

    fun showConfirm(
        title: String,
        content: String,
        dragHandle: Boolean = false,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null,
        neutral: String? = null
    )

    suspend fun awaitConfirm(
        title: String,
        content: String,
        dragHandle: Boolean = false,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null,
        neutral: String? = null
    ): ConfirmResult
}

private abstract class DialogHandleBase(
    val visible: MutableState<Boolean>,
    val coroutineScope: CoroutineScope
) : DialogHandle {
    override val isShown: Boolean
        get() = visible.value

    override fun show() {
        coroutineScope.launch {
            visible.value = true
        }
    }

    final override fun hide() {
        coroutineScope.launch {
            visible.value = false
        }
    }

    override fun toString(): String {
        return dialogType
    }
}

private class LoadingDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope
) : LoadingDialogHandle, DialogHandleBase(visible, coroutineScope) {
    override suspend fun <R> withLoading(block: suspend () -> R): R {
        return coroutineScope.async {
            try {
                visible.value = true
                block()
            } finally {
                visible.value = false
            }
        }.await()
    }

    override fun showLoading() {
        show()
    }

    override val dialogType: String get() = "LoadingDialog"
}

typealias NullableCallback = (() -> Unit)?

interface ConfirmCallback {

    val onConfirm: NullableCallback

    val onDismiss: NullableCallback

    val onNeutral: NullableCallback

    val isEmpty: Boolean get() = onConfirm == null && onDismiss == null

    companion object {
        operator fun invoke(
            onConfirmProvider: () -> NullableCallback,
            onDismissProvider: () -> NullableCallback,
            onNeutralProvider: () -> NullableCallback
        ): ConfirmCallback {
            return object : ConfirmCallback {
                override val onConfirm: NullableCallback
                    get() = onConfirmProvider()
                override val onDismiss: NullableCallback
                    get() = onDismissProvider()
                override val onNeutral: NullableCallback
                    get() = null
            }
        }
    }
}

private class ConfirmDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope,
    callback: ConfirmCallback,
    override var visuals: ConfirmDialogVisuals = ConfirmDialogVisualsImpl.Empty,
    private val resultFlow: ReceiveChannel<ConfirmResult>
) : ConfirmDialogHandle, DialogHandleBase(visible, coroutineScope) {
    private class ResultCollector(
        private val callback: ConfirmCallback
    ) : FlowCollector<ConfirmResult> {
        fun handleResult(result: ConfirmResult) {
            Log.d(TAG, "handleResult: ${result.javaClass.simpleName}")
            when (result) {
                ConfirmResult.Confirmed -> onConfirm()
                ConfirmResult.Dismissed -> onDismiss()
                ConfirmResult.Neutral -> onNeutral()
            }
        }

        fun onConfirm() {
            callback.onConfirm?.invoke()
        }

        fun onDismiss() {
            callback.onDismiss?.invoke()
        }

        fun onNeutral() {
            callback.onNeutral?.invoke()
        }

        override suspend fun emit(value: ConfirmResult) {
            handleResult(value)
        }
    }

    private val resultCollector = ResultCollector(callback)

    private var awaitContinuation: CancellableContinuation<ConfirmResult>? = null

    private val isCallbackEmpty = callback.isEmpty

    init {
        coroutineScope.launch {
            resultFlow
                .consumeAsFlow()
                .onEach { result ->
                    awaitContinuation?.let {
                        awaitContinuation = null
                        if (it.isActive) {
                            it.resume(result)
                        }
                    }
                }
                .onEach { hide() }
                .collect(resultCollector)
        }
    }

    private suspend fun awaitResult(): ConfirmResult {
        return suspendCancellableCoroutine {
            awaitContinuation = it.apply {
                if (isCallbackEmpty) {
                    invokeOnCancellation {
                        visible.value = false
                    }
                }
            }
        }
    }

    fun updateVisuals(visuals: ConfirmDialogVisuals) {
        this.visuals = visuals
    }

    override fun show() {
        if (visuals !== ConfirmDialogVisualsImpl.Empty) {
            super.show()
        } else {
            throw UnsupportedOperationException("can't show confirm dialog with the Empty visuals")
        }
    }

    override fun showConfirm(
        title: String,
        content: String,
        dragHandle: Boolean,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?,
        neutral: String?
    ) {
        coroutineScope.launch {
            updateVisuals(
                ConfirmDialogVisualsImpl(
                    title,
                    content,
                    dragHandle,
                    markdown,
                    confirm,
                    dismiss,
                    neutral
                )
            )
            show()
        }
    }

    override suspend fun awaitConfirm(
        title: String,
        content: String,
        dragHandle: Boolean,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?,
        neutral: String?
    ): ConfirmResult {
        coroutineScope.launch {
            updateVisuals(
                ConfirmDialogVisualsImpl(
                    title,
                    content,
                    dragHandle,
                    markdown,
                    confirm,
                    dismiss,
                    neutral
                )
            )
            show()
        }
        return awaitResult()
    }

    override val dialogType: String get() = "ConfirmDialog"

    override fun toString(): String {
        return "${super.toString()}(visuals: $visuals)"
    }

    companion object {
        fun Saver(
            visible: MutableState<Boolean>,
            coroutineScope: CoroutineScope,
            callback: ConfirmCallback,
            resultChannel: ReceiveChannel<ConfirmResult>
        ) = Saver<ConfirmDialogHandle, ConfirmDialogVisuals>(
            save = {
                it.visuals
            },
            restore = {
                Log.d(TAG, "ConfirmDialog restore, visuals: $it")
                ConfirmDialogHandleImpl(visible, coroutineScope, callback, it, resultChannel)
            }
        )
    }
}

private class CustomDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope
) : DialogHandleBase(visible, coroutineScope) {
    override val dialogType: String get() = "CustomDialog"
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()

    if (visible.value) {
        LoadingDialog()
    }

    return remember {
        LoadingDialogHandleImpl(visible, coroutineScope)
    }
}

@Composable
private fun rememberConfirmDialog(
    visuals: ConfirmDialogVisuals,
    callback: ConfirmCallback
): ConfirmDialogHandle {
    val visible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val resultChannel = remember {
        Channel<ConfirmResult>()
    }

    val handle = rememberSaveable(
        saver = ConfirmDialogHandleImpl.Saver(visible, coroutineScope, callback, resultChannel),
        init = {
            ConfirmDialogHandleImpl(visible, coroutineScope, callback, visuals, resultChannel)
        }
    )

    if (visible.value) {
        ConfirmDialog(
            handle.visuals,
            confirm = { coroutineScope.launch { resultChannel.send(ConfirmResult.Confirmed) } },
            dismiss = { coroutineScope.launch { resultChannel.send(ConfirmResult.Dismissed) } },
            neutral = { coroutineScope.launch { resultChannel.send(ConfirmResult.Neutral) } }
        )
    }

    return handle
}

@Composable
fun rememberConfirmCallback(
    onConfirm: NullableCallback,
    onDismiss: NullableCallback,
    onNeutral: NullableCallback
): ConfirmCallback {
    val currentOnConfirm by rememberUpdatedState(newValue = onConfirm)
    val currentOnDismiss by rememberUpdatedState(newValue = onDismiss)
    val currentOnNeutral by rememberUpdatedState(newValue = onNeutral)
    return remember {
        ConfirmCallback({ currentOnConfirm }, { currentOnDismiss }, { currentOnNeutral })
    }
}

@Composable
fun rememberConfirmDialog(
    onConfirm: NullableCallback = null,
    onDismiss: NullableCallback = null,
    onNeutral: NullableCallback = null
): ConfirmDialogHandle {
    return rememberConfirmDialog(rememberConfirmCallback(onConfirm, onDismiss, onNeutral))
}

@Composable
fun rememberConfirmDialog(callback: ConfirmCallback): ConfirmDialogHandle {
    return rememberConfirmDialog(ConfirmDialogVisualsImpl.Empty, callback)
}

@Composable
fun rememberCustomDialog(composable: @Composable (dismiss: () -> Unit) -> Unit): DialogHandle {
    val visible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    if (visible.value) {
        composable { visible.value = false }
    }
    return remember {
        CustomDialogHandleImpl(visible, coroutineScope)
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = Modifier.size(100.dp), shape = AppShape.R8
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDialog(
    visuals: ConfirmDialogVisuals,
    confirm: () -> Unit,
    dismiss: () -> Unit,
    neutral: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    MaterialBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                dismiss()
            }
        },
        dragHandle = when {
            visuals.dragHandle -> {
                { BottomSheetDefaults.DragHandle() }
            }

            else -> {
                { Spacer(Modifier.size(8.dp)) }
            }
        },
        sheetState = sheetState,
        title = { Text(visuals.title, fontWeight = FontWeight.SemiBold) },
        text = {
            if (visuals.isMarkdown) {
                MarkdownContent(content = visuals.content)
            } else {
                Text(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = visuals.content
                )
            }
        },
        confirmButton = {
            visuals.confirm?.let {
                TextButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                        confirm()
                    }
                }) {
                    Text(text = it)
                }
            }
        },
        dismissButton = {
            visuals.dismiss?.let {
                TextButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                        dismiss()
                    }

                }) {
                    Text(text = it)
                }
            }
        },
        neutralButton = {
            visuals.neutral?.let {
                TextButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                        neutral()
                    }
                }) {
                    Text(text = it)
                }
            }
        }
    )
}

@Composable
private fun MarkdownContent(content: String) {
    val contentColor = LocalContentColor.current

    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setSpannableFactory(NoCopySpannableFactory.getInstance())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                }
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        update = {
            Markwon.create(it.context).setMarkdown(it, content)
            it.setTextColor(contentColor.toArgb())
        }
    )
}
