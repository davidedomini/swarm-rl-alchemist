package it.unibo.model

import it.unibo.model.network.NeuralNetworkEncoding

case class State(distances: List[(Double, Double)])

object State {
  val neighborhood = 5 // fixed
  implicit val encoding = new NeuralNetworkEncoding[State] {
    override def elements: Int = neighborhood * 2

    override def toSeq(elem: State): Seq[Double] = elem.distances.flatMap { case (l, r) => List(l, r) }
  }
}
