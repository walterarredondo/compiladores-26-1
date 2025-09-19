package com.compiler.parser.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a production rule in a context-free grammar.
 * A production has the form: leftSide -> rightSide1 rightSide2 ... rightSideN
 */
public class Production {
    private final Symbol leftSide;
    private final List<Symbol> rightSide;
    
    /**
     * Creates a new production.
     * @param leftSide The left-hand side (non-terminal)
     * @param rightSide The right-hand side (list of symbols)
     */
    public Production(Symbol leftSide, List<Symbol> rightSide) {
        if (leftSide == null) {
            throw new IllegalArgumentException("Left side cannot be null");
        }
        if (leftSide.type != SymbolType.NON_TERMINAL) {
            throw new IllegalArgumentException("Left side must be a non-terminal");
        }
        
        this.leftSide = leftSide;
        this.rightSide = rightSide != null ? new ArrayList<>(rightSide) : new ArrayList<>();
    }
    
    /**
     * Creates a new production with a single symbol on the right side.
     * @param leftSide The left-hand side (non-terminal)
     * @param rightSymbol The single symbol on the right side
     */
    public Production(Symbol leftSide, Symbol rightSymbol) {
        this(leftSide, Collections.singletonList(rightSymbol));
    }
    
    /**
     * Creates an epsilon production (A -> ε).
     * @param leftSide The left-hand side (non-terminal)
     * @return A new epsilon production
     */
    public static Production epsilon(Symbol leftSide) {
        return new Production(leftSide, Collections.singletonList(Symbol.EPSILON));
    }
    
    /**
     * Gets the left-hand side of this production.
     * @return The left-hand side symbol
     */
    public Symbol getLeftSide() {
        return leftSide;
    }
    
    /**
     * Gets the right-hand side of this production.
     * @return An unmodifiable list of symbols on the right side
     */
    public List<Symbol> getRightSide() {
        return Collections.unmodifiableList(rightSide);
    }
    
    /**
     * Gets the number of symbols on the right-hand side.
     * @return The length of the right-hand side
     */
    public int getRightSideLength() {
        return rightSide.size();
    }
    
    /**
     * Gets the symbol at the specified position on the right-hand side.
     * @param index The position (0-based)
     * @return The symbol at the given position
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Symbol getRightSideSymbol(int index) {
        return rightSide.get(index);
    }
    
    /**
     * Checks if this is an epsilon production (A -> ε).
     * @return true if this production derives epsilon
     */
    public boolean isEpsilonProduction() {
        return rightSide.size() == 1 && rightSide.get(0).isEpsilon();
    }
    
    /**
     * Checks if the right-hand side is empty.
     * @return true if the right side has no symbols
     */
    public boolean hasEmptyRightSide() {
        return rightSide.isEmpty();
    }
    
    /**
     * Gets all symbols that appear in this production.
     * @return A list containing the left side and all right side symbols
     */
    public List<Symbol> getAllSymbols() {
        List<Symbol> allSymbols = new ArrayList<>();
        allSymbols.add(leftSide);
        allSymbols.addAll(rightSide);
        return allSymbols;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Production that = (Production) obj;
        return Objects.equals(leftSide, that.leftSide) &&
               Objects.equals(rightSide, that.rightSide);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(leftSide, rightSide);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(leftSide.toString()).append(" -> ");
        
        if (rightSide.isEmpty()) {
            sb.append("ε");
        } else {
            for (int i = 0; i < rightSide.size(); i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(rightSide.get(i).toString());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a detailed string representation of this production.
     * @return Detailed string with symbol types
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(leftSide.toDetailedString()).append(" -> ");
        
        if (rightSide.isEmpty()) {
            sb.append("ε");
        } else {
            for (int i = 0; i < rightSide.size(); i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(rightSide.get(i).toDetailedString());
            }
        }
        
        return sb.toString();
    }
}
