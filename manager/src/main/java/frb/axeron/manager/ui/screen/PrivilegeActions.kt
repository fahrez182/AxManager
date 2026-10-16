package frb.axeron.manager.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import frb.axeron.manager.R
import frb.axeron.manager.data.PackageControl

/** What a package action's outcome says in a snackbar, the same in the list and on the detail page. */
internal class OutcomeTexts(
    private val done: String,
    private val failed: String,
    private val partialCache: String,
    private val needsServer: String,
    private val needsRoot: String,
) {
    fun of(outcome: PackageControl.Outcome, label: String): String = when {
        outcome.ok && outcome.partial -> partialCache
        outcome.ok -> done.format(label)
        outcome.serverOff -> needsServer
        outcome.needsRoot -> failed.format(label, needsRoot)
        else -> failed.format(label, outcome.error.orEmpty())
    }
}

@Composable
internal fun outcomeTexts() = OutcomeTexts(
    done = stringResource(R.string.privilege_action_done),
    failed = stringResource(R.string.privilege_action_failed),
    partialCache = stringResource(R.string.privilege_clear_cache_partial),
    needsServer = stringResource(R.string.privilege_needs_server),
    needsRoot = stringResource(R.string.privilege_needs_root),
)
