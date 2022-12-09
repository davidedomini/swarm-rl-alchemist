package it.unibo.model

import scala.util.Random

sealed trait AgentAction
object AgentAction {
  final case object North extends AgentAction
  final case object South extends AgentAction
  final case object East extends AgentAction
  final case object West extends AgentAction
  final case object NorthEast extends AgentAction
  final case object NorthWest extends AgentAction
  final case object SouthWest extends AgentAction
  final case object SouthEast extends AgentAction
  final case object StandStill extends AgentAction

  val actionSpace: Seq[AgentAction] =
    Seq(North, South, East, West, NorthEast, NorthWest, SouthEast, SouthWest, StandStill)

  val actionSpaceWithoutStandStill: Seq[AgentAction] =
    actionSpace.filter(_ != StandStill)

  def sample(implicit random: Random): AgentAction = random.shuffle(actionSpace).head
}
