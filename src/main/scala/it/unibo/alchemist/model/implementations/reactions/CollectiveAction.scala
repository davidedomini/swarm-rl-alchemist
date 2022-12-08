package it.unibo.alchemist.model.implementations.reactions

import it.unibo.AggregateComputingRLAgent
import it.unibo.alchemist.model.interfaces.{Environment, Position, TimeDistribution}
import it.unibo.model.Actuator.covertToMovement

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CollectiveAction[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    deltaMovement: Double
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  override def execute(): Unit = {
    // TODO refactor this
    val stateAndActions = agents
      .map(_.getContents.values().asScala)
      .map(_.filter(_.isInstanceOf[AggregateComputingRLAgent.AgentResult]).head)
      .map(_.asInstanceOf[AggregateComputingRLAgent.AgentResult])
    val actions = stateAndActions.map(_.action)
    agents.zip(actions).foreach { case (node, action) =>
      environment.moveNodeToPosition(
        node,
        environment.getPosition(node).plus(covertToMovement(action, environment, deltaMovement).getCoordinates)
      )
    }

    distribution.update(getTimeDistribution.getNextOccurence, true, getRate, environment)
  }
}
