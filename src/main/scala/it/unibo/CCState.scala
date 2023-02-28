package it.unibo

case class CCState(positions: List[(Double, Double)], agentPosition: (Double, Double), agentId: Int) {
  def elements(): Int = 3 * 2

  def toSeq(): Seq[Double] = {
    val fill = List.fill(elements())(0.0)
    (positions.flatMap { case (l, r) => List(l, r) } ++ fill).take(elements())
  }

  def isEmpty(): Boolean = false
}
