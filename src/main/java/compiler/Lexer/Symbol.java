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

    public Symbol(SymbolType type, String value) {
        this.type = type;
        this.value = value;
    }

    public SymbolType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "<" + type + ",>";
        }
        return "<" + type + ", " + value + ">";
    }
}
