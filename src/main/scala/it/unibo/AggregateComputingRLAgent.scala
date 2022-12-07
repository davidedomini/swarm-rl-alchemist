package it.unibo

import it.unibo.alchemist.model.implementations.reactions.CentralServer
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.model.AgentAction.StandStill
import it.unibo.model.{AgentAction, State}

import scala.jdk.CollectionConverters.IteratorHasAsScala

trait AggregateComputingRLAgent {
  self: AggregateProgram with ScafiAlchemistSupport =>

  protected lazy val agentSpace = 5

  lazy val getPolicy: State => AgentAction = {
    alchemistEnvironment.getGlobalReactions
      .iterator()
      .asScala
      .collectFirst { case reaction: CentralServer[_, _] => reaction }
      .map(learning => state => learning.learner.act(state))
      .getOrElse(_ => StandStill)
  }
}

object AggregateComputingRLAgent {
  case class AgentResult(state: State, action: AgentAction)
}
