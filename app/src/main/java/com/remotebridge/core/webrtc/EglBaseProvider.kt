package com.remotebridge.core.webrtc

import org.webrtc.EglBase

/**
 * مزوّد وحيد (Singleton) لسياق EGL.
 *
 * إنشاء EglBase عملية مكلفة، ويجب أن تتشارك كل مكوّنات الفيديو
 * (المصنع، المسار، العارض) نفس السياق لتفادي تسرّب الموارد.
 */
object EglBaseProvider {
    val eglBase: EglBase by lazy { EglBase.create() }

    val eglBaseContext: EglBase.Context
        get() = eglBase.eglBaseContext
}
