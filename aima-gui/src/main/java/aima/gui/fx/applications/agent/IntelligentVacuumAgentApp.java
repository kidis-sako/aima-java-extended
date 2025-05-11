package aima.gui.fx.applications.agent;

import aima.core.agent.Action;
import aima.core.agent.impl.SimpleAgent;
import aima.core.environment.vacuum.*;
import aima.core.util.Tasks;
import aima.gui.fx.framework.IntegrableApplication;
import aima.gui.fx.framework.Parameter;
import aima.gui.fx.framework.TaskExecutionPaneBuilder;
import aima.gui.fx.framework.TaskExecutionPaneCtrl;
import aima.gui.fx.views.VacuumEnvironmentViewCtrl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates the intelligent vacuum agent in a GUI environment.
 * Extends IntegrableApplication to work with the AIMA GUI framework.
 * 
 * VM options (Java>8): --module-path ${PATH_TO_FX} --add-modules javafx.controls
 */
public class IntelligentVacuumAgentApp extends IntegrableApplication {
    public static final String PARAM_ENV = "environment";
    public static final String PARAM_AGENT = "agent";
    
    protected TaskExecutionPaneCtrl taskPaneCtrl;
    protected VacuumEnvironmentViewCtrl envViewCtrl;
    protected VacuumEnvironment env = null;
    protected SimpleAgent<VacuumPercept, Action> agent = null;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public String getTitle() {
        return "Intelligent Vacuum Agent App";
    }

    /**
     * Defines state view, parameters, and call-back functions and calls the
     * simulation pane builder to create layout and controller objects.
     */
    @Override
    public Pane createRootPane() {
        BorderPane root = new BorderPane();

        StackPane envView = new StackPane();
        envViewCtrl = new VacuumEnvironmentViewCtrl(envView, action -> {
            if (action == VacuumEnvironment.ACTION_MOVE_LEFT) return 270.0;
            else if (action == VacuumEnvironment.ACTION_MOVE_RIGHT) return 90.0;
            else if (action == MazeVacuumEnvironment.ACTION_MOVE_UP) return 0.0;
            else if (action == MazeVacuumEnvironment.ACTION_MOVE_DOWN) return 180.0;
            else return null;
        });

        List<Parameter> params = createParameters();

        TaskExecutionPaneBuilder builder = new TaskExecutionPaneBuilder();
        builder.defineParameters(params);
        builder.defineStateView(envView);
        builder.defineInitMethod(this::initialize);
        builder.defineTaskMethod(this::startExperiment);
        taskPaneCtrl = builder.getResultFor(root);

        return root;
    }

    protected List<Parameter> createParameters() {
        Parameter p1 = new Parameter(PARAM_ENV,
                "Small Maze (3x3)",
                "Medium Maze with Obstacles (4x4)",
                "Competition 1 (6x6)",
                "Competition 2 (8x8)");
        Parameter p2 = new Parameter(PARAM_AGENT,
                "IntelligentVacuumAgent",
                "RandomWalkVacuumAgent");
        return Arrays.asList(p1, p2);
    }

    /**
     * Creates a vacuum environment and a corresponding agent based on the
     * state of the selectors.
     */
    @Override
    public void initialize() {
        switch (taskPaneCtrl.getParamValueIndex(PARAM_ENV)) {
            case 0: // Small Maze
                env = new MazeVacuumEnvironment(3, 3, 0.5, 0.0);
                break;
            case 1: // Medium Maze with Obstacles
                env = new MazeVacuumEnvironment(4, 4, 0.5, 0.2);
                break;
            case 2: // Competition 1
                env = new MazeVacuumEnvironment(6, 6, 0.5, 0.15);
                break;
            case 3: // Competition 2
                env = new MazeVacuumEnvironment(8, 8, 0.6, 0.2);
                break;
        }

        switch (taskPaneCtrl.getParamValueIndex(PARAM_AGENT)) {
            case 0:
                agent = new IntelligentVacuumAgent();
                break;
            case 1:
                agent = new RandomWalkVacuumAgent();
                break;
        }

        if (env != null && agent != null) {
            envViewCtrl.initialize(env);
            env.addEnvironmentListener(envViewCtrl);
            env.addAgent(agent);
        }
    }

    public void startExperiment() {
        if (env == null || agent == null) return;
        
        // Create a background thread for the simulation
        Thread simulationThread = new Thread(() -> {
            try {
                while (!env.isDone() && !Tasks.currIsCancelled()) {
                    // Execute environment step
                    env.step();
                    
                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        taskPaneCtrl.setStatus("Performance=" + env.getPerformanceMeasure(agent));
                        envViewCtrl.update();
                    });
                    
                    // Add delay to make movement visible
                    Thread.sleep(500);
                    
                    // Wait for step completion if needed
                    taskPaneCtrl.waitAfterStep();
                }
                
                // Final update on JavaFX thread
                Platform.runLater(() -> {
                    envViewCtrl.notify("Performance=" + env.getPerformanceMeasure(agent));
                });
            } catch (InterruptedException e) {
                // Handle interruption
                if (!Tasks.currIsCancelled()) {
                    e.printStackTrace();
                }
            }
        });
        
        // Start the simulation thread
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    @Override
    public void cleanup() {
        taskPaneCtrl.cancelExecution();
    }
} 