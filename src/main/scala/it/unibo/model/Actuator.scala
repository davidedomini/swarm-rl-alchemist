package it.unibo.model

import it.unibo.alchemist.model.interfaces.{Environment, Position}
import it.unibo.model.AgentAction.{East, North, NorthEast, NorthWest, South, SouthEast, SouthWest, StandStill, West}

object Actuator {
  def covertToMovement[T, P <: Position[P]](
      action: AgentAction,
      environment: Environment[T, P],
      deltaMovement: Double
  ): P =
    action match {
      case North => environment.makePosition(deltaMovement, 0)
      case South => environment.makePosition(-deltaMovement, 0)
      case East => environment.makePosition(0, deltaMovement)
      case West => environment.makePosition(0, -deltaMovement)
      case NorthEast => environment.makePosition(deltaMovement, deltaMovement)
      case NorthWest => environment.makePosition(deltaMovement, -deltaMovement)
      case SouthEast => environment.makePosition(-deltaMovement, deltaMovement)
      case SouthWest => environment.makePosition(-deltaMovement, -deltaMovement)
      case StandStill => environment.makePosition(0, 0)
    }
}
