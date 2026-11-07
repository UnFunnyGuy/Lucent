package app.lucent.firefly.core.common.base

/**
 * Represents the immutable state of the UI at any point in time.
 */
interface UIState

/**
 * Represents one-time side effects emitted by ViewModel (e.g. Navigation, Toast, Snackbar).
 */
interface UIEvent

/**
 * Represents user interactions or intents triggered from the UI layer (e.g. Button click, text change).
 */
interface UIAction
