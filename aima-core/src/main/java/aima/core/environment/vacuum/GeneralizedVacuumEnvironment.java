package aima.core.environment.vacuum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import aima.core.agent.Action;
import aima.core.agent.Agent;
import aima.core.agent.impl.AbstractEnvironment;
import aima.core.agent.impl.DynamicAction;

/**
 * A generalized vacuum environment where the agent operates in a row of squares.
 * The agent can perceive local cleanliness and movement possibilities.
 * The environment is unbounded (agent doesn't know total number of squares).
 */
public class GeneralizedVacuumEnvironment extends AbstractEnvironment<VacuumPercept, Action> {
	public static final Action ACTION_MOVE_LEFT = new DynamicAction("Left");
	public static final Action ACTION_MOVE_RIGHT = new DynamicAction("Right");
	public static final Action ACTION_SUCK = new DynamicAction("Suck");
	
	private final List<Boolean> squares; // true = clean, false = dirty
	private int agentLocation;
	
	public GeneralizedVacuumEnvironment(int numSquares, double dirtProbability) {
		squares = new ArrayList<>();
		Random random = new Random();
		
		// Initialize squares with random dirt
		for (int i = 0; i < numSquares; i++) {
			squares.add(random.nextDouble() > dirtProbability);
		}
		
		// Place agent at random position
		agentLocation = random.nextInt(numSquares);
	}
	
	@Override
	public VacuumPercept getPerceptSeenBy(Agent<?, ?> agent) {
		VacuumPercept percept = new VacuumPercept(
				String.valueOf(agentLocation),
				squares.get(agentLocation) ? VacuumEnvironment.LocationState.Clean : VacuumEnvironment.LocationState.Dirty
		);
		// Add movement possibilities to the percept
		percept.setAttribute("canMoveLeft", canMoveLeft());
		percept.setAttribute("canMoveRight", canMoveRight());
		return percept;
	}
	
	@Override
	public void execute(Agent<?, ?> agent, Action action) {
		if (action.equals(ACTION_SUCK)) {
			if (!squares.get(agentLocation)) {
				squares.set(agentLocation, true);
				updatePerformanceMeasure(agent, 10.0);
			}
		}
		else if (action.equals(ACTION_MOVE_LEFT)) {
			if (agentLocation > 0) {
				agentLocation--;
				updatePerformanceMeasure(agent, -1.0);
			}
		}
		else if (action.equals(ACTION_MOVE_RIGHT)) {
			if (agentLocation < squares.size() - 1) {
				agentLocation++;
				updatePerformanceMeasure(agent, -1.0);
			}
		}
	}
	
	public boolean canMoveLeft() {
		return agentLocation > 0;
	}
	
	public boolean canMoveRight() {
		return agentLocation < squares.size() - 1;
	}
	
	public boolean isClean() {
		return squares.get(agentLocation);
	}
	
	public int getAgentLocation() {
		return agentLocation;
	}
} 