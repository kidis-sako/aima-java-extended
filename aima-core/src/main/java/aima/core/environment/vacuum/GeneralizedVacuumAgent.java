package aima.core.environment.vacuum;

import aima.core.agent.Action;
import aima.core.agent.Model;
import aima.core.agent.impl.SimpleAgent;
import aima.core.agent.impl.DynamicAction;
import aima.core.agent.impl.DynamicState;
import aima.core.agent.impl.aprog.ModelBasedReflexAgentProgram;
import aima.core.agent.impl.aprog.simplerule.ANDCondition;
import aima.core.agent.impl.aprog.simplerule.EQUALCondition;
import aima.core.agent.impl.aprog.simplerule.Rule;
import static aima.core.environment.vacuum.GeneralizedVacuumEnvironment.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A model-based reflex agent for the generalized vacuum environment.
 * This agent is optimized for environments with approximately 50% dirt probability.
 * It uses a systematic approach to clean all squares by:
 * 1. Moving right until it reaches the end
 * 2. Moving left until it reaches the start
 * 3. Repeating this pattern while cleaning any dirty squares it encounters
 */
public class GeneralizedVacuumAgent extends SimpleAgent<VacuumPercept, Action> {
    
    private static final String ATT_CURRENT_LOCATION = "currentLocation";
    private static final String ATT_CURRENT_STATE = "currentState";
    private static final String ATT_LAST_ACTION = "lastAction";
    private static final String ATT_MOVING_RIGHT = "movingRight";
    private static final String ATT_LAST_LOCATION = "lastLocation";
    private static final String ATT_AT_END = "atEnd";
    
    public GeneralizedVacuumAgent() {
        super(new ModelBasedReflexAgentProgram<VacuumPercept, Action>() {
            @Override
            protected void init() {
                DynamicState state = new DynamicState();
                state.setAttribute(ATT_MOVING_RIGHT, true);
                state.setAttribute(ATT_AT_END, false);
                setState(state);
                setRules(getRuleSet());
            }
            
            @Override
            protected DynamicState updateState(DynamicState state, Action anAction, VacuumPercept percept, Model model) {
                String lastLoc = (String) state.getAttribute(ATT_CURRENT_LOCATION);
                if (lastLoc != null) {
                    state.setAttribute(ATT_LAST_LOCATION, lastLoc);
                }
                
                String currLoc = percept.getCurrLocation();
                state.setAttribute(ATT_CURRENT_LOCATION, currLoc);
                state.setAttribute(ATT_CURRENT_STATE, percept.getCurrState());
                
                Boolean canMoveRight = (Boolean) percept.getAttribute("canMoveRight");
                Boolean canMoveLeft = (Boolean) percept.getAttribute("canMoveLeft");
                state.setAttribute("canMoveRight", canMoveRight);
                state.setAttribute("canMoveLeft", canMoveLeft);
                
                state.setAttribute(ATT_AT_END, !canMoveRight && !canMoveLeft);
                
                if (anAction != null) {
                    state.setAttribute(ATT_LAST_ACTION, anAction);
                    
                    // Update movement state
                    if (anAction.equals(ACTION_MOVE_RIGHT) && !canMoveRight) {
                        state.setAttribute(ATT_MOVING_RIGHT, false);
                    } else if (anAction.equals(ACTION_MOVE_LEFT) && !canMoveLeft) {
                        state.setAttribute(ATT_MOVING_RIGHT, true);
                    }
                }
                
                return state;
            }
        });
    }
    
    private static Set<Rule<Action>> getRuleSet() {
        Set<Rule<Action>> rules = new LinkedHashSet<>();
        
        // Rule 1: If current square is dirty, suck
        rules.add(new Rule<>(new EQUALCondition(ATT_CURRENT_STATE, VacuumEnvironment.LocationState.Dirty), ACTION_SUCK));
        
        // Rule 2: If moving right and can move right, move right
        rules.add(new Rule<>(new ANDCondition(
                new EQUALCondition(ATT_MOVING_RIGHT, true),
                new EQUALCondition("canMoveRight", true)), ACTION_MOVE_RIGHT));
        
        // Rule 3: If moving left and can move left, move left
        rules.add(new Rule<>(new ANDCondition(
                new EQUALCondition(ATT_MOVING_RIGHT, false),
                new EQUALCondition("canMoveLeft", true)), ACTION_MOVE_LEFT));
        
        // Rule 4: If can't move in either direction, do nothing
        rules.add(new Rule<>(new EQUALCondition(ATT_AT_END, true), new DynamicAction("NoOp")));
        
        return rules;
    }
}