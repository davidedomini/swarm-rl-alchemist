package it.unibo

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.Point3D
import it.unibo.CCActions._
import it.unibo.model.DeepQLearner.policyFromNetworkSnapshot
import it.unibo.model.network.NeuralNetworkEncoding

class CCAgent
  extends AggregateProgram
    with FieldUtils
    with StandardSensors
    with BlockG
    with ScafiAlchemistSupport
    with CustomSpawn {

  private val encoding = new NeuralNetworkEncoding[CCState] {
    override def elements: Int = 10

    override def toSeq(elem: CCState): Seq[Double] = {
      val fill = List.fill(elements)(0.0)
      (elem.positions.flatMap { case (l, r) => List(l, r) } ++ fill).take(elements)
    }
  }

  override def main(): Unit = {
    val policyPath = node.get("policyPath").toString
    val policy = policyFromNetworkSnapshot(policyPath, 32, encoding, CCActions.toSeq())
    val distances = excludingSelf
      .reifyField(nbrVector())
      .toList
      .sortBy(_._2.distance(Point3D.Zero))
      .map(_._2)
      .map(point => (point.x, point.y))
      .take(5)
    val state = CCState(distances, mid())
    val action = policy(state)
    makeAction(action)
  }

  private def makeAction(action: Action): Unit = {
    val dt = 0.01
    val agent = alchemistEnvironment.getNodeByID(mid())
    action match {
      case NoAction => // do nothing
      case North => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(0.0, dt)))
      case South => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(0.0, -dt)))
      case West => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(-dt, 0.0)))
      case East => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(dt, 0.0)))
      case NorthEast => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(dt, dt)))
      case SouthEast => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(dt, -dt)))
      case NorthWest => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(-dt, dt)))
      case SouthWest => alchemistEnvironment.moveNodeToPosition(agent, alchemistEnvironment.getPosition(agent).plus(Array(-dt, -dt)))
    }
  }

}