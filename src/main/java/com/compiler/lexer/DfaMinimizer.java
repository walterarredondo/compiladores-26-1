package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * Utility class for minimizing DFAs using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
    public DfaMinimizer() {
        // Constructor implementation - no special initialization needed
    }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
        List<DfaState> allStates = new ArrayList<>(originalDfa.allStates);
        allStates.sort((s1, s2) -> Integer.compare(s1.id, s2.id));
        
        Map<Pair, Boolean> table = new HashMap<>();
        
        // Mark pairs as distinguishable if one is final and the other is not
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                
                if (s1.isFinal() != s2.isFinal()) {
                    table.put(pair, true);
                } else {
                    table.put(pair, false);
                }
            }
        }
        
        // Iteratively mark pairs as distinguishable
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (int i = 0; i < allStates.size(); i++) {
                for (int j = i + 1; j < allStates.size(); j++) {
                    DfaState s1 = allStates.get(i);
                    DfaState s2 = allStates.get(j);
                    Pair currentPair = new Pair(s1, s2);
                    
                    if (table.get(currentPair)) {
                        continue;
                    }
                    
                    for (Character symbol : alphabet) {
                        DfaState t1 = s1.getTransition(symbol);
                        DfaState t2 = s2.getTransition(symbol);
                        
                        if ((t1 == null) != (t2 == null)) {
                            table.put(currentPair, true);
                            changed = true;
                            break;
                        }
                        
                        if (t1 != null && t2 != null && t1 != t2) {
                            Pair targetPair = new Pair(t1, t2);
                            if (table.getOrDefault(targetPair, false)) {
                                table.put(currentPair, true);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Create partitions of equivalent states
        List<Set<DfaState>> partitions = createPartitions(allStates, table);
        
        // Create new minimized states
        Map<Set<DfaState>, DfaState> partitionToNewState = new HashMap<>();
        List<DfaState> minimizedStates = new ArrayList<>();
        
        for (Set<DfaState> partition : partitions) {
            DfaState representative = partition.iterator().next();
            DfaState newState = new DfaState(representative.nfaStates);
            
            for (DfaState state : partition) {
                if (state.isFinal()) {
                    newState.setFinal(true);
                    break;
                }
            }
            
            partitionToNewState.put(partition, newState);
            minimizedStates.add(newState);
        }
        
        // Reconstruct transitions
        for (Set<DfaState> partition : partitions) {
            DfaState newState = partitionToNewState.get(partition);
            DfaState representative = partition.iterator().next();
            
            for (Character symbol : alphabet) {
                DfaState targetState = representative.getTransition(symbol);
                if (targetState != null) {
                    for (Set<DfaState> targetPartition : partitions) {
                        if (targetPartition.contains(targetState)) {
                            newState.addTransition(symbol, partitionToNewState.get(targetPartition));
                            break;
                        }
                    }
                }
            }
        }
        
        // Find start state
        DfaState minimizedStartState = null;
        for (Set<DfaState> partition : partitions) {
            if (partition.contains(originalDfa.startState)) {
                minimizedStartState = partitionToNewState.get(partition);
                break;
            }
        }
        
        return new DFA(minimizedStartState, minimizedStates);
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        Map<DfaState, DfaState> parent = new HashMap<>();
        
        for (DfaState state : allStates) {
            parent.put(state, state);
        }
        
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                
                if (!table.get(pair)) {
                    union(parent, s1, s2);
                }
            }
        }
        
        Map<DfaState, Set<DfaState>> groups = new HashMap<>();
        for (DfaState state : allStates) {
            DfaState root = find(parent, state);
            groups.computeIfAbsent(root, k -> new HashSet<>()).add(state);
        }
        
        return new ArrayList<>(groups.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
        if (parent.get(state) != state) {
            parent.put(state, find(parent, parent.get(state)));
        }
        return parent.get(state);
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {
        DfaState root1 = find(parent, s1);
        DfaState root2 = find(parent, s2);
        
        if (root1 != root2) {
            parent.put(root2, root1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param s1 First state.
         * @param s2 Second state.
         */
        public Pair(DfaState s1, DfaState s2) {
            if (s1.id <= s2.id) {
                this.s1 = s1;
                this.s2 = s2;
            } else {
                this.s1 = s2;
                this.s2 = s1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return s1.id == pair.s1.id && s2.id == pair.s2.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(s1.id, s2.id);
        }
    }
}
