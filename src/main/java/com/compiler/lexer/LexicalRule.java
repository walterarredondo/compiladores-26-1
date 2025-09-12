package com.compiler.lexer;

/**
 * Represents a lexical rule that pairs a regular expression pattern with a token type.
 * Used to define the lexical structure of a language.
 */
public class LexicalRule {
    /**
     * The regular expression pattern for this rule.
     */
    public final String pattern;
    
    /**
     * The token type that should be generated when this pattern matches.
     */
    public final TokenType tokenType;
    
    /**
     * The priority of this rule (lower values = higher priority).
     * Used to resolve conflicts when multiple rules could match.
     */
    public final int priority;
    
    /**
     * Whether tokens of this type should be ignored (not included in output).
     * Useful for whitespace, comments, etc.
     */
    public final boolean ignore;
    
    /**
     * Constructs a new lexical rule with default priority and not ignored.
     * @param pattern The regular expression pattern.
     * @param tokenType The token type to generate.
     */
    public LexicalRule(String pattern, TokenType tokenType) {
        this(pattern, tokenType, 0, false);
    }
    
    /**
     * Constructs a new lexical rule with specified priority.
     * @param pattern The regular expression pattern.
     * @param tokenType The token type to generate.
     * @param priority The priority of this rule.
     */
    public LexicalRule(String pattern, TokenType tokenType, int priority) {
        this(pattern, tokenType, priority, false);
    }
    
    /**
     * Constructs a new lexical rule with all parameters.
     * @param pattern The regular expression pattern.
     * @param tokenType The token type to generate.
     * @param priority The priority of this rule.
     * @param ignore Whether to ignore tokens of this type.
     */
    public LexicalRule(String pattern, TokenType tokenType, int priority, boolean ignore) {
        this.pattern = pattern;
        this.tokenType = tokenType;
        this.priority = priority;
        this.ignore = ignore;
    }
    
    /**
     * Returns a string representation of the lexical rule.
     * @return String representation showing pattern, type, priority, and ignore flag.
     */
    @Override
    public String toString() {
        return String.format("LexicalRule{pattern='%s', type=%s, priority=%d, ignore=%s}", 
                           pattern, tokenType, priority, ignore);
    }
}
