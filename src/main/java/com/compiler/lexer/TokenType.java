package com.compiler.lexer;

/**
 * Enumeration of token types supported by the lexical analyzer.
 * Each token type represents a different category of lexical element.
 */
public enum TokenType {
    /**
     * Identifiers - variable names, function names, etc.
     */
    IDENTIFIER,
    
    /**
     * Numeric literals - integers, floating point numbers.
     */
    NUMBER,
    
    /**
     * String literals - text enclosed in quotes.
     */
    STRING,
    
    /**
     * Keywords - reserved words like if, while, class, etc.
     */
    KEYWORD,
    
    /**
     * Operators - arithmetic, logical, comparison operators.
     */
    OPERATOR,
    
    /**
     * Delimiters - parentheses, braces, semicolons, etc.
     */
    DELIMITER,
    
    /**
     * Whitespace - spaces, tabs, newlines (usually ignored).
     */
    WHITESPACE,
    
    /**
     * Comments - single-line or multi-line comments (usually ignored).
     */
    COMMENT,
    
    /**
     * End of file marker.
     */
    EOF,
    
    /**
     * Unknown or invalid tokens.
     */
    UNKNOWN;
    
    /**
     * Returns a string representation of the token type.
     * @return The name of the token type.
     */
    @Override
    public String toString() {
        return name();
    }
}
