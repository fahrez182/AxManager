package com.frb.engine.utils

import com.frb.engine.client.AxeronBinderWrapper
import rikka.hidden.compat.util.SystemServiceBinder

object AxSystemApis {

    init {
        SystemServiceBinder.setOnGetBinderListener {
            return@setOnGetBinderListener AxeronBinderWrapper(it)
        }
    }

}