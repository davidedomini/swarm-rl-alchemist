package it.unibo

object CCActions {
  final case object North extends Action
  final case object South extends Action
  final case object East extends Action
  final case object West extends Action
  final case object NorthEast extends Action
  final case object NorthWest extends Action
  final case object SouthWest extends Action
  final case object SouthEast extends Action

  final case object NoAction extends Action

  def toSeq(): Seq[Action] = Seq(North, South, East, West, NorthEast, NorthWest, SouthEast, SouthWest)
}
