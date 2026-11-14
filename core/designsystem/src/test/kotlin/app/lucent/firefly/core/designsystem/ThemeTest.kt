package app.lucent.firefly.core.designsystem

import androidx.compose.ui.unit.dp
import app.lucent.firefly.core.designsystem.theme.DefaultDimensions
import app.lucent.firefly.core.designsystem.theme.DefaultLucentTypography
import app.lucent.firefly.core.designsystem.theme.LucentDarkColors
import app.lucent.firefly.core.designsystem.theme.LucentLightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeTest {
    @Test
    fun financialColorsAreDefined() {
        assertNotNull(LucentLightColors.income)
        assertNotNull(LucentLightColors.expense)
        assertNotNull(LucentLightColors.transfer)

        assertNotNull(LucentDarkColors.income)
        assertNotNull(LucentDarkColors.expense)
        assertNotNull(LucentDarkColors.transfer)

        // Light and dark financial colors should differ for contrast
        assertNotEquals(LucentLightColors.income, LucentDarkColors.income)
        assertNotEquals(LucentLightColors.expense, LucentDarkColors.expense)
    }

    @Test
    fun dimensionsAreConsistent() {
        assertEquals(0.dp, DefaultDimensions.paddingNone)
        assertEquals(16.dp, DefaultDimensions.paddingMedium)
        assertEquals(24.dp, DefaultDimensions.paddingLarge)
        assertEquals(48.dp, DefaultDimensions.minTouchTarget)
    }

    @Test
    fun typographyScalesAreDefined() {
        assertNotNull(DefaultLucentTypography.h1)
        assertNotNull(DefaultLucentTypography.bodyMedium)
        assertNotNull(DefaultLucentTypography.labelSmall)
    }
}
