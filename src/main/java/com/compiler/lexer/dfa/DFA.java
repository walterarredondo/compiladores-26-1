package com.compiler.lexer.dfa;

import java.util.List;

/**
 * DFA
 * ---
 * Represents a complete Deterministic Finite Automaton (DFA).
 * Contains the start state and a list of all states in the automaton.
 */
public class DFA {
    /**
     * The starting state of the DFA.
     */
    public final DfaState startState;

    /**
     * A list of all states in the DFA.
     */
    public final List<DfaState> allStates;
    // Optionally, a Set<Character> for the alphabet could be added.

    /**
     * Constructs a new DFA.
     * @param startState The starting state of the DFA.
     * @param allStates  A list of all states in the DFA.
     */
    public DFA(DfaState startState, List<DfaState> allStates) {
        this.startState = startState;
        this.allStates = allStates;
    }
}
