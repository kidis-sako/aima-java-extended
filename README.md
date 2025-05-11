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

### 2. Intelligent Vacuum Agent
The `IntelligentVacuumAgent` (`aima-core/src/main/java/aima/core/environment/vacuum/IntelligentVacuumAgent.java`) implements an advanced cleaning strategy with:

#### Core Features:
- **Mental Map Management**:
  - Maintains a detailed map of visited locations
  - Tracks cell states: CLEAN, DIRTY, OBSTACLE, UNKNOWN
  - Updates boundaries dynamically for efficient exploration

#### Decision Making:
- **Priority-Based Targeting**:
  1. Highest priority (100 points): Clean dirty cells
  2. High priority (80 points): Explore unknown cells adjacent to visited areas
  - Distance-based scoring for optimal path selection
  - Obstacle avoidance with increased movement costs

#### Path Planning:
- **A* Path Finding**:
  - Optimal path calculation to targets
  - Cost considerations for turning and obstacles
  - Efficient exploration of unknown areas

#### Stopping Conditions:
- Stops when either:
  1. No known dirty cells AND stuck in a loop (5 repetitions)
  2. No known dirty cells AND no unexplored adjacent cells after 20 moves
- Prevents unnecessary exploration of clean areas

#### Performance Tracking:
- Monitors cleaning efficiency
- Tracks visited locations
- Detects and handles movement loops
- Maintains exploration statistics

### 3. Extended Route Finding Agent OSM Application
The `ExtendedRouteFindingAgentOsmApp` (`aimax-osm/src/main/java/aimax/osm/gui/fx/applications/ExtendedRouteFindingAgentOsmApp.java`) enhances the original route finding application with:
- Multiple goal support (more than two markers)
- Visual search space exploration (highlighted expanded nodes)
- Enhanced map rendering with customizable styles
- Support for different vehicle types
- Improved state visualization and tracking
- Java 17 compatibility with updated JavaFX integration

### 4. Time-Dependent Route Planning (by Kidis Sako)
The `TimeDependentRouteCalculator` (`aimax-osm/src/main/java/aimax/osm/routing/TimeDependentRouteCalculator.java`) extends the base route calculator with:
- Time-based routing using estimated travel times
- Speed estimates for different road types (motorway, trunk, primary, etc.)
- Time-based heuristic function for A* search
- Travel time calculation and formatting utilities
- Support for both distance and time-based route optimization

### 5. Sudoku CSP Solver
The Sudoku implementation demonstrates advanced Constraint Satisfaction Problem (CSP) solving techniques with three main components:

#### Core Implementation (`SudokuCSP`)
Located in `aima-core/src/main/java/aima/core/search/csp/examples/SudokuCSP.java`:
- Implements the Sudoku puzzle as a CSP
- Defines variables for each cell (81 total)
- Sets up domains (1-9) for each variable
- Adds three types of constraints:
  - Row constraints (no duplicates in rows)
  - Column constraints (no duplicates in columns)
  - Box constraints (no duplicates in 3x3 boxes)
- Provides methods for initializing puzzles and setting fixed values

#### Command-line Demo (`SudokuCspDemo`)
Located in `aima-gui/src/main/java/aima/gui/fx/applications/search/SudokuCspDemo.java`:
- Demonstrates solving Sudoku using different strategies:
  - Min-Conflicts search
  - Backtracking with MRV, DEG, LCV, and AC3
  - Simple backtracking
- Shows step counts and performance metrics
- Uses example puzzles from SimpleSudokuApp

#### GUI Application (`SudokuCspApp`)
Located in `aima-gui/src/main/java/aima/gui/fx/applications/search/SudokuCspApp.java`:
- Provides interactive Sudoku solving with:
  - Multiple puzzle selection
  - Strategy selection (Backtracking, Min-Conflicts)
  - Heuristic configuration:
    - MRV (Minimum Remaining Values)
    - DEG (Degree)
    - LCV (Least Constraining Value)
  - Inference method selection:
    - Forward Checking
    - AC3 (Arc Consistency)
- Real-time visualization of solving process
- Step-by-step solving with highlights
- Performance monitoring

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