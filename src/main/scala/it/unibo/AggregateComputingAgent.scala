package it.unibo

import it.unibo.alchemist.model.implementations.reactions.CollectiveLearning
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.model.AgentAction.StandStill
import it.unibo.model.{AgentAction, State}
import it.unibo.scafi.space.Point3D

import scala.jdk.CollectionConverters.IteratorHasAsScala

class AggregateComputingAgent
    extends AggregateProgram
    with FieldUtils
    with StandardSensors
    with ScafiAlchemistSupport
    with CustomSpawn {

  def main(): (State, AgentAction) = {
    val distances = excludingSelf
      .reifyField(nbrVector())
      .toList
      .sortBy(_._2.distance(Point3D(0, 0, 0)))
      .map(_._2)
      .map(point => (point.x, point.y))
      .take(5)
    val state = State(distances)
    node.put("state", distances.size)
    if (distances.size == 5) {
      (state, retrievePolicy(state))
    } else {
      (state, AgentAction.StandStill)
    }
  }

  lazy val retrievePolicy: State => AgentAction = {
    alchemistEnvironment.getGlobalReactions
      .iterator()
      .asScala
      .collectFirst { case reaction: CollectiveLearning[_, _] => reaction }
      .map(learning => state => learning.learner.act(state))
      .getOrElse(_ => StandStill)
  }
}
