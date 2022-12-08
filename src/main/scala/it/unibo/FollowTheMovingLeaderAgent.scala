package it.unibo

import it.unibo.AggregateComputingRLAgent.AgentResult
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.model.AgentAction.{East, North, NorthWest, South, SouthEast, StandStill, West}
import it.unibo.model.{AgentAction, State}
import it.unibo.scafi.space.Point3D

import scala.collection.immutable.Queue

class FollowTheMovingLeaderAgent
    extends AggregateProgram
    with AggregateComputingRLAgent
    with FieldUtils
    with StandardSensors
    with BlockG
    with BlockS
    with ScafiAlchemistSupport
    with CustomSpawn {

  private val movesWithTheSameDirection = 100
  private val waitFor = 3
  private var leaderMoves = Queue(North, East, SouthEast, South, West)
    .flatMap(List.fill(movesWithTheSameDirection)(_))
    .flatMap(_ :: List.fill(waitFor)(StandStill))
  private lazy val leader = sense[Int]("leaderId") == mid()
  def main(): AgentResult = {
    node.put("isLeader", leader)
    val potentialToLeader = classicGradient(leader, nbrRange)
    val nearestToLeader = includingSelf.reifyField((nbr(potentialToLeader), nbrVector())).minBy(_._2._1)._2._2
    val distances = excludingSelf
      .reifyField(nbrVector())
      .toList
      .sortBy(_._2.distance(Point3D.Zero))
      .map(_._2)
      .map(point => (point.x, point.y))
      .take(agentSpace)
    val state = State(distances, (nearestToLeader.x, nearestToLeader.y))
    if (distances.size == agentSpace && !leader) {
      AgentResult(state, getPolicy(state))
    } else {
      val currentMove = leaderMoves.head
      leaderMoves = leaderMoves.tail :+ currentMove
      AgentResult(state, currentMove)
    }
  }
}
