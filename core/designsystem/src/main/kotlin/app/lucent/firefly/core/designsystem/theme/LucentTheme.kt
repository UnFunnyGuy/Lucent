package app.lucent.firefly.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Accessor for Lucent design system tokens.
 */
object LucentTheme {
    val colors: LucentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: LucentTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}

/**
 * Lucent theme wrapper providing Material 3 components and custom [LucentColors],
 * [LucentTypography], and [Dimensions] via CompositionLocal.
 */
@Composable
fun LucentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val materialColorScheme =
        when {
            supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
            supportsDynamic && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> LucentMaterialDarkColorScheme
            else -> LucentMaterialLightColorScheme
        }

    val customColors = if (darkTheme) LucentDarkColors else LucentLightColors

    CompositionLocalProvider(
        LocalColors provides customColors,
        LocalTypography provides DefaultLucentTypography,
        LocalDimensions provides DefaultDimensions,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = LucentMaterialTypography,
            content = content,
        )
    }
}
