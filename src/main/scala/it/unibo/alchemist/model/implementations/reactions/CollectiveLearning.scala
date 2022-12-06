package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.implementations.reactions.CollectiveAction.covertToMovement
import it.unibo.alchemist.model.interfaces.{Environment, Position, Time, TimeDistribution}
import it.unibo.model.{AgentAction, DecayReference, DeepQAgent, ReplyBuffer, State}
import it.unibo.context._
import it.unibo.model.network.torch._
import it.unibo.scafi.space.Point3D

class CollectiveLearning[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    deltaMovement: Double,
    targetDistance: Double
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  private var memory: List[(State, AgentAction)] = List.empty
  private var initialPosition: List[P] = List.empty[P]
  private var updates = 0
  private val epsilon = DecayReference.exponentialDecay(0.9, 0.10).bounded(0.01)
  val learner = new DeepQAgent[State, AgentAction](
    ReplyBuffer.bounded(100000),
    AgentAction.actionSpace,
    epsilon,
    0.99,
    0.0005,
    batchSize = 256
  ) // gamma 0.98, batch size = 256
  learner.trainingMode()
  override def execute(): Unit = {
    if (environment.getSimulation.getTime.toDouble > 1) {
      val stateAndActions = managers.map(_.get[(State, AgentAction)]("it.unibo.AggregateComputingAgent"))
      val actions = stateAndActions.map(_._2)
      val states = stateAndActions.map(_._1)
      val evalReward = rewardFunction(states)
      val totalReward = evalReward.sum
      writer.add_scalar("Reward", totalReward, updates)
      if (memory.nonEmpty) {
        updates += 1
        memory.zip(evalReward).zip(states).foreach { case (((state, action), reward), nextState) =>
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
          writer.add_scalar("Epsilon", epsilon.value, (updates / learner.updateEach).toInt)
          learner.snapshot(updates / learner.updateEach)
        }
      }
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

  def rewardFunction(states: Seq[State]): Seq[Double] = {
    // OLD
    /** states .map(_.distances.map { case (x, y) => Point3D(x, y, 0) }) .map(points => points.reduce(_ +
      * _).distance(Point3D.Zero) / points.size) .map(reward) def reward(distances: Double): Double = if (targetDistance
      * < distances) -(distances - targetDistance) else math.log(distances / targetDistance)
      */
    val distances = states.map(_.distances.map { case (x, y) => Point3D(x, y, 0) }.map(_.distance(Point3D.Zero)))
    val collisionReward = distances
      .map(_.min)
      .map(min =>
        if (min < targetDistance) { 2 * math.log(min / targetDistance) }
        else { 0.0 }
      )
    val cohesionReward = distances.map(_.max).map(max => if (max > targetDistance) -(max - targetDistance) else { 0.0 })
    writer.add_scalar("Reward collision", collisionReward.sum, updates)
    writer.add_scalar("Reward cohesion", cohesionReward.sum, updates)
    cohesionReward.zip(collisionReward).map { case (collision, cohesion) => collision + cohesion }

  }
}
