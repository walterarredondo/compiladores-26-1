package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Symbol;

/**
 * Implements the LALR(1) parsing engine.
 * Uses a stack and the LALR(1) table to process a sequence of tokens.
 * Complementary task for Practice 9.
 */
public class LALR1Parser {
    private final LALR1Table table;

    public LALR1Parser(LALR1Table table) {
        this.table = table;
    }

   // package-private accessor for tests
   LALR1Table getTable() {
       return table;
   }

   /**
    * Parses a sequence of tokens using the LALR(1) parsing algorithm.
    * @param tokens The list of tokens from the lexer.
    * @return true if the sequence is accepted, false if a syntax error is found.
    */
   public boolean parse(List<Token> tokens) {
        // 1. Initialize stack for states
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(table.getInitialState());

        // 2. Create input list with end-of-input marker
        List<Token> input = new ArrayList<>(tokens);
        input.add(new Token("$", "$"));

        // 3. Initialize instruction pointer
        int ip = 0;

        // Get the ACTION and GOTO tables
        Map<Integer, Map<Symbol, LALR1Table.Action>> actionTable = table.getActionTable();
        Map<Integer, Map<Symbol, Integer>> gotoTable = table.getGotoTable();

        // 4. Main parsing loop
        while (true) {
            // a. Get current state from top of stack
            int state = stateStack.peek();

            // b. Get current token
            Token currentToken = input.get(ip);

            // Convert token to symbol - use typeString if available, otherwise use type
            Symbol tokenSymbol = findSymbolForToken(currentToken, actionTable, state);

            if (tokenSymbol == null) {
                // No matching symbol found - syntax error
                return false;
            }

            // c. Look up action in ACTION table
            Map<Symbol, LALR1Table.Action> stateActions = actionTable.get(state);
            LALR1Table.Action action = stateActions != null ? stateActions.get(tokenSymbol) : null;

            // d. If no action found, syntax error
            if (action == null) {
                return false;
            }

            // e. Handle SHIFT action
            if (action.type == LALR1Table.Action.Type.SHIFT) {
                stateStack.push(action.state);
                ip++;
            }
            // f. Handle REDUCE action
            else if (action.type == LALR1Table.Action.Type.REDUCE) {
                com.compiler.parser.grammar.Production production = action.reduceProd;
                int betaLength = production.getRightSide().size();

                // Handle epsilon productions
                if (!production.isEpsilonProduction()) {
                    // Pop |β| states from stack
                    for (int i = 0; i < betaLength; i++) {
                        if (stateStack.isEmpty()) {
                            return false;
                        }
                        stateStack.pop();
                    }
                }

                // Get new state from top of stack
                if (stateStack.isEmpty()) {
                    return false;
                }
                int s = stateStack.peek();

                // Look up GOTO state
                Symbol A = production.getLeftSide();
                Map<Symbol, Integer> stateGotos = gotoTable.get(s);
                Integer gotoState = stateGotos != null ? stateGotos.get(A) : null;

                if (gotoState == null) {
                    return false;
                }

                // Push goto_state onto stack
                stateStack.push(gotoState);
            }
            // g. Handle ACCEPT action
            else if (action.type == LALR1Table.Action.Type.ACCEPT) {
                return true;
            }
            // h. Unhandled case
            else {
                return false;
            }
        }
   }

   /**
    * Find the grammar symbol that matches the given token.
    * Uses typeString if available, otherwise tries to match by name.
    */
   private Symbol findSymbolForToken(Token token, Map<Integer, Map<Symbol, LALR1Table.Action>> actionTable, int state) {
       Map<Symbol, LALR1Table.Action> stateActions = actionTable.get(state);
       if (stateActions == null) {
           return null;
       }

       String tokenName = token.typeString != null ? token.typeString :
                         (token.type != null ? token.type.toString() : token.lexeme);

       // Try to find a symbol that matches the token
       for (Symbol symbol : stateActions.keySet()) {
           if (symbol.name.equals(tokenName)) {
               return symbol;
           }
       }

       return null;
   }
}
