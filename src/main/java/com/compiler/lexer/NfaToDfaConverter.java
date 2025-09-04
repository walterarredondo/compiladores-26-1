package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * Utility class for converting NFAs to DFAs using the subset construction algorithm.
 */
public class NfaToDfaConverter {
    /**
     * Default constructor for NfaToDfaConverter.
     */
    public NfaToDfaConverter() {
        // Constructor implementation - no special initialization needed
    }

    /**
     * Converts an NFA to a DFA using the subset construction algorithm.
     * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
     *
     * @param nfa The input NFA
     * @param alphabet The input alphabet (set of characters)
     * @return The resulting DFA
     */
    public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
        // Auto-mark endState as final if not already marked
        if (nfa.endState != null && !nfa.endState.isFinal) {
            boolean hasOtherFinalStates = hasFinalStates(nfa);
            if (!hasOtherFinalStates) {
                nfa.endState.isFinal = true;
            }
        }
        
        List<DfaState> dfaStates = new ArrayList<>();
        Queue<DfaState> unmarkedStates = new LinkedList<>();
        
        // Create initial DFA state from epsilon-closure of NFA start state
        Set<State> startStateSet = new HashSet<>();
        startStateSet.add(nfa.startState);
        Set<State> startClosure = epsilonClosure(startStateSet);
        
        DfaState startDfaState = new DfaState(startClosure);
        
        // Check if start state is final
        for (State s : startClosure) {
            if (s.isFinal) {
                startDfaState.setFinal(true);
                break;
            }
        }
        
        dfaStates.add(startDfaState);
        unmarkedStates.add(startDfaState);
        
        // Process unmarked states
        while (!unmarkedStates.isEmpty()) {
            DfaState currentDfaState = unmarkedStates.poll();
            
            for (Character symbol : alphabet) {
                Set<State> moveResult = move(currentDfaState.nfaStates, symbol);
                Set<State> targetStateSet = epsilonClosure(moveResult);
                
                if (!targetStateSet.isEmpty()) {
                    DfaState targetDfaState = findDfaState(dfaStates, targetStateSet);
                    
                    if (targetDfaState == null) {
                        targetDfaState = new DfaState(targetStateSet);
                        
                        for (State s : targetStateSet) {
                            if (s.isFinal) {
                                targetDfaState.setFinal(true);
                                break;
                            }
                        }
                        
                        dfaStates.add(targetDfaState);
                        unmarkedStates.add(targetDfaState);
                    }
                    
                    currentDfaState.addTransition(symbol, targetDfaState);
                }
            }
        }
        
        return new DFA(startDfaState, dfaStates);
    }

    /**
     * Checks if an NFA has any states marked as final.
     *
     * @param nfa The NFA to check
     * @return true if the NFA has at least one final state, false otherwise
     */
    private static boolean hasFinalStates(NFA nfa) {
        Set<State> visited = new HashSet<>();
        Stack<State> stack = new Stack<>();
        stack.push(nfa.startState);
        
        while (!stack.isEmpty()) {
            State current = stack.pop();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            
            if (current.isFinal) {
                return true;
            }
            
            for (var transition : current.transitions) {
                if (transition.toState != null && !visited.contains(transition.toState)) {
                    stack.push(transition.toState);
                }
            }
        }
        
        return false;
    }

    /**
     * Computes the epsilon-closure of a set of NFA states.
     * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
     *
     * @param states The set of NFA states.
     * @return The epsilon-closure of the input states.
     */
    private static Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        
        for (State state : states) {
            stack.push(state);
        }
        
        while (!stack.isEmpty()) {
            State current = stack.pop();
            List<State> epsilonStates = current.getEpsilonTransitions();
            
            for (State epsilonState : epsilonStates) {
                if (!closure.contains(epsilonState)) {
                    closure.add(epsilonState);
                    stack.push(epsilonState);
                }
            }
        }
        
        return closure;
    }

    /**
     * Returns the set of states reachable from a set of NFA states by a given symbol.
     *
     * @param states The set of NFA states.
     * @param symbol The input symbol.
     * @return The set of reachable states.
     */
    private static Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        
        for (State state : states) {
            List<State> transitions = state.getTransitions(symbol);
            result.addAll(transitions);
        }
        
        return result;
    }

    /**
     * Finds an existing DFA state representing a given set of NFA states.
     *
     * @param dfaStates The list of DFA states.
     * @param targetNfaStates The set of NFA states to search for.
     * @return The matching DFA state, or null if not found.
     */
    private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
        for (DfaState dfaState : dfaStates) {
            if (dfaState.nfaStates.equals(targetNfaStates)) {
                return dfaState;
            }
        }
        return null;
    }
}
