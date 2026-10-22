package com.remotebridge.ui.viewmodel

import com.remotebridge.BridgeApplication
import com.remotebridge.controller.ControllerManager
import com.remotebridge.controller.RemoteRepository

/**
 * حامل بسيط لمشاركة [RemoteRepository] بين الشاشات بطريقة كسولة وآمنة.
 */
object RepositoryHolder {
    val repository: RemoteRepository by lazy {
        RemoteRepository(
            ControllerManager(BridgeApplication.instance)
        )
    }
}
