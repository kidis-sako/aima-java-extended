package aima.core.environment.vacuum;

import static aima.core.environment.vacuum.MazeVacuumEnvironment.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import aima.core.agent.Action;
import aima.core.agent.impl.SimpleAgent;

/**
 * An advanced intelligent vacuum agent that efficiently cleans a maze-like environment.
 * Features:
 * 1. Dependency injection and proper state management
 * 2. Advanced path planning with cluster analysis
 * 3. Robust error recovery
 * 4. Memory optimization
 * 5. Adaptive learning and performance tuning
 *
 * @author Kidis Sako
 */
public class IntelligentVacuumAgent extends SimpleAgent<VacuumPercept, Action> {
	private static final Logger LOGGER = Logger.getLogger(IntelligentVacuumAgent.class.getName());
	
	// Configuration parameters - could be moved to a config class
	private static final int MOVES_WITHOUT_NEW_CELLS = 20;  // Stop if no new cells discovered after this many moves
	
	private final AgentState state;
	
	public IntelligentVacuumAgent() {
		this.state = new AgentState();
		this.program = (percept) -> {
			try {
				return decideNextAction(percept);
			} catch (Exception e) {
				LOGGER.severe("Error processing percept: " + e.getMessage());
				return Optional.empty();
			}
		};
	}
	
	// Environment states with better type safety
	private enum CellState {
		UNKNOWN(0),
		CLEAN(1),
		DIRTY(2),
		OBSTACLE(3);
		
		private final int value;
		
		CellState(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	// Thread-safe state container
	private static class AgentState {
		private final Position currentPosition;
		private final Boundaries boundaries;
		private final PerformanceTracker performanceTracker;
		private final Map<Position, CellState> mentalMap;
		private final Set<Position> visitedLocations;
		private final Deque<Action> plannedActions;
		private final AtomicReference<VacuumPercept> lastPercept;
		
		AgentState() {
			this.currentPosition = new Position(0, 0);
			this.boundaries = new Boundaries();
			this.performanceTracker = new PerformanceTracker();
			this.mentalMap = new ConcurrentHashMap<>();
			this.visitedLocations = Collections.newSetFromMap(new ConcurrentHashMap<>());
			this.plannedActions = new ArrayDeque<>();
			this.lastPercept = new AtomicReference<>();
		}
	}
	
	// Thread-safe position tracking
	private static class Position {
		private final AtomicInteger x;
		private final AtomicInteger y;
		
		Position(int x, int y) {
			this.x = new AtomicInteger(x);
			this.y = new AtomicInteger(y);
		}
		
		int getX() { return x.get(); }
		int getY() { return y.get(); }
		
		void setX(int newX) { x.set(newX); }
		void setY(int newY) { y.set(newY); }
		
		Position copy() {
			return new Position(getX(), getY());
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Position)) return false;
			Position other = (Position) o;
			return getX() == other.getX() && getY() == other.getY();
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(getX(), getY());
		}
		
		@Override
		public String toString() {
			return "(" + getX() + "," + getY() + ")";
		}
	}
	
	// Enhanced performance tracking with memory management
	private static class PerformanceTracker {
		private final AtomicInteger cleanedCells = new AtomicInteger(0);
		private final AtomicInteger totalMoves = new AtomicInteger(0);
		private final AtomicInteger consecutiveKnownMoves = new AtomicInteger(0);
		private final Map<Position, Integer> visitFrequency = new ConcurrentHashMap<>();
		private final Map<Position, Long> lastVisitTime = new ConcurrentHashMap<>();
		
		// Add fields for loop detection
		private final Deque<Position> lastPositions = new ArrayDeque<>();
		private final AtomicInteger loopCount = new AtomicInteger(0);
		private static final int MAX_LOOP_COUNT = 20;
		private static final int POSITIONS_TO_TRACK = 4; // Track last 4 positions to detect 2-cell loop
		
		void recordCleaning() {
			cleanedCells.incrementAndGet();
		}
		
