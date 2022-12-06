package it.unibo.model
import scala.util.Random
import ReplyBuffer._

trait ReplyBuffer[State, Action] {
  def insert(state: State, action: Action, reward: Double, nextState: State): Unit

  def sample(batchSize: Int)(implicit random: Random): Iterable[Experience[State, Action]]

  def reset(): Unit
}

object ReplyBuffer {
  private class QueueBuffer[State, Action](maxSize: Int) extends ReplyBuffer[State, Action] {
    private var memory: List[Experience[State, Action]] = List.empty

    override def insert(state: State, action: Action, reward: Double, nextState: State): Unit =
      memory = (Experience(state, action, reward, nextState) :: memory).take(maxSize)

    override def sample(batchSize: Int)(implicit random: Random): Iterable[Experience[State, Action]] =
      random.shuffle(memory).take(batchSize)

    override def reset(): Unit = memory = List.empty
  }

  case class Experience[State, Action](state: State, action: Action, reward: Double, nextState: State)

  def bounded[State, Action](size: Int): ReplyBuffer[State, Action] = new QueueBuffer(size)
}
