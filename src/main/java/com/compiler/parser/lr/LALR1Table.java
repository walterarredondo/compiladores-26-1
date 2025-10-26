package com.compiler.parser.lr;

/**
 * Builds the LALR(1) parsing table (ACTION/GOTO).
 * Main task for Practice 9.
 */
public class LALR1Table {
    private final LR1Automaton automaton;

    // merged LALR states and transitions
    private java.util.List<java.util.Set<LR1Item>> lalrStates = new java.util.ArrayList<>();
    private java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lalrTransitions = new java.util.HashMap<>();
    
    // ACTION table: state -> terminal -> Action
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT }
        public final Type type;
        public final Integer state; // for SHIFT
        public final com.compiler.parser.grammar.Production reduceProd; // for REDUCE

        private Action(Type type, Integer state, com.compiler.parser.grammar.Production prod) {
            this.type = type; this.state = state; this.reduceProd = prod;
        }

        public static Action shift(int s) { return new Action(Type.SHIFT, s, null); }
        public static Action reduce(com.compiler.parser.grammar.Production p) { return new Action(Type.REDUCE, null, p); }
        public static Action accept() { return new Action(Type.ACCEPT, null, null); }
    }

    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> action = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> gotoTable = new java.util.HashMap<>();
    private final java.util.List<String> conflicts = new java.util.ArrayList<>();
    private int initialState = 0;

    public LALR1Table(LR1Automaton automaton) {
        this.automaton = automaton;
    }

    /**
     * Builds the LALR(1) parsing table.
     */
    public void build() {
        // Step 1: Ensure the underlying LR(1) automaton is built
        automaton.build();

        // Step 2: Merge LR(1) states to create LALR(1) states
        java.util.List<java.util.Set<LR1Item>> lr1States = automaton.getStates();

        // Step 2a: Group LR(1) states by their kernel
        java.util.Map<java.util.Set<KernelEntry>, java.util.List<Integer>> kernelToStates = new java.util.HashMap<>();

        for (int i = 0; i < lr1States.size(); i++) {
            java.util.Set<LR1Item> state = lr1States.get(i);
            java.util.Set<KernelEntry> kernel = new java.util.HashSet<>();

            for (LR1Item item : state) {
                kernel.add(new KernelEntry(item.production, item.dotPosition));
            }

            kernelToStates.computeIfAbsent(kernel, k -> new java.util.ArrayList<>()).add(i);
        }

        // Step 2b & 2c: Merge states with same kernel and create mapping
        java.util.Map<Integer, Integer> lr1ToLalr = new java.util.HashMap<>();

        for (java.util.List<Integer> stateGroup : kernelToStates.values()) {
            // Merge all LR(1) items from states in the group
            java.util.Map<KernelEntry, java.util.Set<com.compiler.parser.grammar.Symbol>> kernelToLookaheads = new java.util.HashMap<>();

            for (int stateId : stateGroup) {
                java.util.Set<LR1Item> state = lr1States.get(stateId);

                for (LR1Item item : state) {
                    KernelEntry kernel = new KernelEntry(item.production, item.dotPosition);
                    kernelToLookaheads.computeIfAbsent(kernel, k -> new java.util.HashSet<>())
                                      .add(item.lookahead);
                }
            }

            // Create merged LALR(1) state
            java.util.Set<LR1Item> lalrState = new java.util.HashSet<>();
            for (java.util.Map.Entry<KernelEntry, java.util.Set<com.compiler.parser.grammar.Symbol>> entry : kernelToLookaheads.entrySet()) {
                for (com.compiler.parser.grammar.Symbol lookahead : entry.getValue()) {
                    lalrState.add(new LR1Item(entry.getKey().production, entry.getKey().dotPosition, lookahead));
                }
            }

            int lalrStateId = lalrStates.size();
            lalrStates.add(lalrState);

            // Map all LR(1) states in group to this LALR(1) state
            for (int stateId : stateGroup) {
                lr1ToLalr.put(stateId, lalrStateId);
            }
        }

        // Set the initial state (LR1 state 0 maps to which LALR state)
        initialState = lr1ToLalr.get(0);

        // Step 3: Build LALR(1) transitions
        java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lr1Transitions = automaton.getTransitions();

        for (java.util.Map.Entry<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> entry : lr1Transitions.entrySet()) {
            int lr1From = entry.getKey();
            int lalrFrom = lr1ToLalr.get(lr1From);

            for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> trans : entry.getValue().entrySet()) {
                com.compiler.parser.grammar.Symbol symbol = trans.getKey();
                int lr1To = trans.getValue();
                int lalrTo = lr1ToLalr.get(lr1To);

                lalrTransitions.computeIfAbsent(lalrFrom, k -> new java.util.HashMap<>())
                              .put(symbol, lalrTo);
            }
        }

        // Step 4: Fill ACTION and GOTO tables
        fillActionGoto();
    }

    private void fillActionGoto() {
        // 1. Clear tables and conflicts
        action.clear();
        gotoTable.clear();
        conflicts.clear();

        String augmentedLeftName = automaton.getAugmentedLeftName();

        // 2. Iterate through each LALR state
        for (int s = 0; s < lalrStates.size(); s++) {
            java.util.Set<LR1Item> state = lalrStates.get(s);

            // 3. For each item in the state
            for (LR1Item item : state) {
                com.compiler.parser.grammar.Symbol X = item.getSymbolAfterDot();

                if (X != null) {
                    // Symbol after dot exists - could be SHIFT action
                    if (X.isTerminal()) {
                        // SHIFT action
                        java.util.Map<com.compiler.parser.grammar.Symbol, Integer> trans = lalrTransitions.get(s);
                        if (trans != null && trans.containsKey(X)) {
                            int t = trans.get(X);

                            // Check for conflicts
                            Action existing = action.computeIfAbsent(s, k -> new java.util.HashMap<>()).get(X);
                            if (existing != null) {
                                conflicts.add("Shift/Reduce or Shift/Shift conflict in state " + s + " on symbol " + X.name);
                            } else {
                                action.computeIfAbsent(s, k -> new java.util.HashMap<>()).put(X, Action.shift(t));
                            }
                        }
                    }
                } else {
                    // Dot is at the end - REDUCE or ACCEPT action
                    // Check if this is the augmented start production
                    String leftName = item.production.getLeftSide().name;

                    if (leftName.equals(augmentedLeftName) && item.lookahead.isEndOfInput()) {
                        // ACCEPT action
                        action.computeIfAbsent(s, k -> new java.util.HashMap<>())
                              .put(item.lookahead, Action.accept());
                    } else {
                        // REDUCE action
                        Action existing = action.computeIfAbsent(s, k -> new java.util.HashMap<>()).get(item.lookahead);
                        if (existing != null) {
                            if (existing.type == Action.Type.SHIFT) {
                                conflicts.add("Shift/Reduce conflict in state " + s + " on symbol " + item.lookahead.name);
                            } else if (existing.type == Action.Type.REDUCE) {
                                conflicts.add("Reduce/Reduce conflict in state " + s + " on symbol " + item.lookahead.name);
                            }
                        } else {
                            action.computeIfAbsent(s, k -> new java.util.HashMap<>())
                                  .put(item.lookahead, Action.reduce(item.production));
                        }
                    }
                }
            }
        }

        // 4. Populate GOTO table
        for (java.util.Map.Entry<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> entry : lalrTransitions.entrySet()) {
            int s = entry.getKey();

            for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> trans : entry.getValue().entrySet()) {
                com.compiler.parser.grammar.Symbol symbol = trans.getKey();
                int t = trans.getValue();

                if (symbol.isNonTerminal()) {
                    gotoTable.computeIfAbsent(s, k -> new java.util.HashMap<>()).put(symbol, t);
                }
            }
        }
    }
    
    // ... (Getters and KernelEntry class can remain as is)
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> getActionTable() { return action; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getGotoTable() { return gotoTable; }
    public java.util.List<String> getConflicts() { return conflicts; }
    private static class KernelEntry {
        public final com.compiler.parser.grammar.Production production;
        public final int dotPosition;
        KernelEntry(com.compiler.parser.grammar.Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof KernelEntry)) return false;
            KernelEntry o = (KernelEntry) obj;
            return dotPosition == o.dotPosition && production.equals(o.production);
        }
        @Override
        public int hashCode() {
            int r = production.hashCode();
            r = 31 * r + dotPosition;
            return r;
        }
    }
    public java.util.List<java.util.Set<LR1Item>> getLALRStates() { return lalrStates; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getLALRTransitions() { return lalrTransitions; }
    public int getInitialState() { return initialState; }
}
