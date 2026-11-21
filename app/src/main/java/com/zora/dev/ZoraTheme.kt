
package com.zora.dev

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ZoraCream = Color(0xFFFFF9E9)
val ZoraYellow = Color(0xFFFFD94D)
val ZoraInk = Color(0xFF171717)
val ZoraMuted = Color(0xFF77736B)
val ZoraWhite = Color(0xFFFFFFFF)
val ZoraLine = Color(0xFF242424)

@Composable
fun ZoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ZoraYellow,
            onPrimary = ZoraInk,
            background = ZoraCream,
            onBackground = ZoraInk,
            surface = ZoraWhite,
            onSurface = ZoraInk,
            secondary = ZoraInk,
            outline = ZoraLine
        ),
        content = content
    )
}
