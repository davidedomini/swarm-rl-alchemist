package it.unibo

import it.unibo.alchemist.model.implementations.reactions.CollectiveLearning
import it.unibo.alchemist.model.interfaces.Position
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.Point3D

import scala.jdk.CollectionConverters.IteratorHasAsScala

class Main extends AggregateProgram with StandardSensors with BlockG with ScafiAlchemistSupport {
  override def main: Any = {
    val position = alchemistEnvironment.getPosition(alchemistEnvironment.getNodeByID(mid()))
    val center =
      G[Point3D](mid() == 20, Point3D.Zero, point => point + Point3D(nbrVector().x, nbrVector().y, 0.0), nbrRange)
    val module = math.hypot(center.x, center.y)
    if (math.hypot(center.x, center.y) < 0.001) {
      alchemistEnvironment.makePosition(center.x / module, center.y / module).plus(position.getCoordinates)
    } else {
      alchemistEnvironment.makePosition(center.x / module, center.y / module).plus(position.getCoordinates)
    }
    alchemistEnvironment.getGlobalReactions
      .iterator()
      .asScala
      .filter(element => isInstanceOf[CollectiveLearning[_, _]])
  }

}
