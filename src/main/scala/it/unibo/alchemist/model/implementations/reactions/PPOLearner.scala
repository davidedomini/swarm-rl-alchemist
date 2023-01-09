package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.implementations.reactions.PPOLearner.{critic, np}
import it.unibo.alchemist.model.interfaces.{Environment, Position, Time, TimeDistribution}
import it.unibo.model.{RewardFunction, State}
import it.unibo.model.network.torch.torch
import it.unibo.util.LiveLogger
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import me.shadaj.scalapy.py.PyQuote

import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.immutable.Queue
import scala.jdk.CollectionConverters.IteratorHasAsScala

class PPOLearner[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    val targetDistance: Double,
    val rewardFunction: RewardFunction,
    val input: Int,
    val updatePeriod: Int
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  private val gamma = 0.99
  private val tau = 0.95
  private val learningRate = 0.0004
  private var ticks = 1
  private val minibatch = 32
  private val clipParameter = 0.01
  private val ppoEpochs = 16
  private var episodeTicks = 1
  private var globalStateBuffer: Queue[py.Dynamic] = Queue.empty
  private var localObservationBuffer: Queue[py.Dynamic] = Queue.empty
  private var rewardBuffer: Queue[py.Dynamic] = Queue.empty
  private var valueBuffer: Queue[py.Dynamic] = Queue.empty
  private var actionBuffer: Queue[py.Dynamic] = Queue.empty
  private var actionLog: Queue[py.Dynamic] = Queue.empty
  private var initialPosition: List[P] = List.empty[P] // used to restart the simulation with the same configuration
  private val actorNetwork = PPOLearner.actor(input, 2) // movement
  private var criticNetwork: py.Dynamic = null

  private var optimizer: py.Dynamic = null

  override def initializationComplete(time: Time, environment: Environment[T, _]): Unit = {
    initialPosition = agents.map(this.environment.getPosition)
    criticNetwork =
      PPOLearner.critic(2 * initialPosition.size) // the global critic has as input the position of the entire system
    optimizer =
      torch.optim.Adam(py"list($actorNetwork.parameters()) + list($criticNetwork.parameters())", lr = learningRate)
  }

  override protected def executeBeforeUpdateDistribution(): Unit = {
    actorNetwork.eval()
    criticNetwork.eval()
    if (episodeTicks % updatePeriod == 0) {
      // training
      episodeTicks = 0
      var gae: py.Dynamic = py.eval("0")
      var returns: List[py.Dynamic] = List.empty
      rewardBuffer = rewardBuffer.drop(1)
      rewardBuffer.zipWithIndex.reverse.foreach { case (reward, index) =>
        val delta = py"$reward + $gamma * ${valueBuffer(index + 1)} - ${valueBuffer(index)}"
        gae = py"$delta + $gamma * $tau * $gae"
        returns = (gae + valueBuffer(index)) :: returns
      }
      returns = returns.map(_.detach().tolist())
      val returnsTensor = torch.tensor(returns.toPythonCopy)
      val returnNormalised =
        (returnsTensor - returnsTensor.mean()) / (returnsTensor.std() + 1e-10) // .clamp(-1, 1)
      returns = returnNormalised.tolist().as[List[Seq[Double]]].map { seq =>
        torch.tensor(seq.toPythonCopy)
      }
      actionLog = actionLog.map(_.detach())
      valueBuffer = valueBuffer.map(_.detach())
      var advantages: Seq[py.Dynamic] = returns.zip(valueBuffer).map { case (ret, value) => ret - value }
      /*val advantagesTensor = torch.tensor(advantages.toPythonCopy)
      val advantageNormalized =
        ((advantagesTensor - advantagesTensor.mean()) / (advantagesTensor.std() + 1e-10)).clamp(-1, 1)
      advantages = advantageNormalized.tolist().as[List[Seq[Double]]].map { seq =>
        torch.tensor(seq.toPythonCopy)
      }*/
      criticNetwork.train()
      actorNetwork.train()
      for (epoch <- 1 to this.ppoEpochs) {
        print(s"Epoch: $epoch ")
        var totalLoss = py"0"
        val randomPicking = np.random.randint(0, globalStateBuffer.size - 1, minibatch).tolist().as[Seq[Int]]
        for (pick <- randomPicking) {
          val state = globalStateBuffer(pick)
          val obs = localObservationBuffer(pick)
          val action = actionBuffer(pick)
          val oldProbability = actionLog(pick)
          val advantage = advantages(pick)
          val ret = returns(pick).detach()
          val criticValue = criticNetwork(state)
          val actorDistribution = actorNetwork(obs)
          val entropy = actorDistribution.entropy()
          val newLogAction = actorDistribution.log_prob(action)
          val ratio = (newLogAction - oldProbability).exp()
          val surrogate1 = ratio * advantage
          val surrogate2 = torch.clamp(ratio, 1.0 - clipParameter, 1.0 + clipParameter) * advantage
          val actorLoss = -torch.min(surrogate1, surrogate2).mean()
          val criticLoss = (ret - criticValue).pow(2).mean()
          val loss = py"0.5 * $criticLoss + $actorLoss - 0.01 * ${entropy.mean()}" // .sum() // .mean()
          optimizer.zero_grad()
          loss.backward()
          optimizer.step()
          totalLoss = loss.detach() + totalLoss
        }
        LiveLogger.logAny("Loss", totalLoss, ticks)
        println(totalLoss)
      }
      globalStateBuffer = Queue.empty
      localObservationBuffer = Queue.empty
      rewardBuffer = Queue.empty
      valueBuffer = Queue.empty
      actionBuffer = Queue.empty
      actionLog = Queue.empty
      agents.zip(initialPosition).foreach { case (agent, position) =>
        environment.moveNodeToPosition(agent, position)
      }
      val timeMark = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date)
      torch.save(actorNetwork.state_dict(), s"data/actor-$ticks-$timeMark")
      torch.save(criticNetwork.state_dict(), s"data/critic-$ticks-$timeMark")
    } else {
      val inputCritic = agents
        .map(node => environment.getPosition(node))
        .flatMap(position => List(position.getCoordinate(0), position.getCoordinate(1)))
      val inputCriticTensor = torch.tensor(inputCritic.toPythonCopy)
      val value = criticNetwork(inputCriticTensor)
      val observations = agents
        .map(node => (node, environment.getNeighborhood(node).iterator().asScala.toList))
        .map { case (agent, neigh) =>
          agent -> neigh.map(node => environment.getPosition(node).minus(environment.getPosition(agent).getCoordinates))
        }
        .map { case (agent, neigh) => neigh.sortBy(position => environment.getPosition(agent).distanceTo(position)) }
      val inputObservations = observations
        .map { case neigh =>
          neigh.flatMap(position => Seq(position.getCoordinate(0), position.getCoordinate(1))).take(input).toPythonCopy
        }
      // .map(observation => torch.tensor(observation))
      val observationTensor = torch.tensor(inputObservations.toPythonCopy)
      val distributions = actorNetwork(observationTensor)
      val action = distributions.sample().clamp(-1, 1)
      val actionsList = action.detach().tolist().as[Seq[Seq[Double]]]
      agents.zip(actionsList).foreach { case (node, action) =>
        val nodePosition = environment.getPosition(node)
        environment.moveNodeToPosition(node, nodePosition.plus(action.toArray))
      }
      val logProbability = distributions.log_prob(action)
      val statesForReward = observations.map { case (neigh) =>
        State(neigh.map(position => (position.getCoordinate(0), position.getCoordinate(1))), (0, 0))
      }
      val rewards = torch.tensor(rewardFunction.compute(statesForReward, this, ticks).toPythonCopy)
      globalStateBuffer = globalStateBuffer :+ inputCriticTensor
      valueBuffer = valueBuffer :+ value
      localObservationBuffer = localObservationBuffer :+ observationTensor
      actionBuffer = actionBuffer :+ action
      actionLog = actionLog :+ logProbability
      rewardBuffer = rewardBuffer :+ rewards
    }

    ticks += 1
    episodeTicks += 1
  }
}

object PPOLearner {
  val np = py.module("numpy")
  val sys = py.module("sys")
  sys.path.insert(0, "./src/main/python") // otherwise it cannot get the definition
  val networks = py.module("networks")
  def actor(inputSize: Int, actionSize: Int): py.Dynamic =
    networks.Actor(inputSize, actionSize, 64)
  def critic(inputSize: Int): py.Dynamic =
    networks.Critic(inputSize, 128)
}