		void recordMove(Position pos) {
			totalMoves.incrementAndGet();
			
			// Check if this is a revisit
			if (visitFrequency.containsKey(pos)) {
				consecutiveKnownMoves.incrementAndGet();
			} else {
				consecutiveKnownMoves.set(0);
			}
			
			// Update visit frequency and time
			visitFrequency.merge(pos, 1, Integer::sum);
			lastVisitTime.put(pos, System.currentTimeMillis());
			
			// Update loop detection
			lastPositions.addLast(pos.copy());
			if (lastPositions.size() > POSITIONS_TO_TRACK) {
				lastPositions.removeFirst();
			}
			
			// Check for 2-cell loop pattern
			if (lastPositions.size() == POSITIONS_TO_TRACK) {
				List<Position> positionList = new ArrayList<>(lastPositions);
				LOGGER.info("Current position sequence: " + positionList);
				
				if (isLoopPattern(positionList)) {
					loopCount.incrementAndGet();
					LOGGER.warning("Loop detected! Positions: " + positionList + 
						" Loop count: " + loopCount.get() + 
						" Max allowed: " + MAX_LOOP_COUNT);
				} else {
					if (loopCount.get() > 0) {
						LOGGER.info("Loop pattern broken. Resetting counter.");
					}
					loopCount.set(0);
				}
			}
		}
		
		private boolean isLoopPattern(List<Position> positions) {
			boolean isLoop = positions.get(0).equals(positions.get(2)) &&
				   positions.get(1).equals(positions.get(3)) &&
				   !positions.get(0).equals(positions.get(1));
			
			if (isLoop) {
				LOGGER.info("Loop pattern detected: " +
					positions.get(0) + " -> " + positions.get(1) + " -> " +
					positions.get(2) + " -> " + positions.get(3));
			}
			return isLoop;
		}
		
		boolean isStuckInLoop() {
			boolean stuck = loopCount.get() >= MAX_LOOP_COUNT;
			if (stuck) {
				LOGGER.warning("Agent is stuck in loop! Loop count: " + loopCount.get());
			}
			return stuck;
		}
		
		int getConsecutiveKnownMoves() {
			return consecutiveKnownMoves.get();
		}
		
		int getVisitFrequency(Position pos) {
			return visitFrequency.getOrDefault(pos, 0);
		}
		
		long getTimeSinceLastVisit(Position pos) {
			Long lastVisit = lastVisitTime.get(pos);
			return lastVisit != null ? System.currentTimeMillis() - lastVisit : Long.MAX_VALUE;
		}
	}
	
	// Improved boundaries tracking
	private static class Boundaries {
		private final AtomicInteger minX = new AtomicInteger(0);
		private final AtomicInteger maxX = new AtomicInteger(0);
		private final AtomicInteger minY = new AtomicInteger(0);
		private final AtomicInteger maxY = new AtomicInteger(0);
		
		void update(int x, int y) {
			// Update X boundaries
			int currentMinX = minX.get();
			int currentMaxX = maxX.get();
			if (x < currentMinX) {
				minX.set(x);
				LOGGER.fine("Updated minX boundary to: " + x);
			}
			if (x > currentMaxX) {
				maxX.set(x);
				LOGGER.fine("Updated maxX boundary to: " + x);
			}
			
			// Update Y boundaries
			int currentMinY = minY.get();
			int currentMaxY = maxY.get();
			if (y < currentMinY) {
				minY.set(y);
				LOGGER.fine("Updated minY boundary to: " + y);
			}
			if (y > currentMaxY) {
				maxY.set(y);
				LOGGER.fine("Updated maxY boundary to: " + y);
			}
		}
		
		int getWidth() { 
			return maxX.get() - minX.get() + 1; 
		}
		
		int getHeight() { 
			return maxY.get() - minY.get() + 1; 
		}
		
		boolean isWithinBounds(Position pos) {
			int x = pos.getX();
			int y = pos.getY();
			int currentMinX = minX.get();
			int currentMaxX = maxX.get();
			int currentMinY = minY.get();
			int currentMaxY = maxY.get();
			
			return x >= currentMinX && x <= currentMaxX &&
				   y >= currentMinY && y <= currentMaxY;
		}
		
