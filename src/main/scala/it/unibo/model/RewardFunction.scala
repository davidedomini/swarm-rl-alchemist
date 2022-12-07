package it.unibo.model

import it.unibo.alchemist.model.implementations.reactions.GlobalContext
import it.unibo.scafi.space.Point3D
import it.unibo.util.LiveLogger
import CohesionAndCollision._
import it.unibo.alchemist.model.interfaces.Position

trait RewardFunction {
  def compute[T, P <: Position[P]](state: Seq[State], context: GlobalContext[T, P], ticks: Int): Seq[Double]
}

class CohesionAndCollision(targetDistance: Double) extends RewardFunction {
  override def compute[T, P <: Position[P]](
      states: Seq[State],
      context: GlobalContext[T, P],
      ticks: Int
  ): Seq[Double] = {
    val distances = computeDistancesFromNeighborhood(states)
    val collisionReward = collisionFactor(distances, targetDistance)
    val cohesionReward = cohesionFactor(distances, targetDistance)
    LiveLogger.logScalar("Reward collision", collisionReward.sum, ticks)
    LiveLogger.logScalar("Reward cohesion", cohesionReward.sum, ticks)
    cohesionReward.zip(collisionReward).map { case (collision, cohesion) => collision + cohesion }
  }
}

object CohesionAndCollision {
  def computeDistancesFromNeighborhood(states: Seq[State]): Seq[Seq[Double]] =
    states.map(_.distances.map { case (x, y) => Point3D(x, y, 0) }.map(_.distance(Point3D.Zero)))
  def collisionFactor(distances: Seq[Seq[Double]], target: Double): Seq[Double] = distances
    .map(_.min)
    .map(min =>
      if (min < target) { 2 * math.log(min / target) }
      else { 0.0 }
    )

  def cohesionFactor(distances: Seq[Seq[Double]], target: Double): Seq[Double] = distances
    .map(_.max)
    .map(max => if (max > target) -(max - target) else { 0.0 })
}

class FollowTheLeader(referenceId: Int, targetDistance: Double) extends RewardFunction {
  override def compute[T, P <: Position[P]](
      states: Seq[State],
      context: GlobalContext[T, P],
      ticks: Int
  ): Seq[Double] = {
    import context._
    val center = agents.filter(_.getId == referenceId).head
    val rewardLeaderDistance = agents.map(
      -environment.getDistanceBetweenNodes(_, center)
    ) // .map(distance => if(distances < targetDistance) { 0.0 } else { targetDistance })
    val distances = computeDistancesFromNeighborhood(states)
    val collisionReward = collisionFactor(distances, targetDistance)
    LiveLogger.logScalar("Reward collision", collisionReward.sum, ticks)
    LiveLogger.logScalar("Reward Distance Leader", rewardLeaderDistance.sum, ticks)
    rewardLeaderDistance.zip(collisionReward).map { case (distanceReward, collisionReward) =>
      distanceReward + collisionReward
    }
  }
}
