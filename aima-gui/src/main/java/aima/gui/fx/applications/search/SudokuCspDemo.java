package aima.gui.fx.applications.search;

import aima.core.search.csp.*;
import aima.core.search.csp.examples.SudokuCSP;
import aima.core.search.csp.solver.CspListener;
import aima.core.search.csp.solver.CspSolver;
import aima.core.search.csp.solver.FlexibleBacktrackingSolver;
import aima.core.search.csp.solver.MinConflictsSolver;
import aima.gui.fx.applications.search.games.SimpleSudokuApp;

import java.util.Optional;

public class SudokuCspDemo {
    public static void main(String[] args) {
        // Example puzzle from SimpleSudokuApp
        String puzzle = SimpleSudokuApp.puzzle1;
        SudokuCSP csp = new SudokuCSP();
        
        // Initialize the puzzle
        for (int i = 0; i < puzzle.length(); i++) {
            char ch = puzzle.charAt(i);
            if (ch >= '1' && ch <= '9') {
                int row = i / 9;
                int col = i % 9;
                csp.setDigit(row, col, ch - '0');
            }
        }

        CspListener.StepCounter<Variable, Integer> stepCounter = new CspListener.StepCounter<>();
        CspSolver<Variable, Integer> solver;
        Optional<Assignment<Variable, Integer>> solution;

        System.out.println("Sudoku (Min-Conflicts)");
        solver = new MinConflictsSolver<>(1000);
        solver.addCspListener(stepCounter);
        stepCounter.reset();
        solution = solver.solve(csp);
        if (solution.isPresent())
            System.out.println((solution.get().isSolution(csp) ? ":-) " : ":-( ") + solution.get());
        System.out.println(stepCounter.getResults() + "\n");

        System.out.println("Sudoku (Backtracking + MRV & DEG + LCV + AC3)");
        solver = new FlexibleBacktrackingSolver<Variable, Integer>().setAll();
        solver.addCspListener(stepCounter);
        stepCounter.reset();
        solution = solver.solve(csp);
        if (solution.isPresent())
            System.out.println(solution.get());
        System.out.println(stepCounter.getResults() + "\n");

        System.out.println("Sudoku (Backtracking)");
        solver = new FlexibleBacktrackingSolver<>();
        solver.addCspListener(stepCounter);
        stepCounter.reset();
        solution = solver.solve(csp);
        if (solution.isPresent())
            System.out.println(solution.get());
        System.out.println(stepCounter.getResults() + "\n");
    }
} 