package compiler.Lexer;

/*
    * We will regroup symbols that will be verify in the same function.
    * Each function will describe the hierarchy of the symbols, which one is tested first.
*/

public enum SymbolType {
    // Literals / names
    IDENTIFIER, /* Variables */
    KEYWORD, /* if, else, while, for, return, etc. */
    COLLECTION_NAME, // starts with Capital letter, for example: List, Set, Map, etc.
    INT,
    FLOAT,
    BOOLEAN, /* Boolean literals: true, false */
    STRING,

    // Operators
    ASSIGNMENTOPERATOR, /* = */
    PLUS,
    MINUS,
    MULT, /* * for multiplication */
    DIV, /* / for division */
    MOD, /* % for modulo */
    NEGATION, /* - for negative values */

    // Logical and comparison operators
    EQ, /* == */
    NEQ, /* =/= */
    LT, /* < */
    GT, /* > */
    LE, /* <= */
    GE, /* >= */
    AND, /* && */
    OR, /* || */

    // Delimiters and punctuation
    LPAREN, /* ( */
    RPAREN, /* ) */
    LBRACE, /* { */
    RBRACE, /* } */
    LBRACKET, /* [ */
    RBRACKET, /* ] */
    COMMA, /* , */
    SEMICOLON, /* ; */
    DOT, /* . */
    EOF /* End of file */
}

public class Symbol {
    private SymbolType type;
    private String value;

    // Constructor for creating a symbol with a type and an value (value is optional for certain tokens)
    public Symbol(SymbolType type, String value) {
        this.type = type;
        this.value = value;
    }

    // Return the type of the symbol
    public SymbolType getType() {
        return type;
    }

    // Return the value of the symbol (if necessary)
    public String getValue() {
        return value;
    }

    // Override toString for easy debugging and creating our token representation
    @Override
    public String toString() {
        if (value == null) {
            return "<" + type + ",>";
        }
        return "<" + type + ", " + value + ">";
    }
}
