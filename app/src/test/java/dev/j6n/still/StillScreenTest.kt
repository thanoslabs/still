package dev.j6n.still

import org.junit.Assert.assertEquals
import org.junit.Test

class StillScreenTest {
    @Test fun allFoundationDestinationsExist() {
        assertEquals(listOf("CAMERA", "REVIEW", "LOOKS"), StillScreen.entries.map { it.name })
    }
}
