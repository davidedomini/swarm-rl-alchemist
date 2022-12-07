package it.unibo.util

import it.unibo.model.network.torch.log

object LiveLogger {
  private val writer = log.SummaryWriter()
  def logScalar(tag: String, value: Double, tick: Int): Unit = writer.add_scalar(tag, value, tick)
}
