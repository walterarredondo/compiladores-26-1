package com.compiler.lexer;

/**
 * Represents a token in the lexical analysis process.
 * Each token consists of a type, the matched lexeme, and its position in the input.
 */
public class Token {
    /**
     * The type of this token.
     */
    public final TokenType type;

    /**
     * The actual text (lexeme) that was matched.
     */
    public final String lexeme;

    /**
     * The starting position of this token in the input string.
     */
    public final int position;

    /**
     * Optional string representation of type (for parser testing).
     */
    public final String typeString;

    /**
     * Constructs a new token.
     * @param type The token type.
     * @param lexeme The matched text.
     * @param position The starting position in the input.
     */
    public Token(TokenType type, String lexeme, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
        this.typeString = null;
    }

    /**
     * Convenience constructor for simple tokens where the type name matches the lexeme.
     * Used primarily in parser testing.
     * @param typeString The type as a string.
     * @param lexeme The matched text.
     */
    public Token(String typeString, String lexeme) {
        this.type = null;
        this.lexeme = lexeme;
        this.position = 0;
        this.typeString = typeString;
    }
    
    /**
     * Returns a string representation of the token.
     * @return String representation showing type, lexeme, and position.
     */
    @Override
    public String toString() {
        return String.format("Token{type=%s, lexeme='%s', position=%d}", type, lexeme, position);
    }
    
    /**
     * Checks equality based on type, lexeme, and position.
     * @param obj The object to compare with.
     * @return True if tokens are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return position == token.position &&
               type == token.type &&
               lexeme.equals(token.lexeme);
    }
    
    /**
     * Returns hash code based on type, lexeme, and position.
     * @return Hash code for this token.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, lexeme, position);
    }
}
