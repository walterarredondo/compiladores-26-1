package com.compiler.lexer.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
        // Constructor implementation - no special initialization needed
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {
        if (regex == null || regex.isEmpty()) {
            return regex;
        }
        
        StringBuilder output = new StringBuilder();
        
        for (int i = 0; i < regex.length(); i++) {
            char current = regex.charAt(i);
            output.append(current);
            
            // If not at end of string, check if we need concatenation
            if (i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                
                // Insert concatenation operator if current and next form implicit concatenation
                if (shouldInsertConcatenation(current, next)) {
                    output.append('·');
                }
            }
        }
        
        return output.toString();
    }
    
    /**
     * Helper method to determine if concatenation should be inserted between two characters.
     */
    private static boolean shouldInsertConcatenation(char current, char next) {
        // Cases where concatenation should be inserted:
        // 1. operand followed by operand: ab -> a·b
        // 2. operand followed by '(': a( -> a·(
        // 3. ')' followed by operand: )a -> )·a  
        // 4. ')' followed by '(': )( -> )·(
        // 5. '*', '+', '?' followed by operand: a*b -> a*·b
        // 6. '*', '+', '?' followed by '(': a*( -> a*·(
        
        boolean currentCanBeFollowed = isOperand(current) || current == ')' || 
                                     current == '*' || current == '+' || current == '?';
        boolean nextCanFollow = isOperand(next) || next == '(';
        
        return currentCanBeFollowed && nextCanFollow;
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
        // Return true if c is not one of: '|', '*', '?', '+', '(', ')', '·'
        return c != '|' && c != '*' && c != '?' && c != '+' && 
               c != '(' && c != ')' && c != '·';
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        // 1. Define operator precedence map (higher number = higher precedence)
        Map<Character, Integer> precedence = new HashMap<>();
        precedence.put('|', 1);  // lowest precedence (union)
        precedence.put('·', 2);  // concatenation
        precedence.put('*', 3);  // highest precedence (Kleene star)
        precedence.put('+', 3);  // plus operator
        precedence.put('?', 3);  // optional operator
        
        // 2. Preprocess regex to insert explicit concatenation operators
        String preprocessed = insertConcatenationOperator(infixRegex);
        
        StringBuilder output = new StringBuilder();
        Stack<Character> operatorStack = new Stack<>();
        
        // 3. Process each character in the preprocessed regex
        for (int i = 0; i < preprocessed.length(); i++) {
            char c = preprocessed.charAt(i);
            
            if (isOperand(c)) {
                // If operand: append to output
                output.append(c);
            } else if (c == '(') {
                // If '(': push to stack
                operatorStack.push(c);
            } else if (c == ')') {
                // If ')': pop operators to output until '(' is found
                while (!operatorStack.isEmpty() && operatorStack.peek() != '(') {
                    output.append(operatorStack.pop());
                }
                if (!operatorStack.isEmpty() && operatorStack.peek() == '(') {
                    operatorStack.pop(); // Remove the '('
                }
            } else if (precedence.containsKey(c)) {
                // If operator: pop operators with higher/equal precedence, then push current operator
                while (!operatorStack.isEmpty() && 
                       operatorStack.peek() != '(' &&
                       precedence.containsKey(operatorStack.peek()) &&
                       precedence.get(operatorStack.peek()) >= precedence.get(c)) {
                    output.append(operatorStack.pop());
                }
                operatorStack.push(c);
            }
        }
        
        // 4. After loop, pop remaining operators to output
        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek() != '(' && operatorStack.peek() != ')') {
                output.append(operatorStack.pop());
            } else {
                operatorStack.pop(); // Remove unmatched parentheses
            }
        }
        
        // 5. Return output as string
        return output.toString();
    }
}
