package nexus.api

import org.rspeer.game.adapter.type.SceneNode
import org.rspeer.game.movement.Movement
import org.rspeer.game.movement.pathfinding.Collisions

fun SceneNode.walkTo() = Movement.walkTo(this)

fun SceneNode.canReach() = Collisions.canReach(this)