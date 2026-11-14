package app.lucent.firefly.core.ui

import app.lucent.firefly.core.designsystem.theme.DefaultDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScreenWrapperTest {
    @Test
    fun screenWrapperDefaultsAreConsistent() {
        assertNotNull(DefaultDimensions.paddingMedium)
        assertEquals(16, DefaultDimensions.paddingMedium.value.toInt())
    }
}
