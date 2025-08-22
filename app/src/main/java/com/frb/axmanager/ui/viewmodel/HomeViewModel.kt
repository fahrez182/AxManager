package com.frb.axmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frb.engine.Axeron
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class AxeronServiceInfo(
        val versionName: String? = null,
        val versionCode: Long = -1,
        val uid: Int = -1,
        val pid: Int = -1,
        val selinuxContext: String? = null
    ) {
        fun isRunning(): Boolean {
            return Axeron.pingBinder()
        }

        fun getMode(): String {
            return when (uid) {
                -1 -> "Not Activated"
                0 -> "Root"
                2000 -> "Shell"
                else -> "User"
            }
        }
    }

    private val _axeronServiceInfo = MutableStateFlow(AxeronServiceInfo())
    val axeronServiceInfo: StateFlow<AxeronServiceInfo> = _axeronServiceInfo

    init {
        checkAxeronService()
    }

    fun checkAxeronService() {
        viewModelScope.launch {
            if (Axeron.pingBinder()) {
                _axeronServiceInfo.value = AxeronServiceInfo(
                    Axeron.getVersionName(),
                    Axeron.getVersionCode(),
                    Axeron.getUid(),
                    Axeron.getPid(),
                    Axeron.getSELinuxContext()
                )
            } else {
                _axeronServiceInfo.value = AxeronServiceInfo()
            }
        }
    }
}