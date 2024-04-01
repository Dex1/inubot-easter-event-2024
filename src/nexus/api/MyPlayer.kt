package nexus.api

import org.rspeer.game.Game
import org.rspeer.game.Vars
import org.rspeer.game.component.Interfaces
import org.rspeer.game.position.Position
import org.rspeer.game.scene.Players

fun MyPlayer.withinRegions(vararg regions: Int) = position.regionId in regions

object MyPlayer {
    private const val RUN_VARP_ID = 173

    val position: Position
        get() =
            Game.getClient().let { client ->
                val player = client.player
                val absX = player.absoluteX
                val absY = player.absoluteY
                val level = client.floorLevel
                Position.fromAbsolute(absX, absY, level)
            }

    val index: Int
        get() =
            Game.getClient().playerIndex

    val animation: Int
        get() =
            Game.getClient().player.animation

    val username: String
        get() =
            Game.getClient().username

    val runEnergy: Int
        get() =
            Game.getClient().energy.let {
                it / 100
            }

    val isAnimating: Boolean
        get() =
            animation != -1

    val isRunEnabled: Boolean
        get() =
            Vars.get(Vars.Type.VARP, RUN_VARP_ID) == 1

    val isRunDisabled: Boolean
        get() =
            Vars.get(Vars.Type.VARP, RUN_VARP_ID) == 0

    val isMoving: Boolean
        get() =
            Players.query()
                .filter { it.isMoving && it.index == index }
                .results()
                .isNotEmpty()
}