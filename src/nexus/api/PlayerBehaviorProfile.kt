package nexus.api

import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

// Based on the values of variable1 and variable2 for each player,
// the lambda might return true always, never, or sometimes.

// If the threshold is greater than 1 (e.g., variable1 is 60 and variable2 is 40),
// the lambda will always return true, and the player will always do the specific action.

// If the threshold is equal to 0 (e.g., variable1 is 0),
// the lambda will never return true, and the player will never do the specific action.

// If the threshold is between 0 and 1,
// the probability of returning true depends on the ratio of variable1 to variable2.
// The closer the threshold is to 1, the higher the probability that it will be triggered;
// the closer the threshold is to 0, the lower the probability.
fun getVariableBasedTriggerCondition(): (PlayerBehaviorProfile) -> Boolean {
    return { player: PlayerBehaviorProfile ->
        player.variable2 != 0.0 && java.util.Random().nextDouble() < player.variable1 / player.variable2
    }
}

data class DistributionParameters(val mean: Double, val standardDeviation: Double)

// This approach provides a higher level of encryption
// for the relationship between the username and the random seed,
// making it even more difficult to reverse.
data class PlayerBehaviorProfile(
    private val uniqueKey: String, // Unique key based on package name and field
    private val variable1Parameters: DistributionParameters,
    private val variable2Parameters: DistributionParameters
) {
    private val username: String = MyPlayer.username

    val variable1: Double
    val variable2: Double
    val randomSeed: Long

    init {
        val combinedKey = "$uniqueKey:$username"

        val randomSeed1 = computeSha256Hash(combinedKey)
        val randomSeed2 = nextSeed(randomSeed1)

        val random1 = Random(randomSeed1)
        val random2 = Random(randomSeed2)

        randomSeed = randomSeed1

        variable1 = abs(generateNormalVariable(random1, variable1Parameters.mean, variable1Parameters.standardDeviation))
        variable2 = abs(generateNormalVariable(random2, variable2Parameters.mean, variable2Parameters.standardDeviation))
    }

    private fun computeSha256Hash(text: String): Long {
        val bytes = text.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)

        return hashedBytes.fold(0L) { acc, byte -> (acc shl 8) or byte.toLong() }
    }

    private fun generateNormalVariable(
        random: Random,
        mean: Double,
        stdDev: Double
    ): Double {
        val u1 = random.nextDouble()
        val u2 = random.nextDouble()

        val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
        return mean + (z0 * stdDev)
    }

    private fun nextSeed(seed: Long): Long {
        val multiplier = 0x5DEECE66DL
        val addend = 0xBL
        val mask = (1L shl 48) - 1

        return ((seed * multiplier + addend) and mask) xor seed
    }
}