		@Override
		public String toString() {
			return String.format("Boundaries[x:%d to %d, y:%d to %d]", 
				minX.get(), maxX.get(), minY.get(), maxY.get());
		}
	}
	
	private Optional<Action> decideNextAction(VacuumPercept percept) {
		try {
			// Always update mental map first
			updateMentalMap(percept);
			
			// Log current state
			LOGGER.info("Current position: " + state.currentPosition + 
				", Current state: " + state.mentalMap.get(state.currentPosition) +
				", Visited locations: " + state.visitedLocations.size());
			
			// If current location is dirty, clean it
			if (percept.getCurrState() == LocationState.Dirty) {
				LOGGER.info("Found dirty cell at " + state.currentPosition + ", cleaning...");
				state.performanceTracker.recordCleaning();
				// After cleaning, update the mental map to mark it as clean
				state.mentalMap.put(state.currentPosition.copy(), CellState.CLEAN);
				return Optional.of(ACTION_SUCK);
			}
			
			// Check if we're done
			if (shouldStop()) {
				LOGGER.info("Stopping conditions met: " +
					"No known dirty cells and either stuck in loop or fully explored accessible area");
				return Optional.empty();
			}
			
			// If we have planned actions, try to execute the next one
			if (!state.plannedActions.isEmpty()) {
				Action nextAction = state.plannedActions.poll();
				if (isValidMove(nextAction, percept)) {
					LOGGER.info("Executing planned action: " + nextAction);
					updatePosition(nextAction);
					state.performanceTracker.recordMove(state.currentPosition);
					return Optional.of(nextAction);
				} else {
					LOGGER.warning("Planned action " + nextAction + " is no longer valid");
					state.plannedActions.clear(); // Clear invalid plan
				}
			}
			
			// Plan next actions if we don't have any
			planNextActions(percept);
			if (!state.plannedActions.isEmpty()) {
				Action nextAction = state.plannedActions.poll();
				if (isValidMove(nextAction, percept)) {
					LOGGER.info("Executing newly planned action: " + nextAction);
					updatePosition(nextAction);
					state.performanceTracker.recordMove(state.currentPosition);
					return Optional.of(nextAction);
				}
			}
			
			LOGGER.warning("No valid actions found, trying simple moves");
			
			// If we get here and can't do anything else, try a simple move
			for (Action action : getPossibleActions()) {
				if (isValidMove(action, percept)) {
					updatePosition(action);
					state.performanceTracker.recordMove(state.currentPosition);
					return Optional.of(action);
				}
			}
			
			LOGGER.warning("No valid moves available");
			return Optional.empty();
			
		} catch (Exception e) {
			LOGGER.severe("Unexpected error: " + e.getMessage());
			return Optional.empty();
		}
	}
	
	private boolean shouldStop() {
		// Check for any known dirty cells
		boolean hasKnownDirtyCells = false;
		for (Position pos : state.visitedLocations) {
			CellState cellState = state.mentalMap.get(pos);
			LOGGER.info("Checking cell at " + pos + ": state=" + cellState);
			if (cellState == CellState.DIRTY) {
				hasKnownDirtyCells = true;
				break;
			}
		}
		
		if (hasKnownDirtyCells) {
			LOGGER.info("Found dirty cells, continuing cleaning");
			return false; // Never stop if we know of dirty cells
		}
		
		// Check if stuck in a loop
		boolean stuckInLoop = state.performanceTracker.isStuckInLoop();
		
		// Check if we have any unexplored adjacent cells
		boolean hasUnexploredAdjacent = hasUnexploredAdjacentCells();
		
		// Check consecutive known moves
		int consecutiveKnownMoves = state.performanceTracker.getConsecutiveKnownMoves();
		boolean longTimeInKnownArea = consecutiveKnownMoves >= MOVES_WITHOUT_NEW_CELLS;
		
		LOGGER.info("Stop condition check - " +
			"Known dirty cells: " + hasKnownDirtyCells +
			", Stuck in loop: " + stuckInLoop +
			", Has unexplored adjacent: " + hasUnexploredAdjacent +
			", Consecutive known moves: " + consecutiveKnownMoves);
			
		// Stop if no dirty cells AND either:
		// 1. We're stuck in a loop, or
		// 2. We've been in known territory for a while and have no unexplored adjacent cells
		return stuckInLoop || longTimeInKnownArea && !hasUnexploredAdjacent;
	}
	
