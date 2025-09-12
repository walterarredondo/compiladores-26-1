package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * Enhanced NFA to DFA converter that handles multiple lexical rules and token information.
 * Extends the basic conversion algorithm to track which states belong to which rules.
 */
public class EnhancedNfaToDfaConverter {
    
    /**
     * Information about an NFA and its associated lexical rule.
     */
    public static class RuleNfa {
        public final NFA nfa;
        public final LexicalRule rule;
        public final int ruleIndex;
        
        public RuleNfa(NFA nfa, LexicalRule rule, int ruleIndex) {
            this.nfa = nfa;
            this.rule = rule;
            this.ruleIndex = ruleIndex;
        }
    }
    
    /**
     * Converts multiple NFAs with associated rules into a single DFA.
     * @param ruleNfas List of NFAs with their associated lexical rules.
     * @param alphabet The input alphabet.
     * @return Combined DFA with token information.
     */
    public static DFA convertMultipleNfasToDfa(List<RuleNfa> ruleNfas, Set<Character> alphabet) {
        if (ruleNfas.isEmpty()) {
            return null;
        }
        
        // Create mapping from NFA states to rule information
        Map<State, RuleInfo> stateToRule = new HashMap<>();
        
        // Create combined start state
        Set<State> startStateSet = new HashSet<>();
        
        for (RuleNfa ruleNfa : ruleNfas) {
            startStateSet.add(ruleNfa.nfa.startState);
            mapStatesFromNfa(ruleNfa.nfa, ruleNfa.rule, ruleNfa.ruleIndex, stateToRule);
        }
        
        List<DfaState> dfaStates = new ArrayList<>();
        Queue<DfaState> unmarkedStates = new LinkedList<>();
        
        // Create initial DFA state from epsilon-closure of combined start states
        Set<State> startClosure = epsilonClosure(startStateSet);
        DfaState startDfaState = new DfaState(startClosure);
        
        // Set token information for start state if it's final
        setTokenInformationForDfaState(startDfaState, stateToRule);
        
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
                        setTokenInformationForDfaState(targetDfaState, stateToRule);
                        
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
     * Maps all states from an NFA to their rule information.
     */
    private static void mapStatesFromNfa(NFA nfa, LexicalRule rule, int ruleIndex, 
                                       Map<State, RuleInfo> stateToRule) {
        Set<State> visited = new HashSet<>();
        Stack<State> stack = new Stack<>();
        stack.push(nfa.startState);
        
        while (!stack.isEmpty()) {
            State current = stack.pop();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            
            // Map this state to its rule
            RuleInfo ruleInfo = new RuleInfo(rule, ruleIndex, current == nfa.endState);
            stateToRule.put(current, ruleInfo);
            
            // Add adjacent states to stack
            for (var transition : current.transitions) {
                if (transition.toState != null && !visited.contains(transition.toState)) {
                    stack.push(transition.toState);
                }
            }
        }
    }
    
    /**
     * Sets token information for a DFA state based on its constituent NFA states.
     */
    private static void setTokenInformationForDfaState(DfaState dfaState, 
                                                      Map<State, RuleInfo> stateToRule) {
        TokenType bestTokenType = null;
        int bestPriority = Integer.MAX_VALUE;
        boolean isFinal = false;
        
        for (State nfaState : dfaState.nfaStates) {
            RuleInfo ruleInfo = stateToRule.get(nfaState);
            if (ruleInfo != null && ruleInfo.isEndState && nfaState.isFinal) {
                isFinal = true;
                if (ruleInfo.rule.priority < bestPriority) {
                    bestTokenType = ruleInfo.rule.tokenType;
                    bestPriority = ruleInfo.rule.priority;
                }
            }
        }
        
        if (isFinal && bestTokenType != null) {
            dfaState.setTokenInfo(bestTokenType, bestPriority);
        }
    }
    
    /**
     * Computes the epsilon-closure of a set of NFA states.
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
     */
    private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
        for (DfaState dfaState : dfaStates) {
            if (dfaState.nfaStates.equals(targetNfaStates)) {
                return dfaState;
            }
        }
        return null;
    }
    
    /**
     * Helper class to store rule information for NFA states.
     */
    private static class RuleInfo {
        final LexicalRule rule;
        final int ruleIndex;
        final boolean isEndState;
        
        RuleInfo(LexicalRule rule, int ruleIndex, boolean isEndState) {
            this.rule = rule;
            this.ruleIndex = ruleIndex;
            this.isEndState = isEndState;
        }
    }
}
