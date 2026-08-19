package com.musicprayer.vibematch.smartshuffle

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class SmartQueueGeneratorTest {
    private val generator = SmartQueueGenerator(Random(7))

    @Test fun identicalVectorsHavePerfectSimilarity() {
        assertEquals(1f, generator.cosine(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f)), 0.0001f)
    }
}
