package app.lucent.firefly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard spatial and dimensional tokens for Lucent design system.
 */
@Immutable
data class Dimensions(
    // ==== Paddings & Margins ====
    val paddingNone: Dp = 0.dp,
    val paddingXxSmall: Dp = 2.dp,
    val paddingXSmall: Dp = 4.dp,
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 16.dp,
    val paddingLarge: Dp = 24.dp,
    val paddingXLarge: Dp = 32.dp,
    val paddingXxLarge: Dp = 48.dp,
    val paddingHuge: Dp = 64.dp,

    // ==== Corner Radii ====
    val radiusNone: Dp = 0.dp,
    val radiusXxSmall: Dp = 2.dp,
    val radiusXSmall: Dp = 4.dp,
    val radiusSmall: Dp = 8.dp,
    val radiusMedium: Dp = 12.dp,
    val radiusLarge: Dp = 16.dp,
    val radiusXLarge: Dp = 24.dp,
    val radiusFull: Dp = 999.dp,

    // ==== Borders & Outlines ====
    val borderHairline: Dp = 0.5.dp,
    val borderThin: Dp = 1.dp,
    val borderRegular: Dp = 2.dp,
    val borderThick: Dp = 4.dp,

    // ==== Elevations ====
    val elevationNone: Dp = 0.dp,
    val elevationSmall: Dp = 2.dp,
    val elevationMedium: Dp = 4.dp,
    val elevationLarge: Dp = 8.dp,
    val elevationXLarge: Dp = 16.dp,

    // ==== Icons ====
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconXLarge: Dp = 40.dp,
    val iconHuge: Dp = 48.dp,

    // ==== Component Sizing ====
    val minTouchTarget: Dp = 48.dp,
    val buttonHeightSmall: Dp = 36.dp,
    val buttonHeightNormal: Dp = 48.dp,
    val buttonHeightLarge: Dp = 56.dp,
    val inputHeightNormal: Dp = 56.dp,
    val cardMinHeight: Dp = 80.dp,
    val cardPadding: Dp = 16.dp,
    val toolbarHeight: Dp = 64.dp,
    val navBarHeight: Dp = 80.dp,
    val dividerThickness: Dp = 1.dp,
    val bottomSheetPeekHeight: Dp = 64.dp,
    val avatarSmall: Dp = 28.dp,
    val avatarMedium: Dp = 40.dp,
    val avatarLarge: Dp = 56.dp,
    val progressIndicatorSize: Dp = 48.dp,
)

val DefaultDimensions = Dimensions()

val LocalDimensions = staticCompositionLocalOf { DefaultDimensions }
