package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.regex.RegexParser;

/**
 * Tokenizer for lexical analysis using multiple regular expression rules.
 * Implements the maximum match rule: when multiple patterns could match,
 * choose the one that consumes the most characters. In case of ties,
 * use rule priority (lower priority value = higher precedence).
 */
public class Tokenizer {
    private final List<LexicalRule> rules;
    private DFA combinedDfa;
    private Set<Character> alphabet;
    
    /**
     * Constructs a tokenizer with the given lexical rules.
     * @param rules List of lexical rules defining the token patterns.
     */
    public Tokenizer(List<LexicalRule> rules) {
        this.rules = new ArrayList<>(rules);
        this.alphabet = new HashSet<>();
        buildCombinedAutomaton();
    }
    
    /**
     * Tokenizes the input string according to the lexical rules.
     * Applies the maximum match rule to resolve ambiguities.
     * @param input The input string to tokenize.
     * @return List of tokens extracted from the input.
     */
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        
        while (position < input.length()) {
            TokenMatch match = findLongestMatch(input, position);
            
            if (match == null) {
                // No match found, create an UNKNOWN token for the current character
                tokens.add(new Token(TokenType.UNKNOWN, 
                                   String.valueOf(input.charAt(position)), 
                                   position));
                position++;
            } else {
                // Check if this token type should be ignored
                LexicalRule matchedRule = rules.stream()
                    .filter(rule -> rule.tokenType == match.tokenType)
                    .min(Comparator.comparingInt(rule -> rule.priority))
                    .orElse(null);
                
                if (matchedRule == null || !matchedRule.ignore) {
                    tokens.add(new Token(match.tokenType, match.lexeme, position));
                }
                position = match.endPosition;
            }
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }
    
    /**
     * Finds the longest matching token starting at the given position.
     * Implements the maximum match rule with priority-based tie breaking.
     * @param input The input string.
     * @param startPosition The position to start matching from.
     * @return The longest token match, or null if no match found.
     */
    private TokenMatch findLongestMatch(String input, int startPosition) {
        DfaState currentState = combinedDfa.startState;
        TokenMatch bestMatch = null;
        int currentPosition = startPosition;
        
        // Check if start state is final (for empty string matches)
        if (currentState.isFinal() && currentState.getTokenType() != null) {
            bestMatch = new TokenMatch(currentState.getTokenType(), "", startPosition);
        }
        
        while (currentPosition < input.length()) {
            char symbol = input.charAt(currentPosition);
            DfaState nextState = currentState.getTransition(symbol);
            
            if (nextState == null) {
                // No transition available, stop here
                break;
            }
            
            currentState = nextState;
            currentPosition++;
            
            // Check if current state is final and update best match
            if (currentState.isFinal() && currentState.getTokenType() != null) {
                String lexeme = input.substring(startPosition, currentPosition);
                TokenMatch currentMatch = new TokenMatch(currentState.getTokenType(), 
                                                       lexeme, currentPosition);
                
                if (bestMatch == null || 
                    currentMatch.lexeme.length() > bestMatch.lexeme.length() ||
                    (currentMatch.lexeme.length() == bestMatch.lexeme.length() && 
                     currentState.getTokenPriority() < bestMatch.priority)) {
                    bestMatch = currentMatch;
                    bestMatch.priority = currentState.getTokenPriority();
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Builds a combined DFA from all the lexical rules.
     * Each NFA for individual rules is combined into a single automaton.
     */
    private void buildCombinedAutomaton() {
        if (rules.isEmpty()) {
            return;
        }
        
        // Create NFAs for each rule
        List<EnhancedNfaToDfaConverter.RuleNfa> ruleNfas = new ArrayList<>();
        RegexParser parser = new RegexParser();
        
        for (int i = 0; i < rules.size(); i++) {
            LexicalRule rule = rules.get(i);
            
            try {
                // Parse the regex pattern to NFA
                var nfa = parser.parse(rule.pattern);
                
                // Mark end state with token information
                nfa.endState.isFinal = true;
                
                // Extract alphabet from pattern
                extractAlphabet(rule.pattern);
                
                ruleNfas.add(new EnhancedNfaToDfaConverter.RuleNfa(nfa, rule, i));
            } catch (Exception e) {
                // Handle malformed regex patterns gracefully
                System.err.println("Warning: Failed to parse pattern '" + rule.pattern + 
                                 "' for token type " + rule.tokenType + ": " + e.getMessage());
                // For debugging, let's also print the stack trace
                if (System.getProperty("debug.tokenizer") != null) {
                    e.printStackTrace();
                }
                continue;
            }
        }
        
        // Convert all NFAs to combined DFA
        if (!ruleNfas.isEmpty()) {
            combinedDfa = EnhancedNfaToDfaConverter.convertMultipleNfasToDfa(ruleNfas, alphabet);
        } else {
            // If no valid rules, create a minimal DFA
            System.err.println("Warning: No valid lexical rules found, tokenizer may not work correctly");
        }
    }
    
    /**
     * Extracts alphabet characters from a regex pattern.
     * @param pattern The regex pattern to analyze.
     */
    private void extractAlphabet(String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            // Add non-operator characters to alphabet
            if (c != '|' && c != '*' && c != '+' && c != '?' && 
                c != '(' && c != ')' && c != '·' && c != '\\') {
                alphabet.add(c);
            } else if (c == '\\' && i + 1 < pattern.length()) {
                // Handle escaped characters
                char escaped = pattern.charAt(i + 1);
                alphabet.add(escaped);
                i++; // Skip the escaped character
            }
        }
    }
    
    /**
     * Helper class to represent a token match during scanning.
     */
    private static class TokenMatch {
        final TokenType tokenType;
        final String lexeme;
        final int endPosition;
        int priority;
        
        TokenMatch(TokenType tokenType, String lexeme, int endPosition) {
            this.tokenType = tokenType;
            this.lexeme = lexeme;
            this.endPosition = endPosition;
            this.priority = Integer.MAX_VALUE;
        }
    }
}
