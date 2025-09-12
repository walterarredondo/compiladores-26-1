package com.compiler.lexer.dfa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.nfa.State;
import com.compiler.lexer.TokenType;

/**
 * DfaState
 * --------
 * Represents a single state in a Deterministic Finite Automaton (DFA).
 * Each DFA state corresponds to a set of states from the original NFA.
 * Provides methods for managing transitions, checking finality, and equality based on NFA state sets.
 * Extended to support token information for lexical analysis.
 */
public class DfaState {
    private static int nextId = 0;
    /**
     * Unique identifier for this DFA state.
     */
    public final int id;
    /**
     * The set of NFA states this DFA state represents.
     */
    public final Set<State> nfaStates;
    /**
     * Indicates whether this DFA state is a final (accepting) state.
     */
    public boolean isFinal;
    /**
     * Map of input symbols to destination DFA states (transitions).
     */
    public final Map<Character, DfaState> transitions;
    
    /**
     * The token type associated with this state if it's a final state.
     * Used for lexical analysis to determine what token to generate.
     */
    public TokenType tokenType;
    
    /**
     * The priority of the token type for this state.
     * Lower values indicate higher priority.
     */
    public int tokenPriority;

    /**
     * Constructs a new DFA state.
     * @param nfaStates The set of NFA states that this DFA state represents.
     */
    public DfaState(Set<State> nfaStates) {
        this.id = nextId++;
        this.nfaStates = nfaStates;
        this.isFinal = false;
        this.transitions = new HashMap<>();
        this.tokenType = null;
        this.tokenPriority = Integer.MAX_VALUE;
    }

    /**
     * Returns all transitions from this state.
     * @return Map of input symbols to destination DFA states.
     */
    public Map<Character, DfaState> getTransitions() {
        return transitions;
    }

    /**
     * Adds a transition from this state to another on a given symbol.
     * @param symbol The input symbol for the transition.
     * @param toState The destination DFA state.
     */
    public void addTransition(Character symbol, DfaState toState) {
        transitions.put(symbol, toState);
    }

    /**
     * Two DfaStates are considered equal if they represent the same set of NFA states.
     * @param obj The object to compare.
     * @return True if the states are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DfaState dfaState = (DfaState) obj;
        return nfaStates.equals(dfaState.nfaStates);
    }

    /**
     * The hash code is based on the set of NFA states.
     * @return The hash code for this DFA state.
     */
    @Override
    public int hashCode() {
        return nfaStates.hashCode();
    }
    
    /**
     * Returns a string representation of the DFA state, including its id and finality.
     * @return String representation of the state.
     */
    @Override
    public String toString() {
        String tokenInfo = (tokenType != null) ? ", tokenType=" + tokenType : "";
        return "DfaState{id=" + id + ", isFinal=" + isFinal + ", nfaStates=" + nfaStates.size() + tokenInfo + "}";
    }

    /**
     * Sets the finality of the DFA state.
     * @param isFinal True if this state is a final state, false otherwise.
     */
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * Checks if the DFA state is final.
     * @return True if this state is a final state, false otherwise.
     */
    public boolean isFinal() {
        return this.isFinal;
    }

    /**
     * Gets the transition for a given input symbol.
     * @param symbol The input symbol for the transition.
     * @return The destination DFA state for the transition, or null if there is no transition for the given symbol.
     */
    public DfaState getTransition(char symbol) {
        return transitions.get(symbol);
    }

    /**
     * Returns the set of NFA states this DFA state represents.
     * @return The set of NFA states.
     */
    public Set<State> getName() {
        return nfaStates;
    }
    
    /**
     * Sets the token type and priority for this final state.
     * @param tokenType The token type to associate with this state.
     * @param priority The priority of this token type.
     */
    public void setTokenInfo(TokenType tokenType, int priority) {
        this.tokenType = tokenType;
        this.tokenPriority = priority;
        if (tokenType != null) {
            this.isFinal = true;
        }
    }
    
    /**
     * Gets the token type associated with this state.
     * @return The token type, or null if not a final state.
     */
    public TokenType getTokenType() {
        return tokenType;
    }
    
    /**
     * Gets the token priority for this state.
     * @return The token priority.
     */
    public int getTokenPriority() {
        return tokenPriority;
    }
}
