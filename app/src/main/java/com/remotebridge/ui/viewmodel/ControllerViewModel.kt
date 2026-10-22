package com.remotebridge.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.remotebridge.BridgeApplication
import com.remotebridge.controller.ControllerManager
import com.remotebridge.controller.RemoteRepository
import com.remotebridge.core.data.SettingsRepository
import com.remotebridge.core.model.HostInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * نموذج عرض شاشة جهاز التحكم.
 * يدير الاتصال بخادم الإشارات وقائمة الأجهزة المضيفة.
 */
class ControllerViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    val controllerManager = RepositoryHolder.repository.manager
    val remoteRepository = RepositoryHolder.repository

    val serverUrl: StateFlow<String> = settings.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val deviceName: StateFlow<String> = settings.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val state = controllerManager.state

    val hosts = controllerManager.hosts

    /**
     * يتصل بخادم الإشارات.
     */
    fun connect() {
        viewModelScope.launch {
            val url = serverUrl.value.ifBlank { HostViewModel.DEFAULT_LOCAL_SERVER }
            val name = deviceName.value.ifBlank { "جهاز تحكم" }
            controllerManager.connect(url, name)
        }
    }

    /**
     * يتصل بمضيف محدد.
     */
    fun connectToHost(host: HostInfo) {
        controllerManager.connectToHost(host)
    }

    /**
     * يقطع الاتصال.
     */
    fun disconnect() {
        controllerManager.disconnect()
    }

    override fun onCleared() {
        controllerManager.disconnect()
        super.onCleared()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = SettingsRepository(BridgeApplication.instance)
                return ControllerViewModel(repository) as T
            }
        }
    }
}
