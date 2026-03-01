package compiler.Lexer;

public enum SymbolType {
    IDENTIFIER, /* Variables */
    KEYWORD, /* if, else, while, for, return, etc. */
    INT,
    FLOAT,
    BOOL, /* Boolean literals: true, false */
    STRING,
    LITERAL, /* General literals (could be used for various types) */
    CONSTANT,
    ASSIGNMENTOPERATOR,
    ARITHMETICOPERATOR, /* +, -, *, / */
    MODULOOPERATOR, /* % */
    COMPARISONOPERATOR, /* ==, =/=, <, >, <=, >= */
    LOGICAL_AND, /* && */
    LOGICAL_OR, /* || */
    LOGICAL_NOT, /* ! */
    SEPARATOR, /* , */
    COMMENT,
    LEFTPAREN,
    RIGHTPAREN,
    SEMICOLON,
    EOF
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
