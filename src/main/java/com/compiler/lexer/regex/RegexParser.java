package com.compiler.lexer.regex;

import java.util.Stack;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */
/**
 * Parses regular expressions and constructs NFAs using Thompson's construction.
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
    public RegexParser() {
        // Constructor implementation - no special initialization needed
    }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
        // Convert infix to postfix using Shunting Yard algorithm
        String postfixRegex = ShuntingYard.toPostfix(infixRegex);
        
        // Build NFA from postfix expression
        return buildNfaFromPostfix(postfixRegex);
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    private NFA buildNfaFromPostfix(String postfixRegex) {
        Stack<NFA> nfaStack = new Stack<>();
        
        for (int i = 0; i < postfixRegex.length(); i++) {
            char c = postfixRegex.charAt(i);
            
            if (isOperand(c)) {
                // Create NFA for single character and push to stack
                NFA charNfa = createNfaForCharacter(c);
                nfaStack.push(charNfa);
            } else {
                // Handle operators
                switch (c) {
                    case '·':
                        handleConcatenation(nfaStack);
                        break;
                    case '|':
                        handleUnion(nfaStack);
                        break;
                    case '*':
                        handleKleeneStar(nfaStack);
                        break;
                    case '+':
                        handlePlus(nfaStack);
                        break;
                    case '?':
                        handleOptional(nfaStack);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operator: " + c);
                }
            }
        }
        
        // The stack should contain exactly one NFA at the end
        if (nfaStack.size() != 1) {
            throw new IllegalStateException("Invalid postfix expression: stack size = " + nfaStack.size());
        }
        
        return nfaStack.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack underflow for optional operator");
        }
        
        NFA nfa = stack.pop();
        
        State newStart = new State();
        State newEnd = new State();
        
        // Epsilon transition from new start to original start (one occurrence)
        newStart.transitions.add(new Transition(null, nfa.startState));
        
        // Epsilon transition from new start to new end (zero occurrences)
        newStart.transitions.add(new Transition(null, newEnd));
        
        // Epsilon transition from original end to new end
        nfa.endState.transitions.add(new Transition(null, newEnd));
        
        stack.push(new NFA(newStart, newEnd));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack underflow for plus operator");
        }
        
        NFA nfa = stack.pop();
        
        State newStart = new State();
        State newEnd = new State();
        
        // Epsilon transition from new start to original start (mandatory first occurrence)
        newStart.transitions.add(new Transition(null, nfa.startState));
        
        // Epsilon transition from original end to new end
        nfa.endState.transitions.add(new Transition(null, newEnd));
        
        // Epsilon transition from original end back to original start (repetition)
        nfa.endState.transitions.add(new Transition(null, nfa.startState));
        
        stack.push(new NFA(newStart, newEnd));
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
        State start = new State();
        State end = new State();
        
        // Add transition from start to end with the character
        start.transitions.add(new Transition(c, end));
        
        return new NFA(start, end);
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Stack underflow for concatenation operator");
        }
        
        NFA second = stack.pop();  // Pop second operand first
        NFA first = stack.pop();   // Pop first operand second
        
        // Connect end of first NFA to start of second NFA with epsilon transition
        first.endState.transitions.add(new Transition(null, second.startState));
        
        // Create new NFA with first's start and second's end
        stack.push(new NFA(first.startState, second.endState));
    }

    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Stack underflow for union operator");
        }
        
        NFA second = stack.pop();  // Pop second operand first
        NFA first = stack.pop();   // Pop first operand second
        
        State newStart = new State();
        State newEnd = new State();
        
        // Epsilon transitions from new start to both NFAs
        newStart.transitions.add(new Transition(null, first.startState));
        newStart.transitions.add(new Transition(null, second.startState));
        
        // Epsilon transitions from both NFAs to new end
        first.endState.transitions.add(new Transition(null, newEnd));
        second.endState.transitions.add(new Transition(null, newEnd));
        
        stack.push(new NFA(newStart, newEnd));
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack underflow for Kleene star operator");
        }
        
        NFA nfa = stack.pop();
        
        State newStart = new State();
        State newEnd = new State();
        
        // Epsilon transition from new start to original start (enter the loop)
        newStart.transitions.add(new Transition(null, nfa.startState));
        
        // Epsilon transition from new start to new end (zero occurrences)
        newStart.transitions.add(new Transition(null, newEnd));
        
        // Epsilon transition from original end to new end (exit the loop)
        nfa.endState.transitions.add(new Transition(null, newEnd));
        
        // Epsilon transition from original end back to original start (repeat)
        nfa.endState.transitions.add(new Transition(null, nfa.startState));
        
        stack.push(new NFA(newStart, newEnd));
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
        return c != '|' && c != '*' && c != '?' && c != '+' && 
               c != '(' && c != ')' && c != '·';
    }
}
