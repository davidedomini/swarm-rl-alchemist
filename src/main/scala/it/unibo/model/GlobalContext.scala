package it.unibo.model

import it.unibo.AggregateComputingRLAgent
import it.unibo.AggregateComputingRLAgent.AgentResult
import it.unibo.alchemist.model.implementations.nodes.NodeManager
import it.unibo.alchemist.model.interfaces.{Environment, Node, Position}

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait GlobalContext[T, P <: Position[P]] {
  def agents: List[Node[T]]

  def managers: List[NodeManager]

  def environment: Environment[T, P]

  def stateAndAction: (Seq[AgentResult]) = {
    agents
      .map(_.getContents.values().asScala)
      .map(_.filter(_.isInstanceOf[AggregateComputingRLAgent.AgentResult]).head)
      .map(_.asInstanceOf[AggregateComputingRLAgent.AgentResult])

  }
}
