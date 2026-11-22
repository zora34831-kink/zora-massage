
package com.zora.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ZoraBlue = Color(0xFF2589F5)

@Composable
fun VerifiedBadge(
    verified: Boolean,
    modifier: Modifier = Modifier
) {
    if (verified) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Centang biru",
            tint = ZoraBlue,
            modifier = modifier.size(17.dp)
        )
    }
}

@Composable
fun VerifiedName(
    name: String,
    verified: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium
        )

        VerifiedBadge(verified = verified)
    }
}
