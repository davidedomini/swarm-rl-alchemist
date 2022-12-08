package it.unibo

import it.unibo.alchemist.model.implementations.reactions.CentralLearner
import it.unibo.alchemist.model.interfaces.Action
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.model.AgentAction.StandStill
import it.unibo.model.network.NeuralNetworkEncoding
import it.unibo.model.{AgentAction, DeepQLearner, State}

import scala.jdk.CollectionConverters.IteratorHasAsScala

trait AggregateComputingRLAgent {
  self: AggregateProgram with ScafiAlchemistSupport =>

  def dropStandStill: Boolean = false

  private def actionSpace: Seq[AgentAction] = if (dropStandStill) {
    AgentAction.actionSpace.filter(_ == AgentAction.StandStill)
  } else AgentAction.actionSpace

  protected lazy val agentSpace = 5

  lazy val getPolicy: State => AgentAction = {
    node
      .getOption[String]("policyPath")
      .map(path =>
        DeepQLearner
          .policyFromNetworkSnapshot(path, 32, implicitly[NeuralNetworkEncoding[State]], AgentAction.actionSpace)
      )
      .getOrElse {
        alchemistEnvironment.getGlobalReactions
          .iterator()
          .asScala
          .collectFirst { case reaction: CentralLearner[_, _] => reaction }
          .map(learning => state => learning.learner.act(state))
          .getOrElse(_ => StandStill)
      }
  }
}

object AggregateComputingRLAgent {
  case class AgentResult(state: State, action: AgentAction)
}
