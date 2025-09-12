package com.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import com.compiler.lexer.*;

/**
 * Simple unit tests for the tokenizer functionality added in the latest commit.
 */
public class TokenizerTest {
    
    @Test
    void testTokenCreation() {
        Token token = new Token(TokenType.IDENTIFIER, "hello", 0);
        
        assertEquals(TokenType.IDENTIFIER, token.type);
        assertEquals("hello", token.lexeme);
        assertEquals(0, token.position);
    }
    
    @Test
    void testTokenEquality() {
        Token token1 = new Token(TokenType.IDENTIFIER, "hello", 0);
        Token token2 = new Token(TokenType.IDENTIFIER, "hello", 0);
        Token token3 = new Token(TokenType.KEYWORD, "hello", 0);
        
        assertEquals(token1, token2);
        assertNotEquals(token1, token3);
    }
    
    @Test
    void testLexicalRuleCreation() {
        LexicalRule rule = new LexicalRule("abc", TokenType.KEYWORD);
        
        assertEquals("abc", rule.pattern);
        assertEquals(TokenType.KEYWORD, rule.tokenType);
        assertEquals(0, rule.priority);
        assertFalse(rule.ignore);
    }
    
    @Test
    void testLexicalRuleWithPriority() {
        LexicalRule rule = new LexicalRule("def", TokenType.IDENTIFIER, 5);
        
        assertEquals("def", rule.pattern);
        assertEquals(TokenType.IDENTIFIER, rule.tokenType);
        assertEquals(5, rule.priority);
        assertFalse(rule.ignore);
    }
    
    @Test
    void testLexicalRuleWithIgnore() {
        LexicalRule rule = new LexicalRule("\\s+", TokenType.WHITESPACE, 10, true);
        
        assertEquals("\\s+", rule.pattern);
        assertEquals(TokenType.WHITESPACE, rule.tokenType);
        assertEquals(10, rule.priority);
        assertTrue(rule.ignore);
    }
    
    @Test
    void testBasicTokenization() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("a", TokenType.IDENTIFIER),
            new LexicalRule("b", TokenType.KEYWORD)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("ab");
        
        assertEquals(3, tokens.size());
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type);
        assertEquals("a", tokens.get(0).lexeme);
        assertEquals(0, tokens.get(0).position);
        
        assertEquals(TokenType.KEYWORD, tokens.get(1).type);
        assertEquals("b", tokens.get(1).lexeme);
        assertEquals(1, tokens.get(1).position);
        
        assertEquals(TokenType.EOF, tokens.get(2).type);
    }
    
    @Test
    void testEmptyInput() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("a", TokenType.IDENTIFIER)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("");
        
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type);
    }
    
    @Test
    void testUnknownCharacters() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("a", TokenType.IDENTIFIER)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("x");
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.UNKNOWN, tokens.get(0).type);
        assertEquals("x", tokens.get(0).lexeme);
        assertEquals(TokenType.EOF, tokens.get(1).type);
    }
    
    @Test
    void testMaximumMatch() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("a", TokenType.IDENTIFIER),
            new LexicalRule("ab", TokenType.KEYWORD)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("ab");
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.KEYWORD, tokens.get(0).type);
        assertEquals("ab", tokens.get(0).lexeme);
    }
    
    @Test
    void testPriorityResolution() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("if", TokenType.IDENTIFIER, 2),
            new LexicalRule("if", TokenType.KEYWORD, 1)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("if");
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.KEYWORD, tokens.get(0).type);
    }
    
    @Test
    void testIgnoredTokens() {
        List<LexicalRule> rules = Arrays.asList(
            new LexicalRule("a", TokenType.IDENTIFIER),
            new LexicalRule(" ", TokenType.WHITESPACE, 2, true)
        );
        
        Tokenizer tokenizer = new Tokenizer(rules);
        List<Token> tokens = tokenizer.tokenize("a a");
        
        assertEquals(3, tokens.size());
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals(TokenType.EOF, tokens.get(2).type);
    }
    
    @Test
    void testTokenTypeEnum() {
        assertEquals("IDENTIFIER", TokenType.IDENTIFIER.toString());
        assertEquals("KEYWORD", TokenType.KEYWORD.toString());
        assertEquals("EOF", TokenType.EOF.toString());
    }
}
