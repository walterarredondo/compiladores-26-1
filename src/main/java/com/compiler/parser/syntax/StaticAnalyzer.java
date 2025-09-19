package com.compiler.parser.syntax;

import java.util.*;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;

/**
 * StaticAnalyzer for computing FIRST and FOLLOW sets for a context-free grammar.
 * 
 * This class implements the canonical algorithms for static analysis of CFGs,
 * which are essential for constructing predictive parsers (LL(1)) and 
 * bottom-up parsers (LALR(1)).
 */
public class StaticAnalyzer {
    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;
    
    /**
     * Constructs a StaticAnalyzer for the given grammar.
     * @param grammar The context-free grammar to analyze
     */
    public StaticAnalyzer(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }
    
    /**
     * Computes FIRST sets for all symbols in the grammar.
     * 
     * Algorithm:
     * 1. For each terminal symbol t, FIRST(t) = {t}
     * 2. Initialize FIRST sets for non-terminals as empty
     * 3. Repeat until no changes:
     *    For each production A -> X1 X2 ... Xk:
     *    - Add FIRST(X1) - {ε} to FIRST(A)
     *    - If ε ∈ FIRST(X1), add FIRST(X2) - {ε} to FIRST(A)
     *    - Continue until Xi where ε ∉ FIRST(Xi) or all symbols processed
     *    - If ε ∈ FIRST(Xi) for all i, add ε to FIRST(A)
     * 
     * @return Map from symbols to their FIRST sets
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {
        // Initialize FIRST sets
        initializeFirstSets();
        
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Process each production
            for (Production production : grammar.getProductions()) {
                Symbol leftSide = production.getLeftSide();
                List<Symbol> rightSide = production.getRightSide();
                
                // Handle ε-production
                if (rightSide.isEmpty() || (rightSide.size() == 1 && rightSide.get(0).isEpsilon())) {
                    if (addToFirstSet(leftSide, grammar.getEpsilonSymbol())) {
                        changed = true;
                    }
                    continue;
                }
                
                // Process the right-hand side
                Set<Symbol> toAdd = new HashSet<>();
                boolean allHaveEpsilon = true;
                
                for (int i = 0; i < rightSide.size(); i++) {
                    Symbol currentSymbol = rightSide.get(i);
                    Set<Symbol> currentFirst = firstSets.get(currentSymbol);
                    
                    // Add FIRST(currentSymbol) - {ε} to the set we're building
                    for (Symbol symbol : currentFirst) {
                        if (!symbol.isEpsilon()) {
                            toAdd.add(symbol);
                        }
                    }
                    
                    // If ε is not in FIRST(currentSymbol), stop here
                    if (!currentFirst.contains(grammar.getEpsilonSymbol())) {
                        allHaveEpsilon = false;
                        break;
                    }
                }
                
                // Add all symbols we collected to FIRST(leftSide)
                for (Symbol symbol : toAdd) {
                    if (addToFirstSet(leftSide, symbol)) {
                        changed = true;
                    }
                }
                
                // If all symbols can derive ε, add ε to FIRST(leftSide)
                if (allHaveEpsilon) {
                    if (addToFirstSet(leftSide, grammar.getEpsilonSymbol())) {
                        changed = true;
                    }
                }
            }
        }
        
        return new HashMap<>(firstSets);
    }
    
    /**
     * Computes FOLLOW sets for all non-terminal symbols in the grammar.
     * 
     * Algorithm:
     * 1. Add $ to FOLLOW(startSymbol)
     * 2. Repeat until no changes:
     *    For each production A -> αBβ:
     *    - Add FIRST(β) - {ε} to FOLLOW(B)
     *    - If ε ∈ FIRST(β), add FOLLOW(A) to FOLLOW(B)
     *    For each production A -> αB:
     *    - Add FOLLOW(A) to FOLLOW(B)
     * 
     * @return Map from non-terminal symbols to their FOLLOW sets
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        // Ensure FIRST sets are computed
        if (firstSets.isEmpty()) {
            getFirstSets();
        }
        
        // Initialize FOLLOW sets
        initializeFollowSets();
        
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Process each production
            for (Production production : grammar.getProductions()) {
                Symbol leftSide = production.getLeftSide();
                List<Symbol> rightSide = production.getRightSide();
                
                // Skip ε-productions
                if (rightSide.isEmpty() || (rightSide.size() == 1 && rightSide.get(0).isEpsilon())) {
                    continue;
                }
                
                // For each symbol B in the right-hand side
                for (int i = 0; i < rightSide.size(); i++) {
                    Symbol B = rightSide.get(i);
                    
                    // Skip terminals
                    if (B.isTerminal()) {
                        continue;
                    }
                    
                    // Get β (the symbols after B)
                    List<Symbol> beta = rightSide.subList(i + 1, rightSide.size());
                    
                    if (beta.isEmpty()) {
                        // Production is A -> αB, so add FOLLOW(A) to FOLLOW(B)
                        Set<Symbol> followA = followSets.get(leftSide);
                        if (followA != null) {
                            for (Symbol symbol : followA) {
                                if (addToFollowSet(B, symbol)) {
                                    changed = true;
                                }
                            }
                        }
                    } else {
                        // Production is A -> αBβ
                        // Compute FIRST(β)
                        Set<Symbol> firstBeta = computeFirstOfSequence(beta);
                        
                        // Add FIRST(β) - {ε} to FOLLOW(B)
                        for (Symbol symbol : firstBeta) {
                            if (!symbol.isEpsilon()) {
                                if (addToFollowSet(B, symbol)) {
                                    changed = true;
                                }
                            }
                        }
                        
                        // If ε ∈ FIRST(β), add FOLLOW(A) to FOLLOW(B)
                        if (firstBeta.contains(grammar.getEpsilonSymbol())) {
                            Set<Symbol> followA = followSets.get(leftSide);
                            if (followA != null) {
                                for (Symbol symbol : followA) {
                                    if (addToFollowSet(B, symbol)) {
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return new HashMap<>(followSets);
    }
    
    /**
     * Initializes FIRST sets for all symbols.
     * Terminals get themselves, non-terminals start empty.
     */
    private void initializeFirstSets() {
        firstSets.clear();
        
        // Initialize FIRST sets for all symbols
        for (Symbol symbol : grammar.getAllSymbols()) {
            Set<Symbol> firstSet = new HashSet<>();
            
            if (symbol.isTerminal()) {
                // FIRST(terminal) = {terminal}
                firstSet.add(symbol);
            }
            // Non-terminals start with empty sets
            
            firstSets.put(symbol, firstSet);
        }
    }
    
