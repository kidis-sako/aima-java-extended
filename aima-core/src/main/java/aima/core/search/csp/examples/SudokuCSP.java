package aima.core.search.csp.examples;

import aima.core.search.csp.CSP;
import aima.core.search.csp.Domain;
import aima.core.search.csp.Variable;

import java.util.ArrayList;
import java.util.List;

public class SudokuCSP extends CSP<Variable, Integer> {
    private static final int SIZE = 9;
    private static final int BOX_SIZE = 3;

    public SudokuCSP() {
        // Create variables for each cell
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                addVariable(new Variable("C" + (row * SIZE + col + 1)));
            }
        }

        // Create domain (1-9) for each variable
        List<Integer> values = new ArrayList<>();
        for (int val = 1; val <= SIZE; val++) {
            values.add(val);
        }
        Domain<Integer> digits = new Domain<>(values);

        for (Variable var : getVariables()) {
            setDomain(var, digits);
        }

        // Add row constraints
        for (int row = 0; row < SIZE; row++) {
            for (int col1 = 0; col1 < SIZE; col1++) {
                Variable var1 = getVariables().get(row * SIZE + col1);
                for (int col2 = col1 + 1; col2 < SIZE; col2++) {
                    Variable var2 = getVariables().get(row * SIZE + col2);
                    addConstraint(new NotEqualConstraint<>(var1, var2));
                }
            }
        }

        // Add column constraints
        for (int col = 0; col < SIZE; col++) {
            for (int row1 = 0; row1 < SIZE; row1++) {
                Variable var1 = getVariables().get(row1 * SIZE + col);
                for (int row2 = row1 + 1; row2 < SIZE; row2++) {
                    Variable var2 = getVariables().get(row2 * SIZE + col);
                    addConstraint(new NotEqualConstraint<>(var1, var2));
                }
            }
        }

        // Add box constraints
        for (int boxRow = 0; boxRow < BOX_SIZE; boxRow++) {
            for (int boxCol = 0; boxCol < BOX_SIZE; boxCol++) {
                for (int pos1 = 0; pos1 < SIZE; pos1++) {
                    int row1 = boxRow * BOX_SIZE + pos1 / BOX_SIZE;
                    int col1 = boxCol * BOX_SIZE + pos1 % BOX_SIZE;
                    Variable var1 = getVariables().get(row1 * SIZE + col1);
                    for (int pos2 = pos1 + 1; pos2 < SIZE; pos2++) {
                        int row2 = boxRow * BOX_SIZE + pos2 / BOX_SIZE;
                        int col2 = boxCol * BOX_SIZE + pos2 % BOX_SIZE;
                        Variable var2 = getVariables().get(row2 * SIZE + col2);
                        addConstraint(new NotEqualConstraint<>(var1, var2));
                    }
                }
            }
        }
    }

    public void setDigit(int row, int col, int digit) {
        Variable var = getVariables().get(row * SIZE + col);
        Domain<Integer> fixedDomain = new Domain<>(digit);
        setDomain(var, fixedDomain);
    }
} 