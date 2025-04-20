package aimax.osm.gui.fx.applications;

import aima.core.agent.Agent;
import aima.core.agent.impl.DynamicPercept;
import aima.core.environment.map.BidirectionalMapProblem;
import aima.core.environment.map.MapFunctions;
import aima.core.environment.map.MoveToAction;
import aima.core.search.framework.problem.GeneralProblem;
import aima.core.search.framework.problem.OnlineSearchProblem;
import aima.core.search.framework.problem.Problem;
import aima.core.search.online.LRTAStarAgent;
import aima.gui.fx.framework.Parameter;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class ExtendedOnlineAgentOsmApp extends OnlineAgentOsmApp {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public String getTitle() {
        return "Extended Online Agent OSM App";
    }

    @Override
    protected List<Parameter> createParameters() {
        List<Parameter> params = new ArrayList<>(super.createParameters());
        Parameter p = new Parameter("agentType", "LRTA*", "Smart Online Agent");
        p.setDefaultValueIndex(0);
        params.add(p);
        return params;
    }

    @Override
    protected Agent<DynamicPercept, MoveToAction> createAgent(List<String> locations) {
        if (simPaneCtrl.getParamValueIndex("agentType") == 0) {
            return super.createAgent(locations);
        } else {
            Problem<String, MoveToAction> p = new BidirectionalMapProblem(map, null, locations.get(1));
            OnlineSearchProblem<String, MoveToAction> osp = new GeneralProblem<>(
                    null, p::getActions, null, p::testGoal, p::getStepCosts);
            return new SmartOnlineAgent(osp, MapFunctions.createPerceptToStateFunction(), 
                    state -> MapFunctions.getSLD(state, locations.get(1), map));
        }
    }

    private class SmartOnlineAgent extends LRTAStarAgent<DynamicPercept, String, MoveToAction> {
        private static final double EXPLORATION_BONUS = 0.5;
        private String lastLocation;
        private Set<String> visitedLocations;
        private Map<String, Double> costCache;

        public SmartOnlineAgent(OnlineSearchProblem<String, MoveToAction> problem, 
                              Function<DynamicPercept, String> ptsFn, 
                              ToDoubleFunction<String> h) {
            super(problem, ptsFn, h);
            this.visitedLocations = new HashSet<>();
            this.costCache = new HashMap<>();
        }

        @Override
        public Optional<MoveToAction> act(DynamicPercept percept) {
            String currState = getPerceptToStateFunction().apply(percept);
            visitedLocations.add(currState);

            List<MoveToAction> actions = getProblem().getActions(currState);
            if (actions.isEmpty()) {
                return Optional.empty();
            }

            // Filter out actions that would lead back to the last location unless no other option
            List<MoveToAction> filteredActions = new ArrayList<>();
            for (MoveToAction action : actions) {
                String nextState = action.getToLocation();
                if (!nextState.equals(lastLocation)) {
                    filteredActions.add(action);
                }
            }

            // If all actions lead back, use original actions
            if (filteredActions.isEmpty()) {
                filteredActions = actions;
            }

            // Choose action with lowest estimated total cost
            MoveToAction bestAction = null;
            double minEstimatedCost = Double.POSITIVE_INFINITY;

            for (MoveToAction action : filteredActions) {
                String nextState = action.getToLocation();
                double actionCost = getProblem().getStepCosts(currState, action, nextState);
                double hValue = getHeuristicFunction().applyAsDouble(nextState);
                
                // Add exploration bonus for unvisited states
                if (!visitedLocations.contains(nextState)) {
                    hValue -= EXPLORATION_BONUS;
                }

                double estimatedTotalCost = actionCost + hValue;
                if (estimatedTotalCost < minEstimatedCost) {
                    minEstimatedCost = estimatedTotalCost;
                    bestAction = action;
                }
            }

            lastLocation = currState;
            return Optional.ofNullable(bestAction);
        }
    }
} 