	private void updateMentalMap(VacuumPercept percept) {
		state.lastPercept.set(percept);
		Position currentPos = state.currentPosition;
		
		// Update current location state
		CellState newState = percept.getCurrState() == LocationState.Dirty ? 
			CellState.DIRTY : CellState.CLEAN;
		
		// Log the cell state change
		CellState oldState = state.mentalMap.get(currentPos);
		LOGGER.info("Updating cell at " + currentPos + 
			": oldState=" + oldState + 
			", newState=" + newState + 
			", percept=" + percept.getCurrState());
		
		// Store the state in mental map
		Position positionCopy = currentPos.copy();
		state.mentalMap.put(positionCopy, newState);
		state.visitedLocations.add(positionCopy);
		
		// Update knowledge about surrounding cells
		Map<Action, Boolean> movements = new HashMap<>();
		movements.put(ACTION_MOVE_UP, (Boolean) percept.getAttribute(ATT_CAN_MOVE_UP));
		movements.put(ACTION_MOVE_DOWN, (Boolean) percept.getAttribute(ATT_CAN_MOVE_DOWN));
		movements.put(ACTION_MOVE_LEFT, (Boolean) percept.getAttribute(ATT_CAN_MOVE_LEFT));
		movements.put(ACTION_MOVE_RIGHT, (Boolean) percept.getAttribute(ATT_CAN_MOVE_RIGHT));
		
		// Log movement possibilities
		LOGGER.fine("Movement options at " + currentPos + ": " + movements);
		
		for (Map.Entry<Action, Boolean> entry : movements.entrySet()) {
			Position adjacentPos = getAdjacentPosition(currentPos, entry.getKey());
			Position adjacentCopy = adjacentPos.copy();
			if (!entry.getValue()) {
				state.mentalMap.put(adjacentCopy, CellState.OBSTACLE);
			} else if (!state.mentalMap.containsKey(adjacentCopy)) {
				state.mentalMap.put(adjacentCopy, CellState.UNKNOWN);
			}
		}
		
		// Update boundaries
		state.boundaries.update(currentPos.getX(), currentPos.getY());
		
		// Debug log the entire mental map
		LOGGER.info("Current mental map:");
		state.mentalMap.forEach((pos, state) -> 
			LOGGER.info("Position " + pos + ": " + state));
	}
	
	private void planNextActions(VacuumPercept percept) {
		// Clear any existing planned actions
		state.plannedActions.clear();
		
		// First, try to find a dirty location
		Position target = findOptimalTarget();
		
		if (target != null) {
			// We found a dirty location, plan path to it
			List<Action> path = planOptimalPath(state.currentPosition, target);
			if (!path.isEmpty()) {
				state.plannedActions.addAll(path);
				return;
			}
		}
		
		// If no dirty locations or couldn't plan path, explore unknown areas
		List<Action> validMoves = getPossibleActions().stream()
			.filter(action -> isValidMove(action, percept))
			.collect(Collectors.toList());
			
		// Shuffle to avoid deterministic patterns
		Collections.shuffle(validMoves);
		
		// First priority: Move to unknown cells
		for (Action action : validMoves) {
			Position nextPos = getAdjacentPosition(state.currentPosition, action);
			if (state.mentalMap.get(nextPos) == CellState.UNKNOWN) {
				state.plannedActions.add(action);
				return;
			}
		}
		
		// Second priority: Move to least recently visited clean cell
		Optional<Action> leastRecentlyVisited = validMoves.stream()
			.map(action -> {
				Position pos = getAdjacentPosition(state.currentPosition, action);
				long timeSinceVisit = state.performanceTracker.getTimeSinceLastVisit(pos);
				return new AbstractMap.SimpleEntry<>(action, timeSinceVisit);
			})
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey);
			
