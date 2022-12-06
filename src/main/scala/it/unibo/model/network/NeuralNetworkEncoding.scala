package it.unibo.model.network

trait NeuralNetworkEncoding[A] {
  def elements: Int
  def toSeq(elem: A): Seq[Double]
}
