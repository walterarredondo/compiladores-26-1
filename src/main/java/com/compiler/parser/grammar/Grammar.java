package com.compiler.parser.grammar;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a context-free grammar with productions, symbols, and a start symbol.
 * Provides methods for parsing grammar definitions and accessing grammar components.
 */
public class Grammar {
    private final Set<Symbol> terminals;
    private final Set<Symbol> nonTerminals;
    private final List<Production> productions;
    private final Symbol startSymbol;
    
    /**
     * Creates a new grammar.
     * @param productions List of production rules
     * @param startSymbol The start symbol of the grammar
     */
    public Grammar(List<Production> productions, Symbol startSymbol) {
        this.productions = new ArrayList<>(productions);
        this.startSymbol = startSymbol;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        
        // Extract terminals and non-terminals from productions
        extractSymbols();
        
        // Add special symbols
        terminals.add(Symbol.EPSILON);
        terminals.add(Symbol.END_OF_INPUT);
    }
    
    /**
     * Creates a grammar from a string representation.
     * Expected format:
     * S -> a S | b
     * A -> a
     * B -> b
     * 
     * @param grammarText The grammar definition as text
     */
    public Grammar(String grammarText) {
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        this.productions = new ArrayList<>();
        
        // Add special symbols
        terminals.add(Symbol.EPSILON);
        terminals.add(Symbol.END_OF_INPUT);
        
        Map<String, Symbol> symbolMap = new HashMap<>();
        
        // Add special symbols to map
        symbolMap.put("ε", Symbol.EPSILON);
        symbolMap.put("$", Symbol.END_OF_INPUT);
        
        String[] lines = grammarText.trim().split("\n");
        Symbol firstStartSymbol = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("->");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid production format: " + line);
            }
            
            String leftSideName = parts[0].trim();
            String rightSideText = parts[1].trim();
            
            // Create or get left side symbol (always non-terminal)
            Symbol leftSide = symbolMap.get(leftSideName);
            if (leftSide == null) {
                leftSide = Symbol.nonTerminal(leftSideName);
                symbolMap.put(leftSideName, leftSide);
                nonTerminals.add(leftSide);
            }
            
            // Set first non-terminal as start symbol
            if (firstStartSymbol == null) {
                firstStartSymbol = leftSide;
            }
            
            // Handle multiple alternatives separated by |
            String[] alternatives = rightSideText.split("\\|");
            
