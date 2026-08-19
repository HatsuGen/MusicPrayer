package com.musicprayer.vibematch.playback

import com.musicprayer.vibematch.model.Track
import kotlin.random.Random

/**
 * Supplies a shuffled, non-repeating set of tracks for one solo-play session.
 *
 * The seed is considered used immediately. Tracks reserved by another queue action are
 * skipped as well, even when they were already waiting in the shuffled pool.
 */
class SoloRandomQueue(
    seed: Track,
    library: List<Track>,
    random: Random = Random.Default,
) {
    private val pool = NonRepeatingRandomPool(seed.id, library, Track::id, random)

    fun take(count: Int): List<Track> = pool.take(count)

    fun reserve(trackId: Long) = pool.reserve(trackId)
}

internal class NonRepeatingRandomPool<T>(
    seedId: Long,
    items: List<T>,
    private val idOf: (T) -> Long,
    random: Random = Random.Default,
) {
    private val unavailableIds = mutableSetOf(seedId)
    private val remaining = ArrayDeque(
        items
            .distinctBy(idOf)
            .filterNot { idOf(it) == seedId }
            .shuffled(random),
    )

    fun take(count: Int): List<T> {
        require(count >= 0) { "count must not be negative" }

        return buildList {
            while (size < count && remaining.isNotEmpty()) {
                val candidate = remaining.removeFirst()
                if (unavailableIds.add(idOf(candidate))) add(candidate)
            }
        }
    }

    fun reserve(trackId: Long) {
        unavailableIds += trackId
    }
}
