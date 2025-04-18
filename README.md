# AIMA-Java Extended

This is an extended version of the [AIMA-Java](https://github.com/aimacode/aima-java) project, implementing enhanced algorithms from "Artificial Intelligence: A Modern Approach" by Stuart Russell and Peter Norvig. The project has been updated to Java 17 and includes improved DataResource handling.

## Key Extended Applications

### 1. Generalized Vacuum Agent Application
The `GeneralizedVacuumAgentApp` (`aima-gui/src/main/java/aima/gui/fx/applications/agent/GeneralizedVacuumAgentApp.java`) extends the classic vacuum cleaner problem with:
- Configurable environment size (4, 8, or 12 squares)
- Adjustable dirt probability (0.2, 0.5, or 0.8)
- Real-time performance measurement
- JavaFX-based visualization
- Improved state management and environment interaction

### 2. Extended Route Finding Agent OSM Application
The `ExtendedRouteFindingAgentOsmApp` (`aimax-osm/src/main/java/aimax/osm/gui/fx/applications/ExtendedRouteFindingAgentOsmApp.java`) enhances the original route finding application with:
- Multiple goal support (more than two markers)
- Visual search space exploration (highlighted expanded nodes)
- Enhanced map rendering with customizable styles
- Support for different vehicle types
- Improved state visualization and tracking
- Java 17 compatibility with updated JavaFX integration

### 3. Time-Dependent Route Planning (by Kidis Sako)
The `TimeDependentRouteCalculator` (`aimax-osm/src/main/java/aimax/osm/routing/TimeDependentRouteCalculator.java`) extends the base route calculator with:
- Time-based routing using estimated travel times
- Speed estimates for different road types (motorway, trunk, primary, etc.)
- Time-based heuristic function for A* search
- Travel time calculation and formatting utilities
- Support for both distance and time-based route optimization

## Technical Improvements

### Java 17 Compatibility
- Updated to use Java 17 language features
- Enhanced type inference and pattern matching

### Improved DataResource
- Enhanced resource loading and management
- Better error handling and recovery

## Building and Running

[Previous build and run instructions remain unchanged...]

## License

[Previous license information remains unchanged...]