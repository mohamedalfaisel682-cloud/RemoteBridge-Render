package com.remotebridge.core.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * تنفيذ مبسّط لـ SdpObserver لتقليل الأكواد المتكررة.
 * [onSuccess] يُستدعى عند نجاح ضبط الوصف.
 */
class SimpleSdpObserver(
    private val onSuccess: () -> Unit = {},
    private val onFailure: (String?) -> Unit = {}
) : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onCreateFailure(error: String?) {
        onFailure(error)
    }

    override fun onSetSuccess() {
        onSuccess()
    }

    override fun onSetFailure(error: String?) {
        onFailure(error)
    }
}
