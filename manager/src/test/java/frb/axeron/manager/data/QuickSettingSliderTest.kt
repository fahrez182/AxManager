package frb.axeron.manager.data

import frb.axeron.manager.data.QuickSetting.Control.Slider
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSettingSliderTest {

    @Test
    fun decimalStepSnapsAndFormatsWithoutFloatNoise() {
        val s = Slider(decimal = true, min = 0.0, max = 1.0, step = 0.001)
        assertEquals(3, s.decimals)
        assertEquals("0.123", s.format(0.12345))
        assertEquals("0.3", s.format(0.1 + 0.2)) // 0.30000000000000004
        assertEquals("1", s.format(5.0)) // clamped to max
        assertEquals("0", s.format(-1.0)) // clamped to min
    }

    @Test
    fun integerSliderWritesWholeNumbersOnTheStepGrid() {
        val s = Slider(decimal = false, min = 10.0, max = 100.0, step = 5.0)
        assertEquals(0, s.decimals)
        assertEquals("25", s.format(23.0)) // grid starts at min: 10, 15, 20, 25
        assertEquals("100", s.format(99.0))
    }

    @Test
    fun stepOffsetFromMinKeepsItsGrid() {
        val s = Slider(decimal = true, min = 0.5, max = 2.0, step = 0.25)
        assertEquals("1.25", s.format(1.3))
        assertEquals("0.5", s.format(0.6))
    }
}
