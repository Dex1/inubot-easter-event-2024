package nexus.api

import org.rspeer.game.position.area.Area

fun Area.containsMyPlayer() = MyPlayer.position in tiles