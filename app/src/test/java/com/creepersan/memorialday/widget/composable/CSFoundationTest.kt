package com.creepersan.memorialday.widget.composable

import com.creepersan.memorialday.widget.composable.token.csClampedIndex
import com.creepersan.memorialday.widget.composable.token.csClampedProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class CSFoundationTest {
    @Test
    fun progress_is_clamped_to_valid_range() {
        assertEquals(0f, (-1f).csClampedProgress())
        assertEquals(0.5f, 0.5f.csClampedProgress())
        assertEquals(1f, 2f.csClampedProgress())
    }

    @Test
    fun index_is_clamped_and_empty_is_safe() {
        assertEquals(0, (-1).csClampedIndex(3))
        assertEquals(2, 8.csClampedIndex(3))
        assertEquals(0, 4.csClampedIndex(0))
    }
}
