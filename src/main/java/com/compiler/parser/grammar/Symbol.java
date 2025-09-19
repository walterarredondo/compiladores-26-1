package com.compiler.parser.grammar;

import java.util.Objects;

/**
 * Represents a symbol in a context-free grammar.
 * A symbol can be either terminal or non-terminal.
 */
public class Symbol {
    /**
     * The name of this symbol.
     */
    public final String name;
    
    /**
     * The type of this symbol (terminal or non-terminal).
     */
    public final SymbolType type;
    
    // Special symbols
    public static final Symbol EPSILON = new Symbol("ε", SymbolType.TERMINAL);
    public static final Symbol END_OF_INPUT = new Symbol("$", SymbolType.TERMINAL);
    
    /**
     * Creates a new symbol.
     * @param name The name of the symbol
     * @param type The type of the symbol (terminal or non-terminal)
     */
    public Symbol(String name, SymbolType type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * Creates a terminal symbol.
     * @param name The name of the terminal
     * @return A new terminal symbol
     */
    public static Symbol terminal(String name) {
        return new Symbol(name, SymbolType.TERMINAL);
    }
    
    /**
     * Creates a non-terminal symbol.
     * @param name The name of the non-terminal
     * @return A new non-terminal symbol
     */
    public static Symbol nonTerminal(String name) {
        return new Symbol(name, SymbolType.NON_TERMINAL);
    }
    
    /**
     * Gets the name of this symbol.
     * @return The symbol name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the type of this symbol.
     * @return The symbol type
     */
    public SymbolType getType() {
        return type;
    }
    
    /**
     * Checks if this symbol is terminal.
     * @return true if terminal, false if non-terminal
     */
    public boolean isTerminal() {
        return type == SymbolType.TERMINAL;
    }
    
    /**
     * Checks if this symbol is non-terminal.
     * @return true if non-terminal, false if terminal
     */
    public boolean isNonTerminal() {
        return type == SymbolType.NON_TERMINAL;
    }
    
    /**
     * Checks if this symbol is epsilon (ε).
     * @return true if this is the epsilon symbol
     */
    public boolean isEpsilon() {
        return this.equals(EPSILON);
    }
    
    /**
     * Checks if this symbol is the end-of-input marker ($).
     * @return true if this is the end-of-input symbol
     */
    public boolean isEndOfInput() {
        return this.equals(END_OF_INPUT);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Symbol symbol = (Symbol) obj;
        return type == symbol.type && 
               Objects.equals(name, symbol.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * Returns a detailed string representation including symbol type.
     * @return Detailed string representation
     */
    public String toDetailedString() {
        return name + " (" + (type == SymbolType.TERMINAL ? "T" : "NT") + ")";
    }
}
