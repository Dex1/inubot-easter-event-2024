import nexus.api.TestTask
import org.rspeer.commons.ArrayUtils
import org.rspeer.game.script.Task
import org.rspeer.game.script.TaskScript
import org.rspeer.game.script.meta.ScriptMeta

@ScriptMeta(
    developer = "varDoubleD",
    name = "Test Script",
    version = 1.0,
    desc = "Tests functionality"
)
class TestScript : TaskScript() {
    override fun tasks(): Array<Class<out Task>> = ArrayUtils.getTypeSafeArray(
        TestTask::class.java
    )
}