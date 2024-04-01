package nexus.api

import org.rspeer.game.component.Dialog

class ChatScreenManager {
    private val logger = Logger("Chat Screen Manager")

    val isOpen
        get() =
            Dialog.getOpenType(true) != null

    fun handleChatScreen(vararg options: String): Boolean {
        logger.info("Handling screens with " + "options: " + options.contentToString())

        if (Dialog.getOpenType(true) != null) {
            return Dialog.process(*options)
        }

        // Nothing to handle
        return true
    }
}
