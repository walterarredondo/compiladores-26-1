package com.compiler.parser.grammar;

/**
 * Enumeration representing the types of symbols in a context-free grammar.
 */
public enum SymbolType {
    /**
     * Terminal symbol - appears in the input and cannot be further expanded.
     */
    TERMINAL,
    
    /**
     * Non-terminal symbol - can be expanded using production rules.
     */
    NON_TERMINAL
}
