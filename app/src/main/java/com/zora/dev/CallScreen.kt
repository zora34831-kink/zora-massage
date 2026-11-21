
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CallScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Panggilan",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Riwayat dan fitur panggilan ZORA",
            color = ZoraMuted
        )

        Spacer(Modifier.height(36.dp))

        Icon(
            Icons.Default.Call,
            contentDescription = null,
            modifier = Modifier.size(58.dp),
            tint = ZoraInk
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "Belum ada riwayat panggilan",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "Panggilan suara dan video akan tersedia setelah sistem WebRTC dipasang.",
            color = ZoraMuted
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Call, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Panggilan suara")
        }

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoCall, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Panggilan video")
        }
    }
}
