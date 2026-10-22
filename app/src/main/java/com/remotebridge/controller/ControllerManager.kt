package com.remotebridge.controller

import android.content.Context
import android.util.Log
import com.remotebridge.core.model.BridgeRequest
import com.remotebridge.core.model.BridgeResponse
import com.remotebridge.core.model.HostInfo
import com.remotebridge.core.signaling.Signal
import com.remotebridge.core.signaling.SignalingClient
import com.remotebridge.core.signaling.SignalingState
import com.remotebridge.core.webrtc.PeerConnectionManager
import com.remotebridge.core.webrtc.PeerState
import com.remotebridge.host.service.DeviceIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * حالة جهاز التحكم في لحظة معينة.
 */
data class ControllerState(
    val signalingState: SignalingState = SignalingState.Disconnected,
    val peerState: PeerState = PeerState.Idle,
    val connectedHost: HostInfo? = null,
    val hosts: List<HostInfo> = emptyList()
)

/**
 * يدير اتصال جهاز التحكم بالأجهزة المضيفة.
 *
 * المسؤوليات:
 * 1. الاتصال بخادم الإشارات بدور "تحكم".
 * 2. اكتشاف الأجهزة المضيفة المتاحة.
 * 3. بدء اتصال WebRTC مع مضيف محدد.
 * 4. إرسال الطلبات واستقبال الردود عبر قناة البيانات.
 */
class ControllerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "ControllerManager"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val requestIdCounter = AtomicInteger(0)

    private var signaling: SignalingClient? = null
    private var peerManager: PeerConnectionManager? = null
    private var signalJob: Job? = null
    private var myDeviceId: String = ""

    /**
     * الردود الواردة مفهرسة بمعرّف الطلب لمطابقتها.
     */
    private val pendingResponses = ConcurrentHashMap<Int, MutableSharedFlow<BridgeResponse>>()

    private val _state = MutableStateFlow(ControllerState())
    val state: StateFlow<ControllerState> = _state.asStateFlow()

    private val _hosts = MutableStateFlow<List<HostInfo>>(emptyList())
    val hosts: StateFlow<List<HostInfo>> = _hosts.asStateFlow()

    private val _binaryChunks = MutableSharedFlow<PeerConnectionManager.BinaryChunk>(
        extraBufferCapacity = 32
    )
    val binaryChunks: SharedFlow<PeerConnectionManager.BinaryChunk> =
        _binaryChunks.asSharedFlow()

    /**
     * يتصل بخادم الإشارات ويبدأ الاستماع للأجهزة المضيفة.
     */
    suspend fun connect(serverUrl: String, deviceName: String) {
        if (signaling != null) return
        myDeviceId = DeviceIdentifier.getOrGenerate(context)

        peerManager = PeerConnectionManager(context, scope).also { it.start() }

        signaling = SignalingClient(
            url = serverUrl,
            deviceId = myDeviceId,
            role = "controller",
            deviceName = deviceName,
            scope = scope,
            onMessage = { handleSignal(it) },
            onState = { signalingState ->
                _state.value = _state.value.copy(signalingState = signalingState)
            }
        ).also { it.connect() }

        collectPeerEvents()
    }

    /**
     * يعالج رسالة إشارات واردة.
     */
    private fun handleSignal(signal: Signal) {
        when (signal) {
            is Signal.Hosts -> {
                _hosts.value = signal.hosts
                _state.value = _state.value.copy(hosts = signal.hosts)
            }

            is Signal.HostOnline -> {
                val updated = (_hosts.value + signal.host).distinctBy { it.deviceId }
                _hosts.value = updated
                _state.value = _state.value.copy(hosts = updated)
            }

            is Signal.HostOffline -> {
                val updated = _hosts.value.filterNot { it.deviceId == signal.deviceId }
                _hosts.value = updated
                _state.value = _state.value.copy(hosts = updated)
            }

            is Signal.Answer -> {
                // المضيف قبل اتصالنا وردّ بالإجابة
                peerManager?.setRemoteAnswer(signal.sdp, signal.sdpType)
            }

            is Signal.Ice -> {
                peerManager?.addIceCandidate(signal.sdpMid, signal.sdpMLineIndex, signal.sdp)
            }

            is Signal.Reject -> {
                Log.w(TAG, "رفض المضيف الاتصال: ${signal.reason}")
                _state.value = _state.value.copy(peerState = PeerState.Failed)
            }

            is Signal.Pong -> { /* نبض القلب */ }
            is Signal.Registered -> {
                Log.d(TAG, "تم تسجيل جهاز التحكم: ${signal.ok}")
            }

            else -> {}
        }
    }

    /**
     * يجمع أحداث قناة WebRTC.
     */
    private fun collectPeerEvents() {
        val manager = peerManager ?: return
        signalJob?.cancel()
        signalJob = scope.launch {
            // أرسل مرشّحات ICE المحلية للمضيف
            launch {
                manager.iceCandidates.collectLatest { candidate ->
                    val hostId = _state.value.connectedHost?.deviceId ?: return@collectLatest
                    signaling?.send(
                        Signal.Ice(
                            from = myDeviceId,
                            to = hostId,
                            sdp = candidate.sdp,
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex
                        )
                    )
                }
            }

            // وجّه الردود الواردة إلى الـ Flow المنتظر لها
            launch {
                manager.incomingResponses.collectLatest { response ->
                    pendingResponses.remove(response.id)?.tryEmit(response)
                }
            }

            // وجّه القطع الثنائية الواردة
            launch {
                manager.incomingBinary.collectLatest { chunk ->
                    _binaryChunks.tryEmit(chunk)
                }
            }

            // راقب حالة قناة البيانات
            launch {
                manager.state.collectLatest { peerState ->
                    _state.value = _state.value.copy(peerState = peerState)
                }
            }
        }
    }

    /**
     * يبدأ اتصالاً مع جهاز مضيف محدد.
     */
    fun connectToHost(host: HostInfo) {
        val manager = peerManager ?: return
        _state.value = _state.value.copy(
            connectedHost = host,
            peerState = PeerState.Connecting
        )
        manager.createOffer(
            onSuccess = { offerSdp ->
                signaling?.send(
                    Signal.Offer(
                        from = myDeviceId,
                        to = host.deviceId,
                        sdp = offerSdp,
                        sdpType = "OFFER"
                    )
                )
            },
            onFailure = { error ->
                Log.e(TAG, "فشل إنشاء العرض: $error")
                _state.value = _state.value.copy(peerState = PeerState.Failed)
            }
        )
    }

    /**
     * يرسل طلباً للمضيف ويعيد Flow يصدر رداً واحداً عند وصوله.
     */
    fun sendRequest(cmd: String, params: Map<String, String> = emptyMap()): MutableSharedFlow<BridgeResponse> {
        val id = requestIdCounter.incrementAndGet()
        val responseFlow = MutableSharedFlow<BridgeResponse>(extraBufferCapacity = 1)
        pendingResponses[id] = responseFlow
        val request = BridgeRequest(
            id = id,
            cmd = cmd,
            params = buildParamsJson(params)
        )
        val manager = peerManager
        if (manager == null || !manager.sendRequest(request)) {
            responseFlow.tryEmit(BridgeResponse(id, false, "الاتصال غير جاهز", null))
            pendingResponses.remove(id)
        }
        return responseFlow
    }

    /**
     * يحوّل خريطة المعاملات إلى JsonObject.
     */
    private fun buildParamsJson(params: Map<String, String>): kotlinx.serialization.json.JsonObject {
        val obj = kotlinx.serialization.json.buildJsonObject {
            params.forEach { (key, value) ->
                put(
                    key,
                    kotlinx.serialization.json.JsonPrimitive(value)
                )
            }
        }
        return obj
    }

    /**
     * يقطع الاتصال مع المضيف الحالي.
     */
    fun disconnectFromHost() {
        peerManager?.close()
        peerManager = null
        _state.value = _state.value.copy(
            connectedHost = null,
            peerState = PeerState.Idle
        )
        // أعد إنشاء المدير لاتصال لاحق
        peerManager = PeerConnectionManager(context, scope).also { it.start() }
        collectPeerEvents()
    }

    /**
     * يقطع كل الاتصالات ويحرّر الموارد.
     */
    fun disconnect() {
        signalJob?.cancel()
        peerManager?.close()
        peerManager = null
        signaling?.disconnect()
        signaling = null
        pendingResponses.clear()
        _state.value = ControllerState()
        _hosts.value = emptyList()
    }
}
