package it.unibo

import scala.util.Random

package object context {
  implicit val random: Random = new Random(42)
}
