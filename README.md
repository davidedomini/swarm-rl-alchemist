# Large-Scale Multi-Agent Reinforcement Learning with Alchemist

This repository contains two examples in which we use Alchemist as simulator 
in a context of multi agent deep reinforcement learning.

More details about Alchemist, its execution model, its main abstractions could 
be found in the [primer repository](https://github.com/AlchemistSimulator/alchemist-primer)
and in the [official website](http://alchemistsimulator.github.io/).

## General description
Each simulation is described in [src/main/yaml/](src/main/yaml/). 
Configurations that start with `training` are the one in which homogenous Deep Q Learning is deployed.
Simulation that start with `test` are the one in which the agent directly use the learned Q table to perform their action.
The collective movement is performed at each simulated time unit. The progression is collectively made.
Both the scenario use the same technique for training: a central deep q learner gather the experience and improve the 
shared Q table with them. At the end, the best Q table is shared with the whole collective.

## Scenario 1: Learn to 'Flock'
In this scenario
(described in [testCohesion.yml](./src/main/yaml/testCohesion.yml) and
[trainingCohesion.yml](./src/main/yaml/trainingCohesion.yml)
) a group of 20 agent learn how to stay close with each other without colliding. 
Each agent has a fixed neighborhood (the four closest). The state of each agent consist 
of the relative distance to the neighborhood. 
The action that each agent could perform consist in 8 possible direction (north, south, east, ...)

This is the performance at the beginning of the training phase:
![cohesion-start](https://user-images.githubusercontent.com/23448811/208730482-ac25acce-db44-43b6-adc6-4adcc92e9753.gif)

This is the learned policy:
![end](https://user-images.githubusercontent.com/23448811/208730510-b7d7a7d8-a2cc-4d59-aaf2-04f1d564dc7d.gif)

## Scenario 2: Follow the leader

In the rest of the simulations, there is a combination between aggregate computing and 
deep reinforcement learning. In particular, in this case aggregate computing is used to share 
the information of the leader in the system. Then, Q learning is used to learn how to follow it.

This is the performance at the beginning of the training phase:
![start](https://user-images.githubusercontent.com/23448811/208730521-07093c9e-f8b4-4c2b-a479-c1847dd7c56f.gif)

This is the learned policy:
![end-leader](https://user-images.githubusercontent.com/23448811/208730533-1ee21f1e-eab7-4d2f-93f1-833d532a2832.gif)

