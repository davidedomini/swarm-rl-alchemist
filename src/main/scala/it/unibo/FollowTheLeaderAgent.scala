package it.unibo

import it.unibo.AggregateComputingRLAgent.AgentResult
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.model.AgentAction.StandStill
import it.unibo.model.{AgentAction, State}
import it.unibo.scafi.space.Point3D

class FollowTheLeaderAgent
    extends AggregateProgram
    with AggregateComputingRLAgent
    with FieldUtils
    with StandardSensors
    with BlockG
    with ScafiAlchemistSupport
    with CustomSpawn {

  private lazy val leader = sense[Int]("leaderId")
  def main(): AgentResult = {
    val potentialToLeader = classicGradient(leader == mid, nbrRange)
    val nearestToLeader = includingSelf.reifyField((nbr(potentialToLeader), nbrVector())).minBy(_._2._1)._2._2
    val distances = excludingSelf
      .reifyField(nbrVector())
      .toList
      .sortBy(_._2.distance(Point3D.Zero))
      .map(_._2)
      .map(point => (point.x, point.y))
      .take(agentSpace)
    val state = State(distances, (nearestToLeader.x, nearestToLeader.y))
    if (distances.size == agentSpace && leader != mid()) {
      AgentResult(state, getPolicy(state))
    } else {
      AgentResult(state, StandStill)
    }
  }
}
