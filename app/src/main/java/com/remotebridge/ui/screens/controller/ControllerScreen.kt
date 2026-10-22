package com.remotebridge.ui.screens.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbstudio.R
import com.remotebridge.core.signaling.SignalingState
import com.remotebridge.core.webrtc.PeerState
import com.remotebridge.ui.viewmodel.ControllerViewModel

/**
 * شاشة جهاز التحكم: تعرض الأجهزة المضيفة المتاحة وتتير الاتصال بها.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ControllerViewModel = viewModel(factory = ControllerViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val hosts by viewModel.hosts.collectAsState()

    // اتصل بخادم الإشارات عند فتح الشاشة
    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    // انتقل للوحة التحكم عند اكتمال الاتصال المباشر
    LaunchedEffect(state.peerState) {
        if (state.peerState == PeerState.Connected) {
            onConnected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.controller_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // مؤشر حالة الاتصال
            ConnectionStatusCard(state.signalingState, state.peerState)

            // قائمة الأجهزة المضيفة
            Text(
                text = stringResource(R.string.available_hosts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when {
                state.signalingState != SignalingState.Connected -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.scanning),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                hosts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_hosts),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hosts, key = { it.deviceId }) { host ->
                            HostCard(
                                hostName = host.deviceName,
                                isConnecting = state.peerState == PeerState.Connecting,
                                onConnect = { viewModel.connectToHost(host) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    viewModel.disconnect()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.disconnect))
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    signalingState: SignalingState,
    peerState: PeerState
) {
    val (text, color) = when {
        peerState == PeerState.Connected ->
            stringResource(R.string.peer_connected) to MaterialTheme.colorScheme.primary
        peerState == PeerState.Connecting ->
            stringResource(R.string.peer_connecting) to MaterialTheme.colorScheme.tertiary
        peerState == PeerState.Failed ->
            stringResource(R.string.peer_failed) to MaterialTheme.colorScheme.error
        signalingState == SignalingState.Connected ->
            stringResource(R.string.state_connected) to MaterialTheme.colorScheme.primary
        signalingState == SignalingState.Connecting ->
            stringResource(R.string.state_connecting) to MaterialTheme.colorScheme.tertiary
        signalingState == SignalingState.Failed ->
            stringResource(R.string.state_failed) to MaterialTheme.colorScheme.error
        else ->
            stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (signalingState == SignalingState.Connected)
                    Icons.Default.Link else Icons.Default.LinkOff,
                contentDescription = null,
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}

@Composable
private fun HostCard(
    hostName: String,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hostName,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Button(onClick = onConnect) {
                    Text(stringResource(R.string.connect))
                }
            }
        }
    }
}
