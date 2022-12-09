package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.interfaces.{Environment, Position, TimeDistribution}
import it.unibo.model.Actuator.covertToMovement
import it.unibo.model.GlobalContext

class CollectiveAction[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    deltaMovement: Double
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  override def executeBeforeUpdateDistribution(): Unit =
    CollectiveAction.moveAll(this, deltaMovement)
}

object CollectiveAction {
  def moveAll[T, P <: Position[P]](context: GlobalContext[T, P], movement: Double): Unit = {
    import context._
    val actions = stateAndAction.map(_.action)
    agents.zip(actions).foreach { case (node, action) =>
      environment.moveNodeToPosition(
        node,
        environment.getPosition(node).plus(covertToMovement(action, environment, movement).getCoordinates)
      )
    }
  }
}
