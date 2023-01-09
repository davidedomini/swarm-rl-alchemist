package it.unibo.util

import it.unibo.model.network.torch.log
import me.shadaj.scalapy.py

object LiveLogger {
  private val writer = log.SummaryWriter()
  def logScalar(tag: String, value: Double, tick: Int): Unit = writer.add_scalar(tag, value, tick)
  def logAny(tag: String, value: py.Dynamic, tick: Int): Unit = writer.add_scalar(tag, value, tick)

}
