package it.unibo.model

import it.unibo.model.Agent.Training
import it.unibo.model.network.torch._
import it.unibo.model.network.{DQN, NeuralNetworkEncoding}
import it.unibo.util.LiveLogger
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.{PyQuote, SeqConverters}

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Random

class DeepQLearner[State, Action](
    memory: ReplyBuffer[State, Action],
    actionSpace: Seq[Action],
    var epsilon: DecayReference[Double],
    gamma: Double,
    learningRate: Double,
    batchSize: Int = 32,
    val updateEach: Int = 100,
    val hiddenSize: Int = 32
)(implicit encoding: NeuralNetworkEncoding[State], random: Random)
    extends Agent[State, Action] {
  private var updates = 0
  private val targetNetwork = DQN(encoding.elements, hiddenSize, actionSpace.size)
  private val policyNetwork = DQN(encoding.elements, hiddenSize, actionSpace.size)
  private val targetPolicy = DeepQLearner.policyFromNetwork(policyNetwork, encoding, actionSpace)
  private val behaviouralPolicy = DeepQLearner.policyFromNetwork(policyNetwork, encoding, actionSpace)
  private val optimizer = optim.RMSprop(policyNetwork.parameters(), learningRate)

  val optimal: State => Action = targetPolicy

  val behavioural: State => Action = state =>
    if (random.nextDouble() < epsilon) {
      random.shuffle(actionSpace).head
    } else behaviouralPolicy(state)

  override def record(state: State, action: Action, reward: Double, nextState: State): Unit =
    memory.insert(state, action, reward, nextState)

  override def improve(): Unit = if (this.mode == Training) {
    val memorySample = memory.sample(batchSize)
    if (memory.sample(batchSize).size == batchSize) {
      val states = memorySample.map(_.state).toSeq.map(state => encoding.toSeq(state).toPythonCopy).toPythonCopy
      val action = memorySample.map(_.action).toSeq.map(action => actionSpace.indexOf(action)).toPythonCopy
      val rewards = torch.tensor(memorySample.map(_.reward).toSeq.toPythonCopy)
      val nextState = memorySample.map(_.nextState).toSeq.map(state => encoding.toSeq(state).toPythonCopy).toPythonCopy
      val stateActionValue = policyNetwork(torch.tensor(states)).gather(1, torch.tensor(action).view(batchSize, 1))
      val nextStateValues = targetNetwork(torch.tensor(nextState)).max(1).bracketAccess(0).detach()
      val expectedValue = (nextStateValues * gamma) + rewards
      val criterion = nn.SmoothL1Loss()
      val loss = criterion(stateActionValue, expectedValue.unsqueeze(1))
      LiveLogger.logScalar("Loss", loss.item().as[Double], updates)
      optimizer.zero_grad()
      loss.backward()
      py"[param.grad.data.clamp_(-1, 1) for param in ${policyNetwork.parameters()}]"
      optimizer.step()
      updates += 1
      if (updates % updateEach == 0) {
        targetNetwork.load_state_dict(policyNetwork.state_dict())
      }
    }
  }

  def snapshot(episode: Int): Unit = {
    val timeMark = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date)
    torch.save(targetNetwork.state_dict(), s"data/network-$episode-$timeMark")
  }
}

object DeepQLearner {
  def policyFromNetworkSnapshot[S, A](
      path: String,
      hiddenSize: Int,
      encoding: NeuralNetworkEncoding[S],
      actionSpace: Seq[A]
  ): S => A = {
    val model = DQN(encoding.elements, hiddenSize, actionSpace.size)
    model.load_state_dict(torch.load(path))
    policyFromNetwork(model, encoding, actionSpace)
  }

  def policyFromNetwork[S, A](network: py.Dynamic, encoding: NeuralNetworkEncoding[S], actionSpace: Seq[A]): S => A = {
    state =>
      val netInput = encoding.toSeq(state)
      py.`with`(torch.no_grad()) { _ =>
        val tensor = torch.tensor(netInput.toPythonCopy).view(1, encoding.elements)
        val actionIndex = network(tensor).max(1).bracketAccess(1).item().as[Int]
        actionSpace(actionIndex)
      }
  }
}
