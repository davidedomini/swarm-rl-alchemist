package it.unibo.alchemist.model.implementations.reactions
import it.unibo.alchemist.model.implementations.reactions.CollectiveAction.covertToMovement
import it.unibo.alchemist.model.interfaces._
import it.unibo.model.{AgentAction, State}
import it.unibo.model.AgentAction._
class CollectiveAction[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    deltaMovement: Double
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  override def execute(): Unit = {
    val stateAndActions = managers.map(_.get[(State, AgentAction)]("it.unibo.AggregateComputingAgent"))
    val actions = stateAndActions.map(_._2)
    agents.zip(actions).foreach { case (node, action) =>
      environment.moveNodeToPosition(
        node,
        environment.getPosition(node).plus(covertToMovement(action, environment, deltaMovement).getCoordinates)
      )
    }
    distribution.update(getTimeDistribution.getNextOccurence, true, getRate, environment)
  }
}

object CollectiveAction {
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