            for (String alternative : alternatives) {
                alternative = alternative.trim();
                List<Symbol> rightSide = new ArrayList<>();
                
                if (alternative.equals("ε") || alternative.isEmpty()) {
                    rightSide.add(Symbol.EPSILON);
                } else {
                    // Split by whitespace to get individual symbols
                    String[] symbols = alternative.split("\\s+");
                    
                    for (String symbolName : symbols) {
                        symbolName = symbolName.trim();
                        if (!symbolName.isEmpty()) {
                            Symbol symbol = symbolMap.get(symbolName);
                            if (symbol == null) {
                                // Determine if it's terminal or non-terminal
                                // Convention: uppercase/multi-char = non-terminal, 
                                // lowercase/single-char/special = terminal
                                boolean isTerminal = isTerminalByConvention(symbolName);
                                if (isTerminal) {
                                    symbol = Symbol.terminal(symbolName);
                                    terminals.add(symbol);
                                } else {
                                    symbol = Symbol.nonTerminal(symbolName);
                                    nonTerminals.add(symbol);
                                }
                                symbolMap.put(symbolName, symbol);
                            }
                            rightSide.add(symbol);
                        }
                    }
                }
                
                productions.add(new Production(leftSide, rightSide));
            }
        }
        
        this.startSymbol = firstStartSymbol;
        if (this.startSymbol == null) {
            throw new IllegalArgumentException("No valid start symbol found in grammar");
        }
    }
    
    /**
     * Determines if a symbol name represents a terminal by convention.
     * @param symbolName The symbol name to check
     * @return true if it should be treated as a terminal
     */
    private static boolean isTerminalByConvention(String symbolName) {
        // Special symbols
        if (symbolName.equals("ε") || symbolName.equals("$")) {
            return true;
        }
        
        // Single character symbols are usually terminals
        if (symbolName.length() == 1) {
            char c = symbolName.charAt(0);
            // Check if it's a common terminal character
            return !Character.isUpperCase(c) || 
                   "(){}[];,+-*/<>=!&|".indexOf(c) >= 0;
        }
        
        // Multi-character symbols
        // Keywords, operators, and identifiers are terminals
        String lower = symbolName.toLowerCase();
        if (lower.equals("if") || lower.equals("while") || lower.equals("int") || 
            lower.equals("float") || lower.equals("bool") || lower.equals("print") ||
            lower.equals("else") || lower.equals("id") || lower.equals("num") ||
            lower.contains("=") || lower.contains("<") || lower.contains(">")) {
            return true;
        }
        
        // All uppercase or contains ' are typically non-terminals
        return !symbolName.equals(symbolName.toUpperCase()) && 
               !symbolName.contains("'");
    }
    
    /**
     * Extracts all symbols from the productions and categorizes them.
     */
    private void extractSymbols() {
        for (Production production : productions) {
            Symbol leftSide = production.getLeftSide();
            nonTerminals.add(leftSide);
            
            for (Symbol symbol : production.getRightSide()) {
                if (symbol.isTerminal()) {
                    terminals.add(symbol);
                } else {
                    nonTerminals.add(symbol);
                }
            }
        }
    }
    
    /**
     * Gets all terminal symbols in the grammar.
     * @return Set of terminal symbols
     */
    public Set<Symbol> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }
    
    /**
     * Gets all non-terminal symbols in the grammar.
     * @return Set of non-terminal symbols
     */
    public Set<Symbol> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }
    
    /**
     * Gets all symbols (terminals and non-terminals) in the grammar.
     * @return Set of all symbols
     */
    public Set<Symbol> getAllSymbols() {
        Set<Symbol> allSymbols = new HashSet<>(terminals);
        allSymbols.addAll(nonTerminals);
        return allSymbols;
    }
    
    /**
     * Gets all production rules in the grammar.
     * @return List of productions
     */
    public List<Production> getProductions() {
        return Collections.unmodifiableList(productions);
    }
    
    /**
     * Gets productions for a specific non-terminal.
     * @param nonTerminal The non-terminal to get productions for
     * @return List of productions with the given left side
     */
    public List<Production> getProductionsFor(Symbol nonTerminal) {
        return productions.stream()
                .filter(p -> p.getLeftSide().equals(nonTerminal))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the start symbol of the grammar.
     * @return The start symbol
     */
    public Symbol getStartSymbol() {
        return startSymbol;
    }
    
    /**
     * Gets the epsilon symbol.
     * @return The epsilon symbol
     */
    public Symbol getEpsilonSymbol() {
        return Symbol.EPSILON;
    }
    
    /**
     * Gets the end-of-input symbol.
     * @return The end-of-input symbol ($)
     */
    public Symbol getEndOfInputSymbol() {
        return Symbol.END_OF_INPUT;
    }
    
    /**
     * Checks if a symbol is nullable (can derive epsilon).
     * This is a simplified check that only looks for direct epsilon productions.
     * @param symbol The symbol to check
     * @return true if the symbol has an epsilon production
     */
    public boolean isNullable(Symbol symbol) {
        if (symbol.isTerminal()) {
            return symbol.isEpsilon();
        }
        
        return productions.stream()
                .anyMatch(p -> p.getLeftSide().equals(symbol) && p.isEpsilonProduction());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Grammar:\n");
        sb.append("Start Symbol: ").append(startSymbol).append("\n");
        sb.append("Productions:\n");
        
        for (Production production : productions) {
            sb.append("  ").append(production).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a detailed string representation of the grammar.
     * @return Detailed string with symbol information
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Grammar Details:\n");
        sb.append("================\n");
        sb.append("Start Symbol: ").append(startSymbol.toDetailedString()).append("\n\n");
        
        sb.append("Terminals: ");
        sb.append(terminals.stream()
                .map(Symbol::toString)
                .sorted()
                .collect(Collectors.joining(", ")));
        sb.append("\n\n");
        
        sb.append("Non-Terminals: ");
        sb.append(nonTerminals.stream()
                .map(Symbol::toString)
                .sorted()
                .collect(Collectors.joining(", ")));
        sb.append("\n\n");
        
        sb.append("Productions:\n");
        for (Production production : productions) {
            sb.append("  ").append(production.toDetailedString()).append("\n");
        }
        
        return sb.toString();
    }
}
