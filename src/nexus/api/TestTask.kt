package nexus.api

import org.rspeer.game.script.Task
import org.rspeer.game.script.TaskDescriptor

@TaskDescriptor(name = "Testing Task")
class TestTask : Task() {
    override fun execute(): Boolean {
        return false
    }
}