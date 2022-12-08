package it.unibo.alchemist.model.implementations.reactions

import it.unibo.AggregateComputingRLAgent
import it.unibo.AggregateComputingRLAgent.AgentResult
import it.unibo.alchemist.model.interfaces.{Environment, Position, Time, TimeDistribution}
import it.unibo.context._
import it.unibo.model.Actuator.covertToMovement
import it.unibo.model._
import it.unibo.util.LiveLogger

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CentralLearner[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    deltaMovement: Double,
    targetDistance: Double,
    val rewardFunction: RewardFunction
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  private var memory: List[AgentResult] = List.empty // used to store the last collective experience
  private var initialPosition: List[P] = List.empty[P] // used to restart the simulation with the same configuration
  private var updates = 0
  private val epsilon = DecayReference.exponentialDecay(0.9, 0.10).bounded(0.01)

  val learner = new DeepQLearner[State, AgentAction](
    ReplyBuffer.bounded(100000),
    AgentAction.actionSpace,
    epsilon,
    0.90,
    0.0005,
    batchSize = 64
  )
  learner.trainingMode()

  override def execute(): Unit = {
    if (environment.getSimulation.getTime.toDouble > 1) { // skip the first tick
      val stateAndActions = agents
        .map(_.getContents.values().asScala)
        .map(_.filter(_.isInstanceOf[AggregateComputingRLAgent.AgentResult]).head)
        .map(_.asInstanceOf[AggregateComputingRLAgent.AgentResult])
      val actions = stateAndActions.map(_.action)
      val states = stateAndActions.map(_.state)
      improvePolicy(states)
      memory = stateAndActions
      agents.zip(actions).foreach { case (node, action) =>
        environment.moveNodeToPosition(
          node,
          environment.getPosition(node).plus(covertToMovement(action, environment, deltaMovement).getCoordinates)
        )
      }
    }
    distribution.update(getTimeDistribution.getNextOccurence, true, getRate, environment)
  }

  override def initializationComplete(time: Time, environment: Environment[T, _]): Unit =
    initialPosition = agents.map(this.environment.getPosition)

  def improvePolicy(states: Seq[State]): Unit = {
    val evalReward = rewardFunction.compute(states, this, updates)
    val totalReward = evalReward.sum
    LiveLogger.logScalar("Reward", totalReward, updates)
    if (memory.nonEmpty) {
      updates += 1
      memory.zip(evalReward).zip(states).foreach { case ((AgentResult(state, action), reward), nextState) =>
        learner.record(state, action, reward, nextState)
      }
      learner.improve()
      if (updates % learner.updateEach == 0) {
        memory = List.empty
        agents.zip(initialPosition).foreach { case (agent, position) =>
          environment.moveNodeToPosition(agent, position)
        }
        // decay here
        epsilon.update()
        LiveLogger.logScalar("Epsilon", epsilon.value, (updates / learner.updateEach).toInt)
        learner.snapshot(updates / learner.updateEach)
      }
    }
  }
}
