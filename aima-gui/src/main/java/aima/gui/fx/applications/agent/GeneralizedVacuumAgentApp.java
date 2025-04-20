package aima.gui.fx.applications.agent;
import aima.core.agent.Action;
import aima.core.environment.vacuum.GeneralizedVacuumAgent;
import aima.core.environment.vacuum.GeneralizedVacuumEnvironment;
import aima.core.environment.vacuum.VacuumPercept;
import aima.gui.fx.framework.IntegrableApplication;
import aima.gui.fx.framework.Parameter;
import aima.gui.fx.framework.TaskExecutionPaneBuilder;
import aima.gui.fx.framework.TaskExecutionPaneCtrl;
import aima.gui.fx.views.SimpleEnvironmentViewCtrl;
import aima.core.util.Tasks;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.Arrays;
import java.util.List;

/**
 * Application which demonstrates the generalized vacuum cleaner agent
 * operating in a row of squares.
 */
public class GeneralizedVacuumAgentApp extends IntegrableApplication {

    public static void main(String[] args) {
        launch(args);
    }

    public final static String PARAM_NUM_SQUARES = "numSquares";
    public final static String PARAM_DIRT_PROB = "dirtProbability";

    protected TaskExecutionPaneCtrl taskPaneCtrl;
    protected SimpleEnvironmentViewCtrl<VacuumPercept, Action> envViewCtrl;
    protected GeneralizedVacuumEnvironment env = null;
    protected GeneralizedVacuumAgent agent = null;

    @Override
    public String getTitle() {
        return "Generalized Vacuum Agent App";
    }

    @Override
    public Pane createRootPane() {
        BorderPane root = new BorderPane();

        StackPane envView = new StackPane();
        envViewCtrl = new SimpleEnvironmentViewCtrl<>(envView);

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
        Parameter p1 = new Parameter(PARAM_NUM_SQUARES, "4 squares", "8 squares", "12 squares");
        Parameter p2 = new Parameter(PARAM_DIRT_PROB, "0.2 dirt probability", "0.5 dirt probability", "0.8 dirt probability");
        return Arrays.asList(p1, p2);
    }

    @Override
    public void initialize() {
        int numSquares;
        switch (taskPaneCtrl.getParamValueIndex(PARAM_NUM_SQUARES)) {
            case 0: numSquares = 4; break;
            case 1: numSquares = 8; break;
            case 2: numSquares = 12; break;
            default: numSquares = 8;
        }

        double dirtProb;
        switch (taskPaneCtrl.getParamValueIndex(PARAM_DIRT_PROB)) {
            case 0: dirtProb = 0.2; break;
            case 1: dirtProb = 0.5; break;
            case 2: dirtProb = 0.8; break;
            default: dirtProb = 0.5;
        }

        env = new GeneralizedVacuumEnvironment(numSquares, dirtProb);
        agent = new GeneralizedVacuumAgent();
        
        envViewCtrl.initialize(env);
        env.addEnvironmentListener(envViewCtrl);
        env.addAgent(agent);
    }

    public void startExperiment() {
        while (!env.isDone() && !Tasks.currIsCancelled()) {
            env.step();
            taskPaneCtrl.setStatus("Performance=" + env.getPerformanceMeasure(agent));
            taskPaneCtrl.waitAfterStep();
        }
        envViewCtrl.notify("Performance=" + env.getPerformanceMeasure(agent));
    }

    @Override
    public void cleanup() {
        taskPaneCtrl.cancelExecution();
    }
} 