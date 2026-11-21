
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var readReceipts by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Pengaturan",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        Text("Privasi", style = MaterialTheme.typography.titleMedium)

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Laporan dibaca")
            Switch(
                checked = readReceipts,
                onCheckedChange = { readReceipts = it }
            )
        }

        HorizontalDivider()

        Text("Notifikasi", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Aktifkan notifikasi")
            Switch(
                checked = notifications,
                onCheckedChange = { notifications = it }
            )
        }

        HorizontalDivider()

        Spacer(Modifier.height(20.dp))

        Text("ZORA MASSAGE", style = MaterialTheme.typography.titleMedium)
        Text("Versi 1.0.0", color = ZoraMuted)
    }
}
