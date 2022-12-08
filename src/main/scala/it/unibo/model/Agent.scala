package it.unibo.model

import Agent._
trait Agent[Observation, Action] {
  private var modeMemory: AgentMode = Test

  /** Current mode followed by this agent */
  def optimal: Observation => Action
  def behavioural: Observation => Action
  def mode: AgentMode = modeMemory

  /** Using the observation, it chooses an action */
  def act(state: Observation): Action = (if (mode == Training) behavioural else optimal) (state)

  /** The environment response (reward) when the agent is in this state and performs that action moving the environment
    * in the nextState
    */
  def record(state: Observation, action: Action, reward: Double, nextState: Observation): Unit = {}

  /** try to improve the current policy, giving what it had recorded */
  def improve(): Unit

  /** Reset any internal agent structure using during test/training */
  def reset(): Unit = {}

  /** Enter in training mode (in some agents this could not perform any effect) */
  def trainingMode(): Unit = this.modeMemory = Training

  /** Enter in the test mode */
  def testMode(): Unit = this.modeMemory = Test

  /** Store the internal status of the agent */
  def snapshot(episode: Int): Unit
}

object Agent {
  sealed trait AgentMode
  final case object Test extends AgentMode
  final case object Training extends AgentMode
}
