package com.musicprayer.vibematch.smartshuffle

import com.musicprayer.vibematch.model.Track
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

class SmartQueueGenerator(private val random: Random = Random.Default) {
    fun generate(seed: Track, library: List<Track>, limit: Int = 10): List<Track> {
        val seedVector = seed.features?.vector() ?: return fallback(seed, library, limit)
        val candidates = library.asSequence()
            .filter { it.id != seed.id && it.features != null }
            .map { it to cosine(seedVector, it.features!!.vector()) }
            .sortedByDescending { it.second }
            .take(50)
            .toMutableList()
        val selected = mutableListOf<Track>()
        repeat(minOf(limit, candidates.size)) {
            val maxScore = candidates.maxOf { it.second }
            val weights = candidates.map { exp(((it.second - maxScore) / TEMPERATURE).toDouble()) }
            var cursor = random.nextDouble() * weights.sum()
            var picked = candidates.lastIndex
            for (index in weights.indices) {
                cursor -= weights[index]
                if (cursor <= 0.0) { picked = index; break }
            }
            selected += candidates.removeAt(picked).first
        }
        return listOf(seed) + selected
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (index in a.indices) {
            dot += a[index] * b[index]
            normA += a[index] * a[index]
            normB += b[index] * b[index]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat().coerceIn(-1f, 1f)
    }

    private fun fallback(seed: Track, library: List<Track>, limit: Int) =
        listOf(seed) + library.filter { it.id != seed.id }.shuffled(random).take(limit)

    private companion object { const val TEMPERATURE = 0.4f }
}
