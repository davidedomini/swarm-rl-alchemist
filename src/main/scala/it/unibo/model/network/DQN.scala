package it.unibo.model.network

import it.unibo.model.network.torch._
import me.shadaj.scalapy.py

object DQN {
  def apply(input: Int, hidden: Int, output: Int): py.Dynamic =
    nn.Sequential(
      nn.Linear(input, hidden),
      nn.ReLU(),
      nn.Linear(hidden, hidden),
      nn.ReLU(),
      nn.Linear(hidden, output)
    )
}
