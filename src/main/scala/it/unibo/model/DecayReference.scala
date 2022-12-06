package it.unibo.model

trait DecayReference[V] {
  def update(): Unit = {}
  def value: V
}

object DecayReference {
  def constant[V](constant: V) = new DecayReference[V] {
    override val value = constant
  }

  def exponentialDecay(initialValue: Double, rate: Double) = new DecayReference[Double] {
    private var ticks: Int = 0
    override def update(): Unit = ticks += 1
    override def value = initialValue * math.pow(1 - rate, ticks)
  }

  implicit def flat[V](value: DecayReference[V]): V = value.value

  implicit class wrapper(ref: DecayReference[Double]) {
    def bounded(min: Double) = new DecayReference[Double] {
      override def update(): Unit = ref.update()
      override def value: Double = if (ref.value < min) min else ref.value
    }
  }
}
