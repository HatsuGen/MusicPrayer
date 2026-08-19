package com.musicprayer.vibematch.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.holdClickable(
    selected: Boolean,
    clickLabel: String,
    holdLabel: String,
    onClick: () -> Unit,
    onHold: () -> Unit,
): Modifier = composed {
    val currentClick by rememberUpdatedState(onClick)
    val currentHold by rememberUpdatedState(onHold)
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current

    semantics(mergeDescendants = true) {
        role = Role.Button
        if (selected) {
            this.selected = true
            stateDescription = "Currently playing"
        }
        onClick(label = clickLabel) {
            currentClick()
            true
        }
        onLongClick(label = holdLabel) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            currentHold()
            true
        }
    }
        .indication(interactionSource, indication)
        .pointerInput(interactionSource) {
            coroutineScope gestureScope@ {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    var held = false
                    val holdJob = this@gestureScope.launch {
                        delay(HOLD_DURATION_MS)
                        held = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentHold()
                    }
                    val up = try {
                        waitForUpOrCancellation()
                    } finally {
                        holdJob.cancel()
                    }
                    if (up != null && !held) {
                        up.consume()
                        interactionSource.tryEmit(PressInteraction.Release(press))
                        currentClick()
                    } else {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                    }
                }
            }
        }
}

private const val HOLD_DURATION_MS = 1_000L
