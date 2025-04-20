package aima.gui.fx.applications.search;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import aima.core.search.csp.*;
import aima.core.search.csp.examples.SudokuCSP;
import aima.core.search.csp.solver.inference.AC3Strategy;
import aima.core.search.csp.solver.inference.ForwardCheckingStrategy;
import aima.core.search.csp.solver.*;
import aima.gui.fx.applications.search.games.SimpleSudokuApp;
import aima.gui.fx.framework.IntegrableApplication;
import aima.gui.fx.framework.Parameter;
import aima.gui.fx.framework.TaskExecutionPaneBuilder;
import aima.gui.fx.framework.TaskExecutionPaneCtrl;
import aima.gui.fx.views.SudokuViewCtrl;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class SudokuCspApp extends IntegrableApplication {

    public static void main(String[] args) {
        launch(args);
    }

    private final static String PARAM_STRATEGY = "strategy";
    private final static String PARAM_VAR_SELECT = "varSelect";
    private final static String PARAM_VAL_SELECT = "valOrder";
    private final static String PARAM_INFERENCE = "inference";
    private final static String PARAM_PUZZLE = "puzzle";

    private SudokuViewCtrl stateViewCtrl;
    private TaskExecutionPaneCtrl taskPaneCtrl;
    private CSP<Variable, Integer> csp;
    private CspSolver<Variable, Integer> solver;
    private final CspListener.StepCounter<Variable, Integer> stepCounter = new CspListener.StepCounter<>();

    @Override
    public String getTitle() {
        return "Sudoku CSP App";
    }

    @Override
    public Pane createRootPane() {
        BorderPane root = new BorderPane();

        StackPane stateView = new StackPane();
        stateViewCtrl = new SudokuViewCtrl(stateView);

        List<Parameter> params = createParameters();

        TaskExecutionPaneBuilder builder = new TaskExecutionPaneBuilder();
        builder.defineParameters(params);
        builder.defineStateView(stateView);
        builder.defineInitMethod(this::initialize);
        builder.defineTaskMethod(this::startExperiment);
        taskPaneCtrl = builder.getResultFor(root);
        taskPaneCtrl.setParam(TaskExecutionPaneCtrl.PARAM_EXEC_SPEED, 0);

        return root;
    }

    protected List<Parameter> createParameters() {
        Parameter p1 = new Parameter(PARAM_STRATEGY, "Backtracking", "Backjumping", "Min-Conflicts");
        Parameter p2 = new Parameter(PARAM_VAR_SELECT, "Default", "MRV", "DEG", "MRV&DEG");
        Parameter p3 = new Parameter(PARAM_VAL_SELECT, "Default", "LCV");
        Parameter p4 = new Parameter(PARAM_INFERENCE, "None", "Forward Checking", "AC3");
        p2.setDependency(PARAM_STRATEGY, "Backtracking", "Backjumping");
        p3.setDependency(PARAM_STRATEGY, "Backtracking", "Backjumping");
        p4.setDependency(PARAM_STRATEGY, "Backtracking");
        Parameter p5 = new Parameter(PARAM_PUZZLE, "Puzzle 1", "Puzzle 2", "Puzzle 3");
        return Arrays.asList(p1, p2, p3, p4, p5);
    }

    @Override
    public void initialize() {
        csp = new SudokuCSP();
        String puzzle = (String) taskPaneCtrl.getParamValue(PARAM_PUZZLE);
        String puzzleStr = puzzle.equals("Puzzle 1") ? SimpleSudokuApp.puzzle1 :
                          puzzle.equals("Puzzle 2") ? SimpleSudokuApp.puzzle2 :
                          SimpleSudokuApp.puzzle3;

        // Initialize the puzzle
        for (int i = 0; i < puzzleStr.length(); i++) {
            char ch = puzzleStr.charAt(i);
            if (ch >= '1' && ch <= '9') {
                int row = i / 9;
                int col = i % 9;
                ((SudokuCSP) csp).setDigit(row, col, ch - '0');
            }
        }

        Object strategy = taskPaneCtrl.getParamValue(PARAM_STRATEGY);
        if (strategy.equals("Backtracking")) {
            FlexibleBacktrackingSolver<Variable, Integer> bSolver = new FlexibleBacktrackingSolver<>();
            switch ((String) taskPaneCtrl.getParamValue(PARAM_VAR_SELECT)) {
                case "MRV":
                    bSolver.set(CspHeuristics.mrv());
                    break;
                case "DEG":
                    bSolver.set(CspHeuristics.deg());
                    break;
                case "MRV&DEG":
                    bSolver.set(CspHeuristics.mrvDeg());
                    break;
            }
			if (taskPaneCtrl.getParamValue(PARAM_VAL_SELECT).equals("LCV")) {
				bSolver.set(CspHeuristics.lcv());
			}
            switch ((String) taskPaneCtrl.getParamValue(PARAM_INFERENCE)) {
                case "Forward Checking":
                    bSolver.set(new ForwardCheckingStrategy<>());
                    break;
                case "AC3":
                    bSolver.set(new AC3Strategy<>());
                    break;
            }
            solver = bSolver;
        } else if (strategy.equals("Backjumping")) {
            BackjumpingBacktrackingSolver<Variable, Integer> bSolver = new BackjumpingBacktrackingSolver<>();
            switch ((String) taskPaneCtrl.getParamValue(PARAM_VAR_SELECT)) {
                case "MRV":
                    bSolver.set(CspHeuristics.mrv());
                    break;
                case "DEG":
                    bSolver.set(CspHeuristics.deg());
                    break;
                case "MRV&DEG":
                    bSolver.set(CspHeuristics.mrvDeg());
                    break;
            }
			if (taskPaneCtrl.getParamValue(PARAM_VAL_SELECT).equals("LCV")) {
				bSolver.set(CspHeuristics.lcv());
			}
            solver = bSolver;
        } else if (strategy.equals("Min-Conflicts")) {
            solver = new MinConflictsSolver<>(1000);
        }

        solver.addCspListener(stepCounter);
        solver.addCspListener((csp, assign, var) -> {
            if (assign != null) updateStateView(assign);
        });
        stepCounter.reset();
        stateViewCtrl.clear(true);
        for (int i = 0; i < puzzleStr.length(); i++) {
            char ch = puzzleStr.charAt(i);
            if (ch >= '1' && ch <= '9') {
                int row = i / 9 + 1;
                int col = i % 9 + 1;
                stateViewCtrl.fixDigit(col, row, ch - '0');
            }
        }
        taskPaneCtrl.setStatus("");
    }

    @Override
    public void cleanup() {
        taskPaneCtrl.cancelExecution();
    }

    public void startExperiment() {
        Optional<Assignment<Variable, Integer>> solution = solver.solve(csp);
		solution.ifPresent(this::updateStateView);
    }

    private void updateStateView(Assignment<Variable, Integer> assignment) {
        Platform.runLater(() -> {
            for (Variable var : assignment.getVariables()) {
                int index = Integer.parseInt(var.getName().substring(1)) - 1;
                int row = index / 9 + 1;
                int col = index % 9 + 1;
                int currentDigit = stateViewCtrl.getDigit(col, row);
                int newDigit = assignment.getValue(var);
                
                // Highlight the cell if the digit is changing
                if (currentDigit != newDigit) {
                    stateViewCtrl.setRedHighlight(col, row, true);
                    stateViewCtrl.setDigit(col, row, newDigit);
                    // Reset the highlight after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(300);
                            Platform.runLater(() -> stateViewCtrl.setRedHighlight(col, row, false));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    stateViewCtrl.setDigit(col, row, newDigit);
                }
            }
            taskPaneCtrl.setStatus(stepCounter.getResults().toString());
        });
        taskPaneCtrl.waitAfterStep();
    }
} 