package com.musicprayer.vibematch.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class SoloRandomQueueTest {
    @Test
    fun oneTrackLibraryHasNoUpcomingTracks() {
        val queue = pool(1, seed = 1, randomSeed = 1)

        assertEquals(emptyList<Long>(), queue.take(20))
        assertEquals(emptyList<Long>(), queue.take(20))
    }

    @Test
    fun twentyOneTrackLibraryReturnsExactlyTwentyUpcomingTracks() {
        val queue = pool(21, seed = 1, randomSeed = 2)

        val upcoming = queue.take(20)

        assertEquals(20, upcoming.size)
        assertEquals(20, upcoming.toSet().size)
        assertFalse(1L in upcoming)
        assertEquals(emptyList<Long>(), queue.take(1))
    }

    @Test
    fun repeatedRefillsNeverReturnAnIdTwice() {
        val queue = pool(100, seed = 1, randomSeed = 3)
        val returned = buildList {
            repeat(5) { addAll(queue.take(20)) }
        }

        assertEquals(99, returned.size)
        assertEquals(99, returned.toSet().size)
        assertFalse(1L in returned)
        assertEquals(emptyList<Long>(), queue.take(20))
    }

    @Test
    fun theSameRandomSeedProducesTheSameOrder() {
        val first = pool(100, seed = 1, randomSeed = 42)
        val second = pool(100, seed = 1, randomSeed = 42)

        val firstOrder = generateSequence { first.take(13).takeIf(List<Long>::isNotEmpty) }.flatten().toList()
        val secondOrder = generateSequence { second.take(13).takeIf(List<Long>::isNotEmpty) }.flatten().toList()

        assertEquals(firstOrder, secondOrder)
    }

    @Test
    fun reservedTrackIsNeverReturned() {
        val queue = pool(100, seed = 1, randomSeed = 4)

        queue.reserve(57L)
        val returned = generateSequence { queue.take(20).takeIf(List<Long>::isNotEmpty) }.flatten().toList()

        assertEquals(98, returned.size)
        assertFalse(57L in returned)
    }

    private fun pool(size: Int, seed: Long, randomSeed: Int) = NonRepeatingRandomPool(
        seedId = seed,
        items = (1L..size.toLong()).toList(),
        idOf = { it },
        random = Random(randomSeed),
    )
}