    /**
     * Initializes FOLLOW sets for all non-terminal symbols.
     * Only the start symbol gets $ initially.
     */
    private void initializeFollowSets() {
        followSets.clear();
        
        // Initialize FOLLOW sets for non-terminals
        for (Symbol symbol : grammar.getAllSymbols()) {
            if (!symbol.isTerminal()) {
                Set<Symbol> followSet = new HashSet<>();
                
                // Add $ to FOLLOW(startSymbol)
                if (symbol.equals(grammar.getStartSymbol())) {
                    followSet.add(grammar.getEndOfInputSymbol());
                }
                
                followSets.put(symbol, followSet);
            }
        }
    }
    
    /**
     * Adds a symbol to the FIRST set of the given symbol.
     * @param symbol The symbol whose FIRST set to modify
     * @param toAdd The symbol to add
     * @return true if the set was modified, false otherwise
     */
    private boolean addToFirstSet(Symbol symbol, Symbol toAdd) {
        Set<Symbol> firstSet = firstSets.get(symbol);
        if (firstSet == null) {
            firstSet = new HashSet<>();
            firstSets.put(symbol, firstSet);
        }
        return firstSet.add(toAdd);
    }
    
    /**
     * Adds a symbol to the FOLLOW set of the given symbol.
     * @param symbol The symbol whose FOLLOW set to modify
     * @param toAdd The symbol to add
     * @return true if the set was modified, false otherwise
     */
    private boolean addToFollowSet(Symbol symbol, Symbol toAdd) {
        Set<Symbol> followSet = followSets.get(symbol);
        if (followSet == null) {
            followSet = new HashSet<>();
            followSets.put(symbol, followSet);
        }
        return followSet.add(toAdd);
    }
    
    /**
     * Computes FIRST for a sequence of symbols (β in the algorithm).
     * @param sequence The sequence of symbols
     * @return The FIRST set of the sequence
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> sequence) {
        Set<Symbol> result = new HashSet<>();
        
        if (sequence.isEmpty()) {
            result.add(grammar.getEpsilonSymbol());
            return result;
        }
        
        for (Symbol symbol : sequence) {
            Set<Symbol> firstSymbol = firstSets.get(symbol);
            if (firstSymbol == null) {
                break;
            }
            
            // Add FIRST(symbol) - {ε} to result
            for (Symbol s : firstSymbol) {
                if (!s.isEpsilon()) {
                    result.add(s);
                }
            }
            
            // If ε is not in FIRST(symbol), stop
            if (!firstSymbol.contains(grammar.getEpsilonSymbol())) {
                break;
            }
            
            // If we've processed all symbols and all can derive ε
            if (symbol == sequence.get(sequence.size() - 1)) {
                result.add(grammar.getEpsilonSymbol());
            }
        }
        
        return result;
    }
    
    /**
     * Gets the FIRST set for a specific symbol.
     * @param symbol The symbol to get FIRST set for
     * @return The FIRST set, or empty set if not computed
     */
    public Set<Symbol> getFirstSet(Symbol symbol) {
        return firstSets.getOrDefault(symbol, new HashSet<>());
    }
    
    /**
     * Gets the FOLLOW set for a specific symbol.
     * @param symbol The symbol to get FOLLOW set for
     * @return The FOLLOW set, or empty set if not computed
     */
    public Set<Symbol> getFollowSet(Symbol symbol) {
        return followSets.getOrDefault(symbol, new HashSet<>());
    }
    
    /**
     * Returns a string representation of all FIRST sets.
     * @return Formatted string with FIRST sets
     */
    public String getFirstSetsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FIRST Sets:\n");
        sb.append("===========\n");
        
        for (Map.Entry<Symbol, Set<Symbol>> entry : firstSets.entrySet()) {
            sb.append("FIRST(").append(entry.getKey()).append(") = {");
            sb.append(String.join(", ", entry.getValue().stream()
                    .map(Symbol::toString)
                    .sorted()
                    .toArray(String[]::new)));
            sb.append("}\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a string representation of all FOLLOW sets.
     * @return Formatted string with FOLLOW sets
     */
    public String getFollowSetsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FOLLOW Sets:\n");
        sb.append("============\n");
        
        for (Map.Entry<Symbol, Set<Symbol>> entry : followSets.entrySet()) {
            sb.append("FOLLOW(").append(entry.getKey()).append(") = {");
            sb.append(String.join(", ", entry.getValue().stream()
                    .map(Symbol::toString)
                    .sorted()
                    .toArray(String[]::new)));
            sb.append("}\n");
        }
        
        return sb.toString();
    }
}