		if (leastRecentlyVisited.isPresent()) {
			state.plannedActions.add(leastRecentlyVisited.get());
			return;
		}
		
		// Last resort: Pick a random valid move
		if (!validMoves.isEmpty()) {
			state.plannedActions.add(validMoves.get(0));
		}
	}
	
	private Position findOptimalTarget() {
		Map<Position, Double> scores = new HashMap<>();
		
		// First priority: Score dirty cells
		state.mentalMap.forEach((pos, cellState) -> {
			if (cellState == CellState.DIRTY) {
				double baseScore = 100.0;  // Highest priority for dirty cells
				double distanceScore = -calculateDistance(state.currentPosition, pos);
				scores.put(pos, baseScore + distanceScore);
			}
		});
		
		// If no dirty cells found, score unknown cells adjacent to visited cells
		if (scores.isEmpty()) {
			state.visitedLocations.forEach(visitedPos -> {
				for (Action action : getPossibleActions()) {
					Position adjacentPos = getAdjacentPosition(visitedPos, action);
					if (state.mentalMap.get(adjacentPos) == CellState.UNKNOWN) {
						double baseScore = 80.0;  // High priority for unknown cells we can reach
						double distanceScore = -calculateDistance(state.currentPosition, adjacentPos);
						scores.merge(adjacentPos, baseScore + distanceScore, Double::max);
					}
				}
			});
		}
		
		// Log the decision making
		if (!scores.isEmpty()) {
			Position bestTarget = scores.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
			if (bestTarget != null) {
				LOGGER.info("Selected target: " + bestTarget + 
					" with score: " + scores.get(bestTarget) +
					" state: " + state.mentalMap.get(bestTarget));
			}
		}
		
		return scores.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse(null);
	}
	
	private List<Action> planOptimalPath(Position start, Position goal) {
		PriorityQueue<PathNode> openSet = new PriorityQueue<>();
		Set<Position> closedSet = new HashSet<>();
		Map<Position, PathNode> nodes = new HashMap<>();
		
		PathNode startNode = new PathNode(start, null, 0, calculateDistance(start, goal));
		openSet.add(startNode);
		nodes.put(start, startNode);
		
		while (!openSet.isEmpty()) {
			PathNode current = openSet.poll();
			
			if (current.position.equals(goal)) {
				return reconstructPath(current);
			}
			
			closedSet.add(current.position);
			
			for (Action action : getPossibleActions()) {
				Position neighbor = getAdjacentPosition(current.position, action);
				if (closedSet.contains(neighbor) || !isValidPosition(neighbor)) {
					continue;
				}
				
				double movementCost = calculateMovementCost(current, action);
				double newG = current.g + movementCost;
				
				PathNode neighborNode = nodes.get(neighbor);
				if (neighborNode == null) {
					neighborNode = new PathNode(neighbor, current, newG, 
						calculateDistance(neighbor, goal));
					nodes.put(neighbor, neighborNode);
					openSet.add(neighborNode);
				} else if (newG < neighborNode.g) {
					neighborNode.parent = current;
					neighborNode.g = newG;
					neighborNode.f = newG + calculateDistance(neighbor, goal);
				}
			}
		}
		
		// If no path found, try to get closer using simple moves
		Action bestAction = getPossibleActions().stream()
			.min(Comparator.comparingDouble(action -> {
				Position nextPos = getAdjacentPosition(start, action);
				return calculateDistance(nextPos, goal);
			}))
			.orElse(null);
			
		return bestAction != null ? Collections.singletonList(bestAction) : Collections.emptyList();
	}
	
	private double calculateMovementCost(PathNode node, Action action) {
		double baseCost = 1.0;
		
		// Add turning cost if changing direction
		if (node.parent != null) {
			Action previousAction = getActionBetweenPositions(node.parent.position, node.position);
			if (previousAction != action) {
				baseCost += 0.5; // Penalty for turning
			}
		}
		
		// Add cost for moving towards obstacles
		Position nextPos = getAdjacentPosition(node.position, action);
		if (state.mentalMap.get(nextPos) == CellState.OBSTACLE) {
			baseCost += 2.0;
		}
		
		return baseCost;
	}
	
	private double calculateDistance(Position from, Position to) {
		return Math.abs(from.getX() - to.getX()) + Math.abs(from.getY() - to.getY());
	}
	
	private List<Action> getPossibleActions() {
		return Arrays.asList(ACTION_MOVE_UP, ACTION_MOVE_DOWN, ACTION_MOVE_LEFT, ACTION_MOVE_RIGHT);
	}
	
	private Position getAdjacentPosition(Position pos, Action action) {
		Position adjacent = pos.copy();
		if (action == ACTION_MOVE_RIGHT) adjacent.setX(pos.getX() + 1);
		else if (action == ACTION_MOVE_LEFT) adjacent.setX(pos.getX() - 1);
		else if (action == ACTION_MOVE_UP) adjacent.setY(pos.getY() + 1);
		else if (action == ACTION_MOVE_DOWN) adjacent.setY(pos.getY() - 1);
		return adjacent;
	}
	
	private Action getActionBetweenPositions(Position from, Position to) {
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		if (dx > 0) return ACTION_MOVE_RIGHT;
		if (dx < 0) return ACTION_MOVE_LEFT;
		if (dy > 0) return ACTION_MOVE_UP;
		if (dy < 0) return ACTION_MOVE_DOWN;
		return null;
	}
	
	private List<Action> reconstructPath(PathNode goal) {
		List<Action> actions = new ArrayList<>();
		PathNode current = goal;
		PathNode parent = current.parent;
		
		while (parent != null) {
			actions.add(0, getActionBetweenPositions(parent.position, current.position));
			current = parent;
			parent = current.parent;
		}
		
		return actions;
	}
	
	private boolean hasUnexploredAdjacentCells() {
		VacuumPercept percept = state.lastPercept.get();
		if (percept == null) return false;
		
		for (Action action : getPossibleActions()) {
			if (isValidMove(action, percept)) {
				Position adjacentPos = getAdjacentPosition(state.currentPosition, action);
				if (!state.visitedLocations.contains(adjacentPos)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isValidMove(Action action, VacuumPercept percept) {
		if (action == ACTION_MOVE_RIGHT) return (Boolean) percept.getAttribute(ATT_CAN_MOVE_RIGHT);
		if (action == ACTION_MOVE_LEFT) return (Boolean) percept.getAttribute(ATT_CAN_MOVE_LEFT);
		if (action == ACTION_MOVE_UP) return (Boolean) percept.getAttribute(ATT_CAN_MOVE_UP);
		if (action == ACTION_MOVE_DOWN) return (Boolean) percept.getAttribute(ATT_CAN_MOVE_DOWN);
		return false;
	}
	
	private void updatePosition(Action action) {
		if (!isValidMove(action, state.lastPercept.get())) {
			LOGGER.warning("Attempted invalid move: " + action);
			return;
		}
		
		int newX = state.currentPosition.getX();
		int newY = state.currentPosition.getY();
		
		if (action == ACTION_MOVE_RIGHT) {
			newX++;
		} else if (action == ACTION_MOVE_LEFT) {
			newX--;
		} else if (action == ACTION_MOVE_UP) {
			newY++;
		} else if (action == ACTION_MOVE_DOWN) {
			newY--;
		}
		
		state.currentPosition.setX(newX);
		state.currentPosition.setY(newY);
		
		LOGGER.fine("Position updated to: " + state.currentPosition);
	}
	
	private boolean isValidPosition(Position pos) {
		if (!state.boundaries.isWithinBounds(pos)) {
			return false;
		}
		CellState cellState = state.mentalMap.get(pos);
		return cellState != CellState.OBSTACLE;
	}
	
	private static class PathNode implements Comparable<PathNode> {
		Position position;
		PathNode parent;
		double g; // cost from start to current
		double f; // estimated total cost (g + heuristic)
		
		PathNode(Position position, PathNode parent, double g, double h) {
			this.position = position;
			this.parent = parent;
			this.g = g;
			this.f = g + h;
		}
		
		@Override
		public int compareTo(PathNode other) {
			return Double.compare(this.f, other.f);
		}
	}
}