package app.lucent.firefly.feature.capture.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIEvent

sealed interface CaptureUiEvent : UIEvent {
    data object Dismiss : CaptureUiEvent
}
