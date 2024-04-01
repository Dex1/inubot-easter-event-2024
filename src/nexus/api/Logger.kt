package nexus.api

import org.rspeer.commons.logging.Log

class Logger(header: String) {
    private val format = "[$header]"

    private fun formatMessage(message: Any?): String {
        return "$format $message"
    }

    fun info (message: Any?) = Log.info(formatMessage(message))

    fun debug(message: Any?) = Log.debug(formatMessage(message))

    fun warn(message: Any?) = Log.warn(formatMessage(message))
}