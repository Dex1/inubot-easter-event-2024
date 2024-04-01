package nexus.api

import org.rspeer.game.script.Task
import org.rspeer.game.script.TaskDescriptor
import javax.inject.Inject

@TaskDescriptor(
    name = "End Easter Event",
    stoppable = true
)
class EndEasterEventTask @Inject constructor(
    private val easterEventManager: EasterEventManager,
    private val chatScreenManager: ChatScreenManager
) : Task() {
    override fun execute(): Boolean {
        if (easterEventManager.currentEasterStep != EasterStep.COMPLETE_QUEST) {
            return false
        }

        easterEventManager.equipAllEggOutfitPieces()

        // Teleport back to varrock
        easterEventManager.manageEggusManus(chatScreenManager) {
            it.performTeleportAction()
        }

        return !easterEventManager.isInEggTempleRegion()
    }
}