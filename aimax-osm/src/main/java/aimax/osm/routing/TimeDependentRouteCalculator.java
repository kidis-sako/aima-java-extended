package aimax.osm.routing;

import aima.core.search.framework.Node;
import aima.core.search.framework.problem.Problem;
import aima.core.search.framework.problem.StepCostFunction;
import aimax.osm.data.MapWayAttFilter;
import aimax.osm.data.MapWayFilter;
import aimax.osm.data.OsmMap;
import aimax.osm.data.Position;
import aimax.osm.data.entities.MapNode;
import aimax.osm.data.entities.MapWay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Extension of the RouteCalculator which additionally provides
 * routing based on estimated travel time.
 * 
 * @author Kidis Sako
 */
public class TimeDependentRouteCalculator extends RouteCalculator {

    // Speed estimates in km/h for different road types
    private static final Map<String, Double> SPEED_ESTIMATES = new HashMap<>();
    
    static {
        // Initialize speed estimates for different road types (km/h)
        SPEED_ESTIMATES.put("motorway", 120.0);
        SPEED_ESTIMATES.put("motorway_link", 80.0);
        SPEED_ESTIMATES.put("trunk", 100.0);
        SPEED_ESTIMATES.put("trunk_link", 70.0);
        SPEED_ESTIMATES.put("primary", 80.0);
        SPEED_ESTIMATES.put("primary_link", 60.0);
        SPEED_ESTIMATES.put("secondary", 60.0);
        SPEED_ESTIMATES.put("tertiary", 50.0);
        SPEED_ESTIMATES.put("road", 40.0);
        SPEED_ESTIMATES.put("residential", 30.0);
        SPEED_ESTIMATES.put("living_street", 20.0);
        SPEED_ESTIMATES.put("service", 20.0);
        SPEED_ESTIMATES.put("unclassified", 40.0);
        // Default speed for unknown road types
        SPEED_ESTIMATES.put("default", 40.0);
    }

    /** Returns the names of all supported way selection options. */
    @Override
    public String[] getTaskSelectionOptions() {
        return new String[] { "Distance", "Distance (Car)", "Distance (Bike)", "Time (Car)" };
    }

    /** Factory method, responsible for way filter creation. */
    @Override
    protected MapWayFilter createMapWayFilter(OsmMap map, int taskSelection) {
        if (taskSelection == 1 || taskSelection == 3)
            return MapWayAttFilter.createCarWayFilter();
        else if (taskSelection == 2)
            return MapWayAttFilter.createBicycleWayFilter();
        else
            return MapWayAttFilter.createAnyWayFilter();
    }

    /** Factory method, responsible for problem creation. */
    @Override
    protected Problem<MapNode, OsmMoveAction> createProblem(MapNode[] pNodes, OsmMap map,
            MapWayFilter wayFilter, boolean ignoreOneways, int taskSelection) {
        
        if (taskSelection == 3) {
            // Use time-based cost function for Time (Car) option
            StepCostFunction<MapNode, OsmMoveAction> timeCostFunction =
					TimeDependentRouteCalculator::getTimeStepCosts;
            
            return new RouteFindingProblem(pNodes[0], pNodes[1], wayFilter, 
                    ignoreOneways, timeCostFunction);
        } else {
            // Use regular distance-based cost function for other options
            return new RouteFindingProblem(pNodes[0], pNodes[1], wayFilter, ignoreOneways);
        }
    }

    /** Factory method, responsible for heuristic function creation. */
    @Override
    protected ToDoubleFunction<Node<MapNode, OsmMoveAction>> createHeuristicFunction(MapNode[] pNodes,
                                                                   int taskSelection) {
        if (taskSelection == 3) {
            // Use time-based heuristic for Time (Car) option
            return new OsmTimeHeuristicFunction(pNodes[1]);
        } else {
            // Use regular distance-based heuristic for other options
            return new OsmSldHeuristicFunction(pNodes[1]);
        }
    }

    /**
     * Calculates the travel time cost for moving along a particular road segment.
     * Time is calculated as distance / speed and returned in hours.
     */
    public static double getTimeStepCosts(MapNode state, OsmMoveAction action, MapNode statePrimed) {
        double distance = action.getTravelDistance(); // in km
        MapWay way = action.getWay();
        
        // Get the highway type
        String roadType = way.getAttributeValue("highway");
        
        // Look up the estimated speed for this road type (km/h)
        Double speed = SPEED_ESTIMATES.get(roadType);
        if (speed == null) {
            speed = SPEED_ESTIMATES.get("default");
        }
        
        // Calculate time in hours (distance / speed)
        return distance / speed;
    }

    /**
     * Heuristic function that estimates the travel time to the goal.
     */
    public static class OsmTimeHeuristicFunction implements ToDoubleFunction<Node<MapNode, OsmMoveAction>> {
        private final MapNode goalState;
        
        public OsmTimeHeuristicFunction(MapNode goalState) {
            this.goalState = goalState;
        }
        
        /**
         * Estimates travel time based on straight-line distance and an optimistic
         * speed estimate of 120 km/h (assumed to be the maximum allowed speed)
         */
        @Override
        public double applyAsDouble(Node<MapNode, OsmMoveAction> node) {
            double distanceKm = (new Position(node.getState())).getDistKM(goalState);
            // Use optimistic max speed of 120 km/h for the heuristic to ensure admissibility
            return distanceKm / 120.0; // Time in hours
        }
    }

    /**
     * Calculates the estimated travel time for a route.
     * @param nodes A list of MapNodes representing a route
     * @return The estimated travel time in hours
     */
    public static double calculateTravelTimeHours(List<MapNode> nodes) {
        if (nodes == null || nodes.size() < 2) {
            return 0.0;
        }
        
        double totalTime = 0.0;
        
        for (int i = 1; i < nodes.size(); i++) {
            MapNode n1 = nodes.get(i - 1);
            MapNode n2 = nodes.get(i);
            
            // Get the distance in km
            double distance = Position.getDistKM(n1.getLat(), n1.getLon(), n2.getLat(), n2.getLon());
            
            // For simplicity, assume default road type with speed 60 km/h
            // In a real implementation, we would need to find the actual road connecting these nodes
            totalTime += distance / 60.0;
        }
        
        return totalTime;
    }
    
    /**
     * Formats travel time from hours to a human-readable string
     * @param hours Travel time in hours
     * @return Formatted string like "1h 30min"
     */
    public static String formatTravelTime(double hours) {
        int hoursPart = (int) hours;
        int minutesPart = (int) Math.round((hours - hoursPart) * 60);
        
        if (hoursPart > 0) {
            return hoursPart + "h " + minutesPart + "min";
        } else {
            return minutesPart + "min";
        }
    }
} 