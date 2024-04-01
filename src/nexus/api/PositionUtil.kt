package nexus.api

import org.rspeer.game.movement.Movement
import org.rspeer.game.movement.pathfinding.Collisions
import org.rspeer.game.position.Position

fun Position.walkTo() = Movement.walkTo(this)

fun Position.isReachable() = Collisions.isReachable(this)

fun Position.isNearbyAndReachable(dist: Int = 15) = distance() <= dist && isReachable()