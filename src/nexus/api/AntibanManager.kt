package nexus.api

import org.rspeer.commons.math.Random.nextGaussian
import org.rspeer.game.movement.Movement
import javax.inject.Singleton

@Singleton
class AntibanManager {
    private val logger: Logger = Logger("Antiban Manager")
    private var lastUpdateTime: Long = System.currentTimeMillis()

    // Minimum possible value:
    // Mean - (Standard deviation * 3)
    //  35 - (3.2 * 3) = 25.4
    //
    // Maximum possible value:
    // Mean + (Standard deviation * 3)
    //  65 + (3.2 * 3) = 74.6
    private val normalRunAtEnergyProfile = PlayerBehaviorProfile(
        "src.nexus.api.AntibanManager.normalRunAtEnergyProfile",
        DistributionParameters(50.0, 5.0),
        DistributionParameters(2.0, 0.4)
    )

    private fun getNewRunAtEnergy() = normal(normalRunAtEnergyProfile.variable1, normalRunAtEnergyProfile.variable2)
        .coerceIn(30.0, 75.0)

    private var _runAtEnergy = getNewRunAtEnergy()

    val runAtEnergy get() = _runAtEnergy

    fun shouldToggleOnRun() = !Movement.isRunEnabled() && Movement.getRunEnergy() >= _runAtEnergy

    fun checkAndUpdateProps() {
        if (!shouldUpdateProps()) return
        updateProps()
    }

    private fun shouldUpdateProps() = System.currentTimeMillis() - lastUpdateTime >= 300000

    private fun updateProps() {
        _runAtEnergy = getNewRunAtEnergy()
        lastUpdateTime = System.currentTimeMillis()
        logger.info("Updated props")
        logger.info("New run at energy: $_runAtEnergy")
        logger.info("Last update time: $lastUpdateTime")
    }

    companion object {
        fun normal(
            mean: Double,
            stdDev: Double,
            min: Double? = null,
            max: Double? = null
        ): Double {
            require(min == null || max == null || min <= max) {
                "Invalid range constraints: min must be less than or equal to max."
            }

            var value: Double
            do {
                value = nextGaussian() * stdDev + mean
            } while ((min != null && value < min) || (max != null && value > max))
            return value
        }
    }
}