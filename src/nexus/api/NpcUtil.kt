package nexus.api

import org.rspeer.game.adapter.scene.Npc

const val TALK_TO_NPC_ACTION = "Talk-to"
const val TELEPORT_NPC_ACTION = "Teleport"
const val CAPTURE_NPC_ACTION = "Capture"

fun Npc.performTalkToAction() = interact(TALK_TO_NPC_ACTION)

fun Npc.performTeleportAction() = interact(TELEPORT_NPC_ACTION)

fun Npc.performCaptureAction() = interact(CAPTURE_NPC_ACTION)

