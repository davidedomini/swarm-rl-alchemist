import math
import random

import numpy as np

import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from torch.distributions import MultivariateNormal

def init_weights(m):
    if isinstance(m, nn.Linear):
        nn.init.normal_(m.weight, mean=0., std=0.1)
        nn.init.constant_(m.bias, 0.1)

class Actor(nn.Module):
    def __init__(self, num_inputs, num_outputs, hidden_size, std=0.1):
        super(Actor, self).__init__()

        self.actor = nn.Sequential(
            nn.Linear(num_inputs, hidden_size),
            nn.Tanh(),
            nn.Linear(hidden_size, hidden_size),
            nn.Tanh(),
            nn.Linear(hidden_size, num_outputs)
        )
        self.log_std = nn.Parameter(torch.full((num_outputs,), std * std))

        #self.apply(init_weights)

    def forward(self, x):
        mu      = self.actor(x)
        std     = self.log_std#.expand_as(mu)
        cov_mat = torch.diag_embed(std)
        dist    = MultivariateNormal(mu, cov_mat)
        return dist

class Critic(nn.Module):
    def __init__(self, num_inputs, hidden_size):
        super(Critic, self).__init__()

        self.critic = nn.Sequential(
            nn.Linear(num_inputs, hidden_size),
            nn.ReLU(),
            nn.Linear(hidden_size, hidden_size),
            nn.ReLU(),
            nn.Linear(hidden_size, 1)
        )

        #self.apply(init_weights)

    def forward(self, x):
        value = self.critic(x)
        return value