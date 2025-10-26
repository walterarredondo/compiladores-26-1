package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.syntax.StaticAnalyzer;

/**
 * Builds the canonical collection of LR(1) items (the DFA automaton).
 * Items contain a lookahead symbol.
 */
public class LR1Automaton {
    private final Grammar grammar;
    private final List<Set<LR1Item>> states = new ArrayList<>();
    private final Map<Integer, Map<Symbol, Integer>> transitions = new HashMap<>();
    private String augmentedLeftName = null;

    public LR1Automaton(Grammar grammar) {
        this.grammar = Objects.requireNonNull(grammar);
    }

    public List<Set<LR1Item>> getStates() { return states; }
    public Map<Integer, Map<Symbol, Integer>> getTransitions() { return transitions; }

    /**
     * CLOSURE for LR(1): standard algorithm using FIRST sets to compute lookaheads for new items.
     */
    private Set<LR1Item> closure(Set<LR1Item> items) {
        Set<LR1Item> closure = new HashSet<>(items);
        Queue<LR1Item> worklist = new LinkedList<>(items);

        // Pre-calculate FIRST sets for all symbols
        StaticAnalyzer analyzer = new StaticAnalyzer(grammar);
        Map<Symbol, Set<Symbol>> firstSets = analyzer.getFirstSets();
        Symbol epsilon = grammar.getEpsilonSymbol();

        while (!worklist.isEmpty()) {
            LR1Item item = worklist.poll();
            Symbol B = item.getSymbolAfterDot();

            // If B is a non-terminal
            if (B != null && B.isNonTerminal()) {
                // For each production B -> γ
                for (Production production : grammar.getProductionsFor(B)) {
                    // Calculate FIRST(βa) where β is the sequence after B and a is the lookahead
                    List<Symbol> betaA = new ArrayList<>();

                    // Add symbols after B (β)
                    List<Symbol> rightSide = item.production.getRightSide();
                    for (int i = item.dotPosition + 1; i < rightSide.size(); i++) {
                        betaA.add(rightSide.get(i));
                    }

                    // Add the lookahead (a)
                    betaA.add(item.lookahead);

                    // Compute FIRST(βa)
                    Set<Symbol> firstBetaA = computeFirstOfSequence(betaA, firstSets, epsilon);

                    // For each terminal b in FIRST(βa)
                    for (Symbol b : firstBetaA) {
                        // Skip epsilon - we only want terminals
                        if (b.isEpsilon()) {
                            continue;
                        }

                        // Create new item [B -> •γ, b]
                        LR1Item newItem = new LR1Item(production, 0, b);

                        // If not already in closure, add it
                        if (!closure.contains(newItem)) {
                            closure.add(newItem);
                            worklist.add(newItem);
                        }
                    }
                }
            }
        }

        return closure;
    }

    /**
     * Compute FIRST of a sequence of symbols.
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> seq, Map<Symbol, Set<Symbol>> firstSets, Symbol epsilon) {
        Set<Symbol> result = new HashSet<>();

        if (seq.isEmpty()) {
            result.add(epsilon);
            return result;
        }

        for (int i = 0; i < seq.size(); i++) {
            Symbol X = seq.get(i);
            Set<Symbol> firstX = firstSets.get(X);

            if (firstX == null) {
                // If we don't have FIRST set for this symbol, skip it
                break;
            }

            // Add FIRST(X) - {epsilon} to result
            for (Symbol symbol : firstX) {
                if (!symbol.isEpsilon()) {
                    result.add(symbol);
                }
            }

            // If epsilon is not in FIRST(X), stop here
            if (!firstX.contains(epsilon)) {
                break;
            }

            // If this is the last symbol and epsilon is in FIRST(X), add epsilon
            if (i == seq.size() - 1 && firstX.contains(epsilon)) {
                result.add(epsilon);
            }
        }

        return result;
    }

    /**
     * GOTO for LR(1): moves dot over symbol and takes closure.
     */
    private Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {
        Set<LR1Item> movedItems = new HashSet<>();

        // For each item in the state
        for (LR1Item item : state) {
            Symbol X = item.getSymbolAfterDot();

            // If the symbol after the dot matches the input symbol
            if (X != null && X.equals(symbol)) {
                // Create new item with dot moved one position to the right
                LR1Item movedItem = new LR1Item(item.production, item.dotPosition + 1, item.lookahead);
                movedItems.add(movedItem);
            }
        }

        // Return closure of moved items
        if (movedItems.isEmpty()) {
            return new HashSet<>();
        }
        return closure(movedItems);
    }

    /**
     * Build the LR(1) canonical collection: states and transitions.
     */
    public void build() {
        // 1. Clear any existing states and transitions
        states.clear();
        transitions.clear();

        // 2. Create augmented grammar: S' -> S
        Symbol startSymbol = grammar.getStartSymbol();
        augmentedLeftName = startSymbol.name + "'";
        Symbol augmentedStart = Symbol.nonTerminal(augmentedLeftName);
        Production augmentedProduction = new Production(augmentedStart,
                java.util.Collections.singletonList(startSymbol));

        // 3. Create initial item [S' -> •S, $]
        Symbol endOfInput = grammar.getEndOfInputSymbol();
        LR1Item initialItem = new LR1Item(augmentedProduction, 0, endOfInput);

        // 4. Compute closure of initial item to get I0
        Set<LR1Item> initialItemSet = new HashSet<>();
        initialItemSet.add(initialItem);
        Set<LR1Item> I0 = closure(initialItemSet);
        states.add(I0);

        // 5. Create worklist and add I0
        Queue<Integer> worklist = new LinkedList<>();
        worklist.add(0);

        // 6. Process all states
        while (!worklist.isEmpty()) {
            int stateIndex = worklist.poll();
            Set<LR1Item> state = states.get(stateIndex);

            // For each grammar symbol (terminals and non-terminals)
            for (Symbol X : grammar.getAllSymbols()) {
                // Skip epsilon
                if (X.isEpsilon()) {
                    continue;
                }

                // Calculate GOTO(state, X)
                Set<LR1Item> J = goTo(state, X);

                if (!J.isEmpty()) {
                    // Check if J is already in the list of states
                    int targetStateIndex = states.indexOf(J);

                    if (targetStateIndex == -1) {
                        // J is a new state
                        targetStateIndex = states.size();
                        states.add(J);
                        worklist.add(targetStateIndex);
                    }

                    // Create transition from stateIndex to targetStateIndex on symbol X
                    transitions.computeIfAbsent(stateIndex, k -> new HashMap<>())
                              .put(X, targetStateIndex);
                }
            }
        }
    }

    public String getAugmentedLeftName() { return augmentedLeftName; }
}
