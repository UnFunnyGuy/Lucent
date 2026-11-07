package app.lucent.firefly.core.common.network

import javax.inject.Qualifier

/**
 * Qualifier for an application-wide [kotlinx.coroutines.CoroutineScope].
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
