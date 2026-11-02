package com.bertschell.immichcontacts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Coroutine-driven braille spinner. Deliberately NOT built on
 * ValueAnimator/InfiniteTransition because those freeze when the device
 * has system animations removed (Developer option "Animation scale = 0").
 * Coroutines keep ticking regardless.
 */
private val FRAMES = charArrayOf('⠋','⠙','⠹','⠸','⠼','⠴','⠦','⠧','⠇','⠏')

@Composable
fun PulsingLoader(size: Dp = 48.dp) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(90)
            frame = (frame + 1) % FRAMES.size
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Text(
            FRAMES[frame].toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
