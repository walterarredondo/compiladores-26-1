package com.compiler.lexer;

import java.util.HashSet;
import java.util.Set;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaSimulator
 * ------------
 * This class provides functionality to simulate a Non-deterministic Finite Automaton (NFA)
 * on a given input string. It determines whether the input string is accepted by the NFA by processing
 * each character and tracking the set of possible states, including those reachable via epsilon (ε) transitions.
 *
 * Simulation steps:
 * - Initialize the set of current states with the ε-closure of the NFA's start state.
 * - For each character in the input, compute the next set of states by following transitions labeled with that character,
 *   and include all states reachable via ε-transitions from those states.
 * - After processing the input, check if any of the current states is a final (accepting) state.
 *
 * The class also provides a helper method to compute the ε-closure of a given state, which is the set of all states
 * reachable from the given state using only ε-transitions.
 */
/**
 * Simulator for running input strings on an NFA.
 */
public class NfaSimulator {
    /**
     * Default constructor for NfaSimulator.
     */
    public NfaSimulator() {
        // Constructor implementation - no special initialization needed
    }

    /**
     * Simulates the NFA on the given input string.
     * Starts at the NFA's start state and processes each character, following transitions and epsilon closures.
     * If any final state is reached after processing the input, the string is accepted.
     *
     * @param nfa The NFA to simulate.
     * @param input The input string to test.
     * @return True if the input is accepted by the NFA, false otherwise.
     */
    public boolean simulate(NFA nfa, String input) {
        // 1. Initialize currentStates with epsilon-closure of NFA start state
        Set<State> currentStates = new HashSet<>();
        addEpsilonClosure(nfa.startState, currentStates);
        
        // 2. For each character in input
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            Set<State> nextStates = new HashSet<>();
            
            // For each state in currentStates
            for (State state : currentStates) {
                // For each transition from this state
                for (State nextState : state.getTransitions(symbol)) {
                    // Add epsilon-closure of destination state to nextStates
                    addEpsilonClosure(nextState, nextStates);
                }
            }
            
            // Set currentStates to nextStates for next iteration
            currentStates = nextStates;
            
            // If no states reachable, string is rejected
            if (currentStates.isEmpty()) {
                return false;
            }
        }
        
        // 3. After input, if any state in currentStates is final, return true
        for (State state : currentStates) {
            if (state.isFinal) {
                return true;
            }
        }
        
        // 4. Otherwise, return false
        return false;
    }

    /**
     * Computes the epsilon-closure: all states reachable from 'start' using only epsilon (null) transitions.
     *
     * @param start The starting state.
     * @param closureSet The set to accumulate reachable states.
     */
    private void addEpsilonClosure(State start, Set<State> closureSet) {
        // If start not in closureSet, add it and explore its epsilon transitions
        if (!closureSet.contains(start)) {
            closureSet.add(start);
            
            // For each epsilon transition from start
            for (State epsilonState : start.getEpsilonTransitions()) {
                // Recursively add epsilon-closure of destination state
                addEpsilonClosure(epsilonState, closureSet);
            }
        }
    }
}
