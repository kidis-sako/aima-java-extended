package aima.test.core.unit.environment.vacuum;

import aima.core.agent.Action;
import aima.core.environment.vacuum.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the IntelligentVacuumAgent in various scenarios.
 */
public class IntelligentVacuumAgentTest {

    @Test
    public void testSmallEnvironment() {
        // Create a 3x3 environment with 50% dirt and no obstacles
        MazeVacuumEnvironment env = new MazeVacuumEnvironment(3, 3, 0.5, 0.0);
        IntelligentVacuumAgent agent = new IntelligentVacuumAgent();
        env.addAgent(agent);

        // Run until the agent stops
        while (!env.isDone()) {
            env.step();
        }

        // Verify that all accessible squares are clean
        for (String loc : env.getLocations()) {
            if (env.getLocationState(loc) != null) {  // not an obstacle
                Assert.assertEquals("Location " + loc + " should be clean",
                        VacuumEnvironment.LocationState.Clean, env.getLocationState(loc));
            }
        }
    }

    @Test
    public void testWithObstacles() {
        // Create a 4x4 environment with 50% dirt and 20% obstacles
        MazeVacuumEnvironment env = new MazeVacuumEnvironment(4, 4, 0.5, 0.2);
        IntelligentVacuumAgent agent = new IntelligentVacuumAgent();
        env.addAgent(agent);

        // Run until the agent stops
        while (!env.isDone()) {
            env.step();
        }

        // Verify that all accessible squares are clean
        for (String loc : env.getLocations()) {
            if (env.getLocationState(loc) != null) {  // not an obstacle
                Assert.assertEquals("Location " + loc + " should be clean",
                        VacuumEnvironment.LocationState.Clean, env.getLocationState(loc));
            }
        }
    }

    @Test
    public void testFullyDirtyEnvironment() {
        // Create a 3x3 environment with 100% dirt for debugging
        MazeVacuumEnvironment env = new MazeVacuumEnvironment(3, 3, 1.0, 0.0);
        IntelligentVacuumAgent agent = new IntelligentVacuumAgent();
        env.addAgent(agent);

        // Run until the agent stops
        while (!env.isDone()) {
            env.step();
        }

        // Verify that all squares are clean
        for (String loc : env.getLocations()) {
            Assert.assertEquals("Location " + loc + " should be clean",
                    VacuumEnvironment.LocationState.Clean, env.getLocationState(loc));
        }
    }
} 