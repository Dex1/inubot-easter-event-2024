import nexus.api.EasterStep
import nexus.api.EndEasterEventTask
import nexus.api.Logger
import nexus.api.MasterEasterEventTask
import org.rspeer.commons.ArrayUtils
import org.rspeer.game.script.Task
import org.rspeer.game.script.TaskScript
import org.rspeer.game.script.meta.ScriptMeta
import java.util.concurrent.TimeUnit

/**
 * This script is designed to complete the 2024 Easter Event for you quickly.
 *
 * Before running the script, ensure that your inventory has enough slots to gather all the necessary items.
 * You can either start with your desired pickaxe, or the script will obtain one for you during execution.
 *
 * @author varDoubleD
 */
@ScriptMeta(
    developer = "varDoubleD",
    name = "Easter Event 2024",
    version = 1.0,
    desc = "Completes the Easter Event for you."
)
class EasterEvent2024Script : TaskScript() {
    private val logger = Logger(meta.name)
    private val startTime: Long = System.currentTimeMillis()

    override fun initialize() {
        super.initialize()

        logger.info("Welcome to the Easter Event 2024 script by varDoubleD.")
        logger.info("We have a total of ${EasterStep.values().count()} steps to complete, buckle up.")
    }

    override fun shutdown() {
        super.shutdown()

        val completeDurationMillis = System.currentTimeMillis() - startTime
        val minutes = TimeUnit.MILLISECONDS.toMinutes(completeDurationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(completeDurationMillis) - TimeUnit.MINUTES.toSeconds(minutes)
        logger.info("Script runtime: $minutes minutes $seconds seconds.")
        logger.info("Thank you for checking this script out, see you next time.")
    }

    override fun tasks(): Array<Class<out Task>> = ArrayUtils.getTypeSafeArray(
        MasterEasterEventTask::class.java,
        EndEasterEventTask::class.java
    )
}